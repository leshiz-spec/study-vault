package com.example.studyvault.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

/** Access logging without query strings, bodies, cookies, or credentials. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try { chain.doFilter(request, response); }
        finally {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Object principal = auth == null ? null : auth.getPrincipal();
            String userId = principal instanceof com.example.studyvault.entity.User user && user.getId() != null ? String.valueOf(user.getId()) : "anonymous";
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            log.info("request path={} status={} durationMs={} userId={}", request.getRequestURI(), response.getStatus(), durationMs, userId);
        }
    }
}
