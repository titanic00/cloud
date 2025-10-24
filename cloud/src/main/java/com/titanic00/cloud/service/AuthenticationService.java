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
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO signUp(AuthorizationRequest authorizationRequest) {
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

            return UserDTO.from(user);
        } catch (AlreadyExistsException | ValidationErrorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }

    public UserDTO signIn(AuthorizationRequest authorizationRequest) {
        try {
            if (!UserUtil.validateUsername(authorizationRequest.getUsername())) {
                throw new ValidationErrorException("Username must be between 2 and 10 characters.");
            }

            User user = userRepository.findByUsername(authorizationRequest.getUsername());

            if (!passwordEncoder.matches(authorizationRequest.getPassword(), user.getPassword())) {
                throw new UnauthorizedException("Password doesn't match or user doesn't exist.");
            }

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
}
