package com.example.studyvault.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {
    private MockMvc mvc;
    private HealthEndpoint endpoint;

    @BeforeEach
    void setUp() {
        endpoint = mock(HealthEndpoint.class);
        mvc = MockMvcBuilders.standaloneSetup(new HealthController(endpoint)).build();
    }

    @Test
    void healthReportsProcessIsAlive() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void readyReportsReadyWhenDependenciesAreUp() throws Exception {
        when(endpoint.health()).thenReturn(Health.up().build());
        mvc.perform(get("/api/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void readyReportsServiceUnavailableWhenDependencyIsDown() throws Exception {
        when(endpoint.health()).thenReturn(Health.down().build());
        mvc.perform(get("/api/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.data.status").value("NOT_READY"));
    }
}
