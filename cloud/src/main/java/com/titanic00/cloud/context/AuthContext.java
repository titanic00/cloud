package com.titanic00.cloud.context;

import com.titanic00.cloud.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    public UserDetails getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authorized.");
        }

        return (UserDetails) authentication.getPrincipal();
    }
}
