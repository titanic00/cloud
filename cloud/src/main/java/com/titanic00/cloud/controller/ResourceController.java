package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.service.ResourceService;
import com.titanic00.cloud.util.MinioObjectUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("")
    public ResponseEntity<MinioObjectDTO> getResource(@RequestParam String path) {

        MinioObjectDTO minioObjectDTO = resourceService.getResource(path);

        return ResponseEntity.status(HttpStatus.OK).body(minioObjectDTO);
    }

    @PostMapping("")
    public ResponseEntity<MinioObjectDTO> uploadResource(@RequestParam String path, @RequestParam("object") MultipartFile file) {

        MinioObjectDTO objectDTO = resourceService.uploadResource(path, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(objectDTO);
    }

    @DeleteMapping("")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam String path) {
        resourceService.deleteResource(path);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadResource(@RequestParam String path) {

        if (MinioObjectUtil.isDir(path)) {
            byte[] zip = resourceService.downloadResources(path);

            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).body(zip);
        } else {
            Resource resource = resourceService.downloadResource(path);

            return ResponseEntity.status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + MinioObjectUtil.getFileNameFromObjectName(path) + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
        }
    }

    @GetMapping("/move")
    public ResponseEntity<MinioObjectDTO> moveResource(@RequestParam String from, @RequestParam String to) {

        MinioObjectDTO minioObjectDTO = resourceService.moveOrRenameResource(from, to);

        return ResponseEntity.status(HttpStatus.OK).body(minioObjectDTO);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MinioObjectDTO>> searchResource(@RequestParam String query) {

        List<MinioObjectDTO> minioObjectDTOs = resourceService.searchResource(query);

        return ResponseEntity.status(HttpStatus.OK).body(minioObjectDTOs);
    }
}
