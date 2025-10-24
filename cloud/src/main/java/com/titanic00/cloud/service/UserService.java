package com.titanic00.cloud.service;

import com.titanic00.cloud.context.AuthContext;
import com.titanic00.cloud.dto.UserDTO;
import com.titanic00.cloud.entity.User;
import com.titanic00.cloud.exception.UnauthorizedException;
import com.titanic00.cloud.exception.UnidentifiedErrorException;
import com.titanic00.cloud.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthContext authContext;

    public UserService(UserRepository userRepository, AuthContext authContext) {
        this.userRepository = userRepository;
        this.authContext = authContext;
    }

    public UserDTO getSignedInUser() {
        try {
            User user = userRepository.findByUsername(authContext.getUserDetails().getUsername());

            return UserDTO.from(user);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error, please try again.");
        }
    }
}
