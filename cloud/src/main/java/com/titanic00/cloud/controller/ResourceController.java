package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
