package com.example.studyvault.dto;

import java.util.List;

/** Paged response returned by the note search endpoint. */
public record NoteSearchResponse(
        List<NoteResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
