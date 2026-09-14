package com.example.studyvault.controller;

import com.example.studyvault.dto.ApiResponse;
import com.example.studyvault.dto.StudyTaskCreateRequest;
import com.example.studyvault.dto.StudyTaskResponse;
import com.example.studyvault.dto.StudyTaskUpdateRequest;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.service.StudyTaskService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class StudyTaskController {
  private final StudyTaskService tasks;

  public StudyTaskController(StudyTaskService tasks) {
    this.tasks = tasks;
  }

  @GetMapping
  public ApiResponse<List<StudyTaskResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueAfter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueBefore) {
    return ApiResponse.success(
        tasks.list(user(authentication), status, dueDate, dueAfter, dueBefore));
  }

  @PostMapping
  public ApiResponse<StudyTaskResponse> create(
      Authentication authentication, @Valid @RequestBody StudyTaskCreateRequest request) {
    return ApiResponse.success(tasks.create(user(authentication), request));
  }

  @PutMapping("/{id}")
  public ApiResponse<StudyTaskResponse> update(
      Authentication authentication,
      @PathVariable Long id,
      @Valid @RequestBody StudyTaskUpdateRequest request) {
    return ApiResponse.success(tasks.update(user(authentication), id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
    tasks.delete(user(authentication), id);
    return ApiResponse.success(null);
  }

  private User user(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof User user))
      throw new UnauthorizedException();
    return user;
  }
}
