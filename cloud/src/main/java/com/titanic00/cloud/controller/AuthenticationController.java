package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.dto.request.AuthorizationRequest;
import com.titanic00.cloud.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<UserDTO> signUp(@RequestBody @Valid AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {

        UserDTO registered = authenticationService.signUp(authorizationRequest, request, response);

        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserDTO> signIn(@RequestBody AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {

        UserDTO signedIn = authenticationService.signIn(authorizationRequest, request, response);

        return ResponseEntity.status(HttpStatus.OK).body(signedIn);
    }
}
