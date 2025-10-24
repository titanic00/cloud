package com.titanic00.cloud.service;

import com.titanic00.cloud.context.AuthContext;
import com.titanic00.cloud.dto.DirectoryDTO;
import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.dto.ResourceDTO;
import com.titanic00.cloud.entity.User;
import com.titanic00.cloud.enumeration.ObjectType;
import com.titanic00.cloud.exception.*;
import com.titanic00.cloud.repository.UserRepository;
import com.titanic00.cloud.util.MinioObjectUtil;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class ResourceService {

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.root-folder}")
    private String rootFolderName;

    private final MinioClient minioClient;
    private final AuthContext authContext;
    private final UserRepository userRepository;

    public ResourceService(MinioClient minioClient, AuthContext authContext, UserRepository userRepository) {
        this.minioClient = minioClient;
        this.authContext = authContext;
        this.userRepository = userRepository;
    }

    public MinioObjectDTO getResource(String path) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String objectName = String.format(rootFolderName, user.getId()) + path;

            validateResourceAndPathConditions(objectName);

            return buildMinioObjectDTO(objectName);
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public MinioObjectDTO uploadResource(String path, MultipartFile file) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());
            String objectName = MinioObjectUtil.buildObjectName(String.format(rootFolderName, user.getId()) + path,
                    file.getOriginalFilename());

            validateUploadConditions(objectName, file);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getInputStream().available(), -1)
                            .build()
            );

            return buildMinioObjectDTO(objectName);
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public void deleteResource(String path) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String objectName = String.format(rootFolderName, user.getId()) + path;

            validateResourceAndPathConditions(objectName);

            if (MinioObjectUtil.isDir(objectName)) {
                Iterable<Result<Item>> items = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(objectName)
                                .recursive(true)
                                .build()
                );

                for (Result<Item> item : items) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(item.get().objectName())
                                    .build()
                    );
                }
            } else {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
            }
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public Resource downloadResource(String path) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String objectName = String.format(rootFolderName, user.getId()) + path;

            validateResourceAndPathConditions(objectName);

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            return new InputStreamResource(stream);
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public byte[] downloadResources(String path) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            String fullPath = String.format(rootFolderName, user.getId()) + path;

            validateDirectoryAndPathConditions(fullPath);

            String parentFolder = MinioObjectUtil.getParentDirectoryPath(fullPath);

            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );

            return MinioObjectUtil.toZip(items, parentFolder);
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public boolean resourceExists(String objectName) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            return true;
        } catch (ErrorResponseException ex) {
            if (ex.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw ex;
        }
    }

    public boolean directoryExists(String path) {
        Iterable<Result<Item>> items = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .build()
        );

        if (items.iterator().hasNext()) {
            return true;
        }

        return false;
    }

    public MinioObjectDTO buildMinioObjectDTO(String objectName) throws Exception {
        StatObjectResponse statObjectResponse = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        boolean isDir = MinioObjectUtil.isDir(statObjectResponse.object());

        if (isDir) {
            return DirectoryDTO.builder()
                    .path(MinioObjectUtil.formatDirectoryPath(statObjectResponse.object()))
                    .name(MinioObjectUtil.getDirectoryNameFromObjectName(statObjectResponse.object()))
                    .type(ObjectType.DIRECTORY)
                    .build();
        } else {
            return ResourceDTO.builder()
                    .path(MinioObjectUtil.formatObjectPath(statObjectResponse.object()))
                    .name(MinioObjectUtil.getFileNameFromObjectName(statObjectResponse.object()))
                    .size(MinioObjectUtil.humanReadableByteCountSI(statObjectResponse.size()))
                    .type(ObjectType.FILE)
                    .build();
        }
    }

    public void validateUploadConditions(String objectName, MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new ValidationErrorException("Invalid file.");
        }

        if (resourceExists(objectName)) {
            throw new AlreadyExistsException("Object already exists.");
        }
    }

    public void validateResourceAndPathConditions(String objectName) throws Exception {
        if (!MinioObjectUtil.validatePath(objectName)) {
            throw new ValidationErrorException("Invalid path.");
        }

        if (!resourceExists(objectName) && !MinioObjectUtil.isDir(objectName)) {
            throw new NotFoundException("Object doesn't exist.");
        }

        if (!directoryExists(objectName) && MinioObjectUtil.isDir(objectName)) {
            throw new NotFoundException("Directory doesn't exist.");
        }
    }

    public void validateDirectoryAndPathConditions(String objectName) {
        if (!MinioObjectUtil.validatePath(objectName)) {
            throw new ValidationErrorException("Invalid path.");
        }

        if (!directoryExists(objectName)) {
            throw new NotFoundException("Object doesn't exist.");
        }
    }

    @PostConstruct
    private void createBucket() throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    public void createUserRootFolder(Long userId) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(String.format(rootFolderName, userId))
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build()
        );
    }
}
