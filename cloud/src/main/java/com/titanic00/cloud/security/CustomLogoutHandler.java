package com.titanic00.cloud.security;

import com.titanic00.cloud.exception.UnauthorizedException;
import com.titanic00.cloud.exception.UnidentifiedErrorException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.stereotype.Component;

@Component
@EnableRedisIndexedHttpSession
public class CustomLogoutHandler implements LogoutHandler {

    private final FindByIndexNameSessionRepository<? extends Session> redisIndexedSessionRepository;

    public CustomLogoutHandler(RedisIndexedSessionRepository redisIndexedSessionRepository) {
        this.redisIndexedSessionRepository = redisIndexedSessionRepository;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new UnauthorizedException("User is not authorized.");
            }

            String id = request.getSession(false).getId();
            if (id != null && this.redisIndexedSessionRepository.findById(id) != null) {
                this.redisIndexedSessionRepository.deleteById(id);
            }
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnidentifiedErrorException("Unknown error.");
        }
    }
}
