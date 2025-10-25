package com.titanic00.cloud.service;

import com.titanic00.cloud.context.AuthContext;
import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.entity.User;
import com.titanic00.cloud.exception.NotFoundException;
import com.titanic00.cloud.exception.UnauthorizedException;
import com.titanic00.cloud.exception.UnidentifiedErrorException;
import com.titanic00.cloud.exception.ValidationErrorException;
import com.titanic00.cloud.repository.UserRepository;
import com.titanic00.cloud.util.MinioObjectUtil;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectoryService {

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.root-folder}")
    private String rootFolderName;

    private final MinioClient minioClient;
    private final AuthContext authContext;
    private final UserRepository userRepository;
    private final ResourceService resourceService;

    public DirectoryService(MinioClient minioClient, AuthContext authContext, UserRepository userRepository, ResourceService resourceService) {
        this.minioClient = minioClient;
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
    }

    public List<MinioObjectDTO> getDirectoryInfo(String path) {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());
            String fullPath = String.format(rootFolderName, user.getId()) + path;

            if (path.equals("/")) {
                fullPath = String.format(rootFolderName, user.getId());
            }

            if (!fullPath.endsWith("/")) {
                fullPath += "/";
            }

            validateGetDirectoryInfoConditions(fullPath);

            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(false)
                            .build()
            );

            List<MinioObjectDTO> minioObjectDTOs = new ArrayList<>();


            for (Result<Item> item : items) {
                // ignore root folder and avoid duplicates
                if (item.get().objectName().equals(String.format(rootFolderName, user.getId())) || item.get().objectName().equals(fullPath)) {
                    continue;
                }

                minioObjectDTOs.add(resourceService.buildMinioObjectDTO(item.get().objectName()));
            }

            return minioObjectDTOs;
        } catch (ValidationErrorException | UnauthorizedException | NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public void validateGetDirectoryInfoConditions(String fullPath) {
        if (!MinioObjectUtil.validatePath(fullPath)) {
            throw new ValidationErrorException("Invalid path.");
        }

        if (!resourceService.directoryExists(fullPath)) {
            throw new NotFoundException("Directory is empty or doesn't exist.");
        }
    }
}
