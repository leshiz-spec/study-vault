package com.example.studyvault.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.studyvault.dto.UserResponse;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.GlobalExceptionHandler;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.exception.UsernameAlreadyExistsException;
import com.example.studyvault.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  private MockMvc mvc;
  private AuthService auth;

  @BeforeEach
  void setUp() {
    auth = mock(AuthService.class);
    mvc =
        MockMvcBuilders.standaloneSetup(new AuthController(auth))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void registrationReturnsHttpOnlyCookieAndSafeUser() throws Exception {
    when(auth.register(any()))
        .thenReturn(new AuthService.AuthResult(publicResponse(), "jwt-token"));
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    "{\"username\":\"alice\",\"email\":\"a@example.com\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(jsonPath("$.data.username").value("alice"))
        .andExpect(content().string(not(containsString("password_hash"))));
  }

  @Test
  void unauthenticatedMeReturns401() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  @Test
  void authenticatedMeReturnsSafeUser() throws Exception {
    var controller = new AuthController(auth);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    User u = publicUser();
    mvc.perform(get("/api/auth/me").principal(new UsernamePasswordAuthenticationToken(u, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("a@example.com"))
        .andExpect(content().string(not(containsString("password_hash"))));
  }

  @Test
  void loginFailureReturnsStableUnauthorizedError() throws Exception {
    when(auth.login(any())).thenThrow(new UnauthorizedException());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"usernameOrEmail\":\"alice\",\"password\":\"wrongpass\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  @Test
  void duplicateUsernameReturnsUsernameAlreadyExists() throws Exception {
    when(auth.register(any())).thenThrow(new UsernameAlreadyExistsException());
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    "{\"username\":\"alice\",\"email\":\"new@example.com\",\"password\":\"secret123\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("USERNAME_ALREADY_EXISTS"));
  }

  private User publicUser() {
    User u = new User();
    u.setUsername("alice");
    u.setEmail("a@example.com");
    u.setPasswordHash("never-returned");
    return u;
  }

  private UserResponse publicResponse() {
    return new UserResponse(null, "alice", "a@example.com", null, null);
  }
}
