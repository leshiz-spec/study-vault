package com.example.studyvault.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable public record NoteTagId(Long noteId, Long tagId) implements Serializable { }
