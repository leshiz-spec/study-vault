package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.LoginRequest;
import com.example.studyvault.dto.RegisterRequest;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.EmailAlreadyExistsException;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.repository.UserRepository;
import com.example.studyvault.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private UserRepository users; private PasswordEncoder encoder; private JwtService jwt; private AuthService service;
    @BeforeEach void setUp() { users = mock(UserRepository.class); encoder = mock(PasswordEncoder.class); jwt = mock(JwtService.class); service = new AuthService(users, encoder, jwt); }

    @Test void successfulRegistrationHashesPasswordAndReturnsToken() {
        when(users.existsByEmail("a@example.com")).thenReturn(false); when(users.existsByUsername("alice")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("$2a$hashed"); when(users.save(any())).thenAnswer(inv -> { User u=inv.getArgument(0); return u; }); when(jwt.issue(null, "alice")).thenReturn("jwt");
        var result = service.register(new RegisterRequest("alice", "a@example.com", "secret123"));
        verify(encoder).encode("secret123"); verify(users).save(any(User.class)); assertEquals("alice", result.user().username()); assertEquals("jwt", result.token());
    }

    @Test void duplicateEmailUsesStableError() {
        when(users.existsByEmail("a@example.com")).thenReturn(true);
        var ex = assertThrows(EmailAlreadyExistsException.class, () -> service.register(new RegisterRequest("alice", "a@example.com", "secret123")));
        assertEquals("EMAIL_ALREADY_EXISTS", ex.getCode().name()); verify(users, never()).save(any());
    }

    @Test void successfulLoginChecksPassword() {
        User user = user(); when(users.findByUsername("alice")).thenReturn(java.util.Optional.of(user)); when(encoder.matches("secret123", "hash")).thenReturn(true); when(jwt.issue(7L, "alice")).thenReturn("jwt");
        var result = service.login(new LoginRequest("alice", "secret123"));
        assertEquals("jwt", result.token()); verify(encoder).matches("secret123", "hash");
    }

    @Test void invalidPasswordIsUnauthorized() {
        User user = user(); when(users.findByUsername("alice")).thenReturn(java.util.Optional.of(user)); when(encoder.matches("wrong", "hash")).thenReturn(false);
        var ex = assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("alice", "wrong")));
        assertEquals("UNAUTHORIZED", ex.getCode().name());
    }

    private User user() { User u = new User(); try { var id=User.class.getDeclaredField("id"); id.setAccessible(true); id.set(u, 7L); } catch (ReflectiveOperationException ignored) {} u.setUsername("alice"); u.setEmail("a@example.com"); u.setPasswordHash("hash"); return u; }
}
