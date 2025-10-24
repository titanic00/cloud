package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.dto.request.AuthorizationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @PostMapping("/sign-up")
    public ResponseEntity<UserDTO> signUp(@RequestBody @Valid AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        return null;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserDTO> signIn(@RequestBody AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        return null;
    }
}
