package com.example.studyvault.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {
  private CorsConfigurationSource source;

  @BeforeEach
  void setUp() {
    source = new SecurityConfig().corsConfigurationSource();
  }

  @Test
  void activeVercelDeploymentIsAllowed() {
    CorsConfiguration configuration = configurationFor("/api/auth/login");

    assertEquals(
        "https://study-vault-git-main-annz.vercel.app",
        configuration.checkOrigin("https://study-vault-git-main-annz.vercel.app"));
  }

  @Test
  void unrelatedVercelProjectIsRejected() {
    CorsConfiguration configuration = configurationFor("/api/auth/login");

    assertNull(configuration.checkOrigin("https://another-project.vercel.app"));
  }

  @Test
  void localhostDevelopmentOriginsAreAllowed() {
    CorsConfiguration configuration = configurationFor("/api/auth/login");

    assertEquals(
        "http://localhost:5173", configuration.checkOrigin("http://localhost:5173"));
  }

  private CorsConfiguration configurationFor(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
    return source.getCorsConfiguration(request);
  }
}
