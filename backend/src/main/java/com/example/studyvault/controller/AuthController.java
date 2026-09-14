package com.example.studyvault.controller;

import com.example.studyvault.dto.*;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.security.JwtAuthenticationFilter;
import com.example.studyvault.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final boolean secureCookie;

  @Autowired
  public AuthController(
      AuthService auth, @Value("${studyvault.auth-cookie.secure:false}") boolean secureCookie) {
    this.auth = auth;
    this.secureCookie = secureCookie;
  }

  public AuthController(AuthService auth) {
    this(auth, false);
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> register(
      HttpServletRequest servletRequest, @Valid @RequestBody RegisterRequest request) {
    return withCookie(auth.register(request), servletRequest);
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<UserResponse>> login(
      HttpServletRequest servletRequest, @Valid @RequestBody LoginRequest request) {
    return withCookie(auth.login(request), servletRequest);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    return ResponseEntity.ok()
        .header("Set-Cookie", clearCookie(request).toString())
        .body(ApiResponse.success(null));
  }

  @GetMapping("/me")
  public ApiResponse<UserResponse> me(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof User user))
      throw new UnauthorizedException();
    return ApiResponse.success(UserResponse.from(user));
  }

  private ResponseEntity<ApiResponse<UserResponse>> withCookie(
      AuthService.AuthResult result, HttpServletRequest request) {
    return ResponseEntity.ok()
        .header("Set-Cookie", cookie(result.token(), request).toString())
        .body(ApiResponse.success(result.user()));
  }

  private ResponseCookie cookie(String token, HttpServletRequest request) {
    boolean crossSiteHttps = isCrossSiteHttpsRequest(request);
    return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token)
        .httpOnly(true)
        .secure(secureCookie || crossSiteHttps)
        .sameSite(crossSiteHttps ? "None" : "Lax")
        .path("/")
        .maxAge(86400)
        .build();
  }

  private ResponseCookie clearCookie(HttpServletRequest request) {
    boolean crossSiteHttps = isCrossSiteHttpsRequest(request);
    return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
        .httpOnly(true)
        .secure(secureCookie || crossSiteHttps)
        .sameSite(crossSiteHttps ? "None" : "Lax")
        .path("/")
        .maxAge(0)
        .build();
  }

  private boolean isCrossSiteHttpsRequest(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (origin == null || origin.isBlank()) return false;

    try {
      URI originUri = URI.create(origin);
      String originHost = originUri.getHost();
      return "https".equalsIgnoreCase(originUri.getScheme())
          && originHost != null
          && !originHost.equalsIgnoreCase(request.getServerName());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }
}
