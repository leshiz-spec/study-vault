package com.example.studyvault.service;

import com.example.studyvault.dto.LoginRequest;
import com.example.studyvault.dto.RegisterRequest;
import com.example.studyvault.dto.UserResponse;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.EmailAlreadyExistsException;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.repository.UserRepository;
import com.example.studyvault.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }
    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) throw new EmailAlreadyExistsException();
        if (users.existsByUsername(request.username())) throw new UnauthorizedException();
        User user = new User(); user.setUsername(request.username()); user.setEmail(request.email()); user.setPasswordHash(encoder.encode(request.password()));
        user = users.save(user); return new AuthResult(UserResponse.from(user), jwt.issue(user.getId(), user.getUsername()));
    }
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        User user = users.findByUsername(request.usernameOrEmail()).or(() -> users.findByEmail(request.usernameOrEmail())).orElseThrow(UnauthorizedException::new);
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new UnauthorizedException();
        return new AuthResult(UserResponse.from(user), jwt.issue(user.getId(), user.getUsername()));
    }
    public record AuthResult(UserResponse user, String token) { }
}
