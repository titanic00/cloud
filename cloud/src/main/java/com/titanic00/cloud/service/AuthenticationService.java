package com.titanic00.cloud.service;

import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.dto.request.AuthorizationRequest;
import com.titanic00.cloud.entity.User;
import com.titanic00.cloud.exception.AlreadyExistsException;
import com.titanic00.cloud.exception.UnidentifiedErrorException;
import com.titanic00.cloud.exception.ValidationErrorException;
import com.titanic00.cloud.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
}
