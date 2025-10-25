package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.MinioObjectDTO;
import com.titanic00.cloud.service.DirectoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("")
    public List<MinioObjectDTO> getDirectory(@RequestParam String path) {

        return directoryService.getDirectoryInfo(path);
    }

    @PostMapping("")
    public MinioObjectDTO createEmptyDirectory(@RequestParam String path) {

        return directoryService.createEmptyDirectory(path);
    }
}
