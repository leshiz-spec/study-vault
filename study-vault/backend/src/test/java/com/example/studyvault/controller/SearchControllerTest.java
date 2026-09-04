package com.example.studyvault.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteSearchResponse;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.GlobalExceptionHandler;
import com.example.studyvault.service.NoteService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SearchControllerTest {
    private MockMvc mvc;
    private NoteService service;
    private User alice;

    @BeforeEach
    void setUp() {
        service = mock(NoteService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SearchController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        alice = new User();
        alice.setUsername("alice");
    }

    @Test
    void searchPassesAllFiltersAndPagingToService() throws Exception {
        when(service.search(eq(alice), eq("graph"), eq("math"), eq(true), eq("active"), any(Pageable.class)))
                .thenReturn(new NoteSearchResponse(List.of(response()), 1, 5, 6, 2));

        mvc.perform(get("/api/search")
                        .principal(auth(alice))
                        .param("q", "graph")
                        .param("tag", "math")
                        .param("favorite", "true")
                        .param("status", "active")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "updatedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(content().string(containsString("Algorithms")));

        verify(service).search(eq(alice), eq("graph"), eq("math"), eq(true), eq("active"), any(Pageable.class));
    }

    @Test
    void searchRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/search")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test
    void invalidPagingParameterReturnsValidationError() throws Exception {
        mvc.perform(get("/api/search").principal(auth(alice)).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void searchWithNoResultsReturnsEmptyPage() throws Exception {
        when(service.search(eq(alice), eq("missing"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new NoteSearchResponse(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/api/search").principal(auth(alice)).param("q", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private static UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static NoteResponse response() {
        return new NoteResponse(1L, "Algorithms", "Graph search", null, "active", true, "not_started", null, null);
    }
}
