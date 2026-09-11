package com.example.studyvault.controller;

import com.example.studyvault.dto.*;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tags")
public class TagController {
    private final TagService tags;
    public TagController(TagService tags) { this.tags = tags; }
    @GetMapping public ApiResponse<List<TagResponse>> list(Authentication a) { return ApiResponse.success(tags.list(user(a))); }
    @PostMapping public ApiResponse<TagResponse> create(Authentication a, @Valid @RequestBody TagCreateRequest r) { return ApiResponse.success(tags.create(user(a), r)); }
    @PutMapping("/{id}") public ApiResponse<TagResponse> update(Authentication a, @PathVariable Long id, @Valid @RequestBody TagUpdateRequest r) { return ApiResponse.success(tags.update(user(a), id, r)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(Authentication a, @PathVariable Long id) { tags.delete(user(a), id); return ApiResponse.success(null); }
    private User user(Authentication a) { if (a == null || !(a.getPrincipal() instanceof User u)) throw new UnauthorizedException(); return u; }
}
