package com.titanic00.cloud.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic(withDefaults()).csrf(csrf -> csrf.disable()).authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/auth/sign-in", "/api/auth/sign-up", "/api/auth/sign-out").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}
