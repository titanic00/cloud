package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser() {

        UserDTO userDTO = userService.getSignedInUser();

        return ResponseEntity.status(HttpStatus.OK).body(userDTO);
    }
}
