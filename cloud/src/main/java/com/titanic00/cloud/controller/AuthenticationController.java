package com.titanic00.cloud.controller;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.dto.request.AuthorizationRequest;
import com.titanic00.cloud.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO signUp(@RequestBody @Valid AuthorizationRequest authorizationRequest,
                          HttpServletRequest request,
                          HttpServletResponse response) {

        return authenticationService.signUp(authorizationRequest, request, response);
    }

    @PostMapping("/sign-in")
    public UserDTO signIn(@RequestBody @Valid AuthorizationRequest authorizationRequest,
                          HttpServletRequest request,
                          HttpServletResponse response) {

        return authenticationService.signIn(authorizationRequest, request, response);
    }
}
