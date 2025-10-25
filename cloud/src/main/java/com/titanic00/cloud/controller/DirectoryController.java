package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.service.DirectoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("")
    public ResponseEntity<List<MinioObjectDTO>> getDirectory(@RequestParam String path) {

        List<MinioObjectDTO> minioObjectDTOs = directoryService.getDirectoryInfo(path);

        return ResponseEntity.status(HttpStatus.OK).body(minioObjectDTOs);
    }
}
