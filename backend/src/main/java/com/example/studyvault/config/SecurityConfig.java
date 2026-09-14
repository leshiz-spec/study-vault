package com.example.studyvault.config;

import com.example.studyvault.dto.ApiResponse;
import com.example.studyvault.dto.ErrorResponse;
import com.example.studyvault.exception.ErrorCode;
import com.example.studyvault.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtFilter,
      RequestLoggingFilter requestLoggingFilter,
      AuthenticationEntryPoint entryPoint)
      throws Exception {

    return http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/health",
                        "/api/ready",
                        "/actuator/health",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/logout")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            jwtFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .addFilterAfter(requestLoggingFilter, JwtAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:5173",
            "https://study-vault-dun.vercel.app"
        )
    );

    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
    );

    configuration.setAllowedHeaders(List.of("*"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
    return (request, response, ex) -> {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      mapper.writeValue(
          response.getOutputStream(),
          ApiResponse.failure(
              new ErrorResponse(
                  ErrorCode.UNAUTHORIZED.name(),
                  "Authentication is required",
                  java.util.Map.of(),
                  request.getRequestURI())));
    };
  }

  @Bean
  FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistration(
      RequestLoggingFilter filter) {

    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);

    return registration;
  }
}