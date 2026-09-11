package com.example.studyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "study_tasks", indexes = {
        @Index(name = "idx_study_tasks_user_status", columnList = "user_id,status"),
        @Index(name = "idx_study_tasks_user_due_date", columnList = "user_id,due_date")
})
public class StudyTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "study_task_notes",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "note_id"))
    @OrderBy("title ASC")
    private Set<Note> notes = new LinkedHashSet<>();
    @Column(nullable = false, length = 255) private String title;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(nullable = false, length = 20) private String status = "todo";
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    @PrePersist void onCreate() { var now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Set<Note> getNotes() { return notes; }
    public void setNotes(Set<Note> notes) { this.notes = notes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(notes); }
    @Transient public Note getNote() { return notes.isEmpty() ? null : notes.iterator().next(); }
    @Transient public void setNote(Note note) { notes.clear(); if (note != null) notes.add(note); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
