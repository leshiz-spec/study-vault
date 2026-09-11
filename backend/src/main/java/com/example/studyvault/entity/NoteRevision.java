package com.example.studyvault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** A point-in-time snapshot of a note's Markdown content. */
@Entity
@Table(name = "note_revisions", indexes = @Index(name = "idx_note_revisions_note_created_at", columnList = "note_id,created_at"))
public class NoteRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Note getNote() { return note; }
    public void setNote(Note note) { this.note = note; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
