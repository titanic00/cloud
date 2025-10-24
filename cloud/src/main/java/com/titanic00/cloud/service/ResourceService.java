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
import java.util.ArrayList;
import java.util.List;

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

    /*
       Since there is no traditional way to move or rename an object from one place to another,
       we will copy the object from the source directory to the destination directory
       and then delete it from the source directory.
     */
    public MinioObjectDTO moveOrRenameResource(String from, String to) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            if (from.startsWith("/")) {
                from = from.substring(1);
            }
            String objectName = String.format(rootFolderName, user.getId()) + from;

            if (to.startsWith("/")) {
                to = to.substring(1);
            }
            String pathTo = String.format(rootFolderName, user.getId()) + to;

            if (pathTo.endsWith("/")) {
                pathTo += MinioObjectUtil.getFileNameFromObjectName(objectName);
            }

            validateMoveConditions(objectName, pathTo);

            // copy all objects from directory
            if (MinioObjectUtil.isDir(objectName)) {
                Iterable<Result<Item>> items = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(objectName)
                                .recursive(true)
                                .build()
                );

                for (Result<Item> item : items) {
                    {
                        String newPath = pathTo + item.get().objectName().substring(objectName.length());

                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(newPath)
                                        .source(CopySource.builder()
                                                .bucket(bucketName)
                                                .object(item.get().objectName())
                                                .build())
                                        .build());
                    }
                }
            }
            // copy normal object
            else {
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(pathTo)
                                .source(CopySource.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .build())
                                .build()
                );
            }

            deleteResource(from);

            return buildMinioObjectDTO(pathTo);
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException | AlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public List<MinioObjectDTO> searchResource(String query) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());
            String userRootFolder = String.format(rootFolderName, user.getId());

            validateQueryConditions(query);

            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(userRootFolder)
                            .recursive(true)
                            .build()
            );

            List<MinioObjectDTO> minioObjectDTOs = new ArrayList<>();

            for (Result<Item> item : items) {
                // ignore user root folder
                if (item.get().objectName().substring(userRootFolder.length()).contains(query)) {
                    minioObjectDTOs.add(buildMinioObjectDTO(item.get().objectName()));
                }
            }

            return minioObjectDTOs;
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

    public void validateMoveConditions(String objectName, String pathTo) throws Exception {
        if (!MinioObjectUtil.validatePath(objectName) || !MinioObjectUtil.validatePath(pathTo)) {
            throw new ValidationErrorException("Invalid path.");
        }

        if (!directoryExists(MinioObjectUtil.getResourceFullPath(objectName))) {
            throw new ValidationErrorException("Path doesn't exist.");
        }

        if (!resourceExists(objectName) && !MinioObjectUtil.isDir(objectName)) {
            throw new NotFoundException("Object to copy doesn't exist.");
        }

        if (!directoryExists(objectName) && MinioObjectUtil.isDir(objectName)) {
            throw new NotFoundException("Directory to copy doesn't exist.");
        }

        if (resourceExists(pathTo) || directoryExists(pathTo)) {
            throw new AlreadyExistsException("Object in the destination folder already exists.");
        }
    }

    public void validateQueryConditions(String query) {
        if (query.isEmpty()) {
            throw new ValidationErrorException("Invalid query.");
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
