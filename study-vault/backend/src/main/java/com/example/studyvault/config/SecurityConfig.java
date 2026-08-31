package com.example.studyvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import com.example.studyvault.security.JwtAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.studyvault.dto.ApiResponse;
import com.example.studyvault.dto.ErrorResponse;
import com.example.studyvault.exception.ErrorCode;

@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, AuthenticationEntryPoint entryPoint) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(e -> e.authenticationEntryPoint(entryPoint)).authorizeHttpRequests(auth -> auth.requestMatchers("/api/health", "/api/ready", "/actuator/health", "/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll().anyRequest().authenticated()).addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) { return (request, response, ex) -> { response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE); mapper.writeValue(response.getOutputStream(), ApiResponse.failure(new ErrorResponse(ErrorCode.UNAUTHORIZED.name(), "Authentication is required", java.util.Map.of(), request.getRequestURI()))); }; }
}
