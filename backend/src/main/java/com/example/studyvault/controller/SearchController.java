package com.example.studyvault.controller;

import com.example.studyvault.dto.ApiResponse;
import com.example.studyvault.dto.NoteSearchResponse;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.InvalidRequestException;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.service.NoteService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {
  private final NoteService notes;

  public SearchController(NoteService notes) {
    this.notes = notes;
  }

  @GetMapping
  public ApiResponse<NoteSearchResponse> search(
      Authentication authentication,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) Boolean favorite,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "updatedAt,desc") String sort) {
    if (page < 0) throw new InvalidRequestException("page must be at least 0");
    if (size < 1 || size > 100) throw new InvalidRequestException("size must be between 1 and 100");
    return ApiResponse.success(
        notes.search(
            currentUser(authentication),
            q,
            tag,
            favorite,
            status,
            PageRequest.of(page, size, parseSort(sort))));
  }

  private Sort parseSort(String value) {
    String[] parts = value == null ? new String[0] : value.split(",", 2);
    String property = parts.length > 0 ? parts[0].trim() : "updatedAt";
    if (!property.equals("updatedAt") && !property.equals("createdAt")) property = "updatedAt";
    String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "desc";
    return Sort.by("asc".equals(direction) ? Sort.Direction.ASC : Sort.Direction.DESC, property);
  }

  private User currentUser(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof User user))
      throw new UnauthorizedException();
    return user;
  }
}
