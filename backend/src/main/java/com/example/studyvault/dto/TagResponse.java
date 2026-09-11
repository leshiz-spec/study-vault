package com.example.studyvault.dto;

import com.example.studyvault.entity.Tag;
import java.time.OffsetDateTime;

public record TagResponse(Long id, String name, String color, OffsetDateTime createdAt) {
    public static TagResponse from(Tag tag) { return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), tag.getCreatedAt()); }
}
