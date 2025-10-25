package com.titanic00.cloud.service;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.dto.request.AuthorizationRequest;
import com.titanic00.cloud.entity.User;
import com.titanic00.cloud.exception.AlreadyExistsException;
import com.titanic00.cloud.exception.UnauthorizedException;
import com.titanic00.cloud.exception.UnidentifiedErrorException;
import com.titanic00.cloud.exception.ValidationErrorException;
import com.titanic00.cloud.repository.UserRepository;
import com.titanic00.cloud.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final ResourceService resourceService;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 SecurityContextRepository securityContextRepository, ResourceService resourceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.resourceService = resourceService;
        this.securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        this.securityContextRepository = securityContextRepository;
    }

    public UserDTO signUp(AuthorizationRequest authorizationRequest,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        try {
            if (usernameExists(authorizationRequest.getUsername())) {
                throw new AlreadyExistsException("An account for that username already exists.");
            }

            if (!UserUtil.validateUsername(authorizationRequest.getUsername())) {
                throw new ValidationErrorException("Username must be between 2 and 10 characters.");
            }

            User user = userRepository.save(User.builder()
                    .username(authorizationRequest.getUsername())
                    .password(passwordEncoder.encode(authorizationRequest.getPassword()))
                    .build());

            resourceService.createUserRootFolder(user.getId());

            storeAuthenticationToSession(authorizationRequest, request, response);

            return UserDTO.from(user);
        } catch (AlreadyExistsException | ValidationErrorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public UserDTO signIn(AuthorizationRequest authorizationRequest,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        try {
            if (!UserUtil.validateUsername(authorizationRequest.getUsername())) {
                throw new ValidationErrorException("Username must be between 2 and 10 characters.");
            }

            User user = userRepository.findByUsername(authorizationRequest.getUsername());

            if (!passwordEncoder.matches(authorizationRequest.getPassword(), user.getPassword())) {
                throw new UnauthorizedException("Password doesn't match or user doesn't exist.");
            }

            storeAuthenticationToSession(authorizationRequest, request, response);

            return UserDTO.from(user);
        } catch (ValidationErrorException | UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public void storeAuthenticationToSession(AuthorizationRequest authorizationRequest,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authorizationRequest.getUsername(), authorizationRequest.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);

        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
