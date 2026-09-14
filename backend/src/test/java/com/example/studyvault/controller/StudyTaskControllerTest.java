package com.example.studyvault.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.studyvault.dto.StudyTaskCreateRequest;
import com.example.studyvault.dto.StudyTaskResponse;
import com.example.studyvault.dto.StudyTaskUpdateRequest;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.GlobalExceptionHandler;
import com.example.studyvault.exception.InvalidTaskStatusException;
import com.example.studyvault.service.StudyTaskService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StudyTaskControllerTest {
  private MockMvc mvc;
  private StudyTaskService service;
  private User alice;

  @BeforeEach
  void setUp() {
    service = mock(StudyTaskService.class);
    mvc =
        MockMvcBuilders.standaloneSetup(new StudyTaskController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    alice = new User();
    alice.setUsername("alice");
  }

  @Test
  void listTasksPassesOwnershipAndFilters() throws Exception {
    when(service.list(eq(alice), eq("todo"), eq(LocalDate.of(2026, 9, 10)), isNull(), isNull()))
        .thenReturn(List.of(task(1L, "Review", "todo")));

    mvc.perform(get("/api/tasks?status=todo&dueDate=2026-09-10").principal(auth(alice)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Review"));
    verify(service).list(alice, "todo", LocalDate.of(2026, 9, 10), null, null);
  }

  @Test
  void createUpdateAndDeleteTask() throws Exception {
    when(service.create(eq(alice), any(StudyTaskCreateRequest.class)))
        .thenReturn(task(1L, "Review", "todo"));
    when(service.update(eq(alice), eq(1L), any(StudyTaskUpdateRequest.class)))
        .thenReturn(task(1L, "Review", "done"));

    mvc.perform(
            post("/api/tasks")
                .principal(auth(alice))
                .contentType("application/json")
                .content("{\"title\":\"Review\",\"status\":\"todo\",\"noteIds\":[3,4]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1));
    verify(service)
        .create(eq(alice), argThat(request -> request.noteIds().equals(List.of(3L, 4L))));
    mvc.perform(
            put("/api/tasks/1")
                .principal(auth(alice))
                .contentType("application/json")
                .content("{\"title\":\"Review\",\"status\":\"done\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("done"));
    mvc.perform(delete("/api/tasks/1").principal(auth(alice))).andExpect(status().isOk());
    verify(service).delete(alice, 1L);
  }

  @Test
  void invalidTaskStatusUsesStableError() throws Exception {
    when(service.create(eq(alice), any(StudyTaskCreateRequest.class)))
        .thenThrow(new InvalidTaskStatusException("later"));

    mvc.perform(
            post("/api/tasks")
                .principal(auth(alice))
                .contentType("application/json")
                .content("{\"title\":\"Review\",\"status\":\"later\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_TASK_STATUS"));
  }

  private static UsernamePasswordAuthenticationToken auth(User user) {
    return new UsernamePasswordAuthenticationToken(user, null, List.of());
  }

  private static StudyTaskResponse task(Long id, String title, String status) {
    return new StudyTaskResponse(id, title, null, status, null, null, null, null);
  }
}
