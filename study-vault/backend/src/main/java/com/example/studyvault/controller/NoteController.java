package com.example.studyvault.controller;

import com.example.studyvault.dto.ApiResponse;
import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.dto.NoteTagRequest;
import com.example.studyvault.dto.NoteSummaryResponse;
import com.example.studyvault.dto.NoteSummarySaveRequest;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.UnauthorizedException;
import com.example.studyvault.service.NoteService;
import com.example.studyvault.service.NoteTagService;
import com.example.studyvault.service.NoteFileService;
import com.example.studyvault.service.NoteSummaryService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService notes; private final NoteTagService noteTags; private final NoteFileService files; private final NoteSummaryService summaries;

    @Autowired
    public NoteController(NoteService notes, NoteTagService noteTags, NoteFileService files, NoteSummaryService summaries) { this.notes = notes; this.noteTags = noteTags; this.files = files; this.summaries = summaries; }
    public NoteController(NoteService notes, NoteTagService noteTags, NoteFileService files) { this(notes, noteTags, files, null); }
    public NoteController(NoteService notes, NoteTagService noteTags) { this(notes, noteTags, null, null); }
    public NoteController(NoteService notes) { this(notes, null, null, null); }

    @GetMapping
    public ApiResponse<List<NoteResponse>> list(Authentication authentication) {
        return ApiResponse.success(notes.list(currentUser(authentication)));
    }

    @GetMapping("/trash")
    public ApiResponse<List<NoteResponse>> trash(Authentication authentication) {
        return ApiResponse.success(notes.trash(currentUser(authentication)));
    }

    @PostMapping
    public ApiResponse<NoteResponse> create(Authentication authentication, @Valid @RequestBody NoteCreateRequest request) {
        return ApiResponse.success(notes.create(currentUser(authentication), request));
    }

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportAll(Authentication authentication) {
        User user = currentUser(authentication);
        StreamingResponseBody body = output -> files.writeZip(user, output);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("studyvault-notes.zip", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/zip")).body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<NoteResponse> importMarkdown(Authentication authentication, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(files.importMarkdown(currentUser(authentication), file));
    }

    @GetMapping("/{id}")
    public ApiResponse<NoteResponse> get(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(notes.get(currentUser(authentication), id));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<StreamingResponseBody> export(Authentication authentication, @PathVariable Long id) {
        User user = currentUser(authentication);
        String filename = files.filename(user, id);
        StreamingResponseBody body = output -> files.writeMarkdown(user, id, output);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.TEXT_MARKDOWN).body(body);
    }

    @PutMapping("/{id}")
    public ApiResponse<NoteResponse> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody NoteUpdateRequest request) {
        return ApiResponse.success(notes.update(currentUser(authentication), id, request));
    }

    @PostMapping("/{id}/summarize")
    public ApiResponse<NoteSummaryResponse> summarize(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(summaries.summarize(currentUser(authentication), id));
    }

    /** Persists a generated draft only after the user explicitly confirms it. */
    @PutMapping("/{id}/summary")
    public ApiResponse<NoteResponse> saveSummary(Authentication authentication, @PathVariable Long id,
                                                  @Valid @RequestBody NoteSummarySaveRequest request) {
        return ApiResponse.success(summaries.save(currentUser(authentication), id, request.summary()));
    }

    @PostMapping("/{id}/tags")
    public ApiResponse<NoteResponse> addTag(Authentication authentication, @PathVariable Long id, @Valid @RequestBody NoteTagRequest request) {
        return ApiResponse.success(noteTags.add(currentUser(authentication), id, request.tagId()));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public ApiResponse<Void> removeTag(Authentication authentication, @PathVariable Long id, @PathVariable Long tagId) {
        noteTags.remove(currentUser(authentication), id, tagId); return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        notes.delete(currentUser(authentication), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<NoteResponse> favorite(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(notes.favorite(currentUser(authentication), id));
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<NoteResponse> restore(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(notes.restore(currentUser(authentication), id));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) throw new UnauthorizedException();
        return user;
    }

}
