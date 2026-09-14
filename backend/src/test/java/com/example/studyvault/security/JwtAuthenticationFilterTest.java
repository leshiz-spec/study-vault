package com.example.studyvault.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.example.studyvault.entity.User;
import com.example.studyvault.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.DispatcherType;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void restoresAuthenticationOnAsyncDispatchUsedByStreamingDownloads() throws Exception {
    JwtService jwt = mock(JwtService.class);
    UserRepository users = mock(UserRepository.class);
    Claims claims = mock(Claims.class);
    User user = new User();
    when(jwt.parse("token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("42");
    when(users.findById(42L)).thenReturn(Optional.of(user));

    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setDispatcherType(DispatcherType.ASYNC);
    request.setCookies(
        new jakarta.servlet.http.Cookie(JwtAuthenticationFilter.COOKIE_NAME, "token"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) ->
            assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()));

    verify(users).findById(anyLong());
  }
}
