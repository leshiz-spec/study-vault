package com.example.studyvault.controller;

import com.example.studyvault.dto.*;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.security.JwtAuthenticationFilter;
import com.example.studyvault.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final boolean secureCookie;
    @Autowired
    public AuthController(AuthService auth, @Value("${studyvault.auth-cookie.secure:false}") boolean secureCookie) { this.auth = auth; this.secureCookie = secureCookie; }
    public AuthController(AuthService auth) { this(auth, false); }
    @PostMapping("/register") public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) { return withCookie(auth.register(request)); }
    @PostMapping("/login") public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) { return withCookie(auth.login(request)); }
    @PostMapping("/logout") public ResponseEntity<ApiResponse<Void>> logout() { return ResponseEntity.ok().header("Set-Cookie", clearCookie().toString()).body(ApiResponse.success(null)); }
    @GetMapping("/me") public ApiResponse<UserResponse> me(Authentication authentication) { if (authentication == null || !(authentication.getPrincipal() instanceof User user)) throw new UnauthorizedException(); return ApiResponse.success(UserResponse.from(user)); }
    private ResponseEntity<ApiResponse<UserResponse>> withCookie(AuthService.AuthResult result) { return ResponseEntity.ok().header("Set-Cookie", cookie(result.token()).toString()).body(ApiResponse.success(result.user())); }
    private ResponseCookie cookie(String token) { return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token).httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(86400).build(); }
    private ResponseCookie clearCookie() { return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "").httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(0).build(); }
}
