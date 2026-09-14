package com.example.studyvault.security;

import com.example.studyvault.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  public static final String COOKIE_NAME = "STUDYVAULT_TOKEN";
  private final JwtService jwtService;
  private final UserRepository users;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
    this.jwtService = jwtService;
    this.users = users;
  }

  /**
   * Streaming downloads use Servlet async dispatches. Re-read the HttpOnly cookie on that dispatch
   * so authorization does not see the request as anonymous.
   */
  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = cookie(request);
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        Long userId = Long.valueOf(jwtService.parse(token).getSubject());
        users
            .findById(userId)
            .ifPresent(
                user -> {
                  var auth =
                      new UsernamePasswordAuthenticationToken(
                          user, null, java.util.Collections.emptyList());
                  auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                  SecurityContextHolder.getContext().setAuthentication(auth);
                });
      } catch (JwtException | IllegalArgumentException ignored) {
        /* invalid tokens are treated as anonymous */
      }
    }
    chain.doFilter(request, response);
  }

  private String cookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie c : request.getCookies()) if (COOKIE_NAME.equals(c.getName())) return c.getValue();
    return null;
  }
}
