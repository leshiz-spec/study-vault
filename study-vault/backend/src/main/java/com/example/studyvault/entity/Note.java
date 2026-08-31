package com.example.studyvault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "notes", indexes = {@Index(name="idx_notes_user_updated_at", columnList="user_id,updated_at"), @Index(name="idx_notes_user_status", columnList="user_id,status"), @Index(name="idx_notes_user_favorite", columnList="user_id,is_favorite")})
public class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false) private String title;
    @Column(nullable=false, columnDefinition="TEXT") private String content;
    @Column(columnDefinition="TEXT") private String summary;
    @Column(nullable=false, length=20) private String status = "active";
    @Column(name="is_favorite", nullable=false) private boolean favorite;
    @Column(name="review_status", nullable=false, length=20) private String reviewStatus = "not_started";
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate(){var now=OffsetDateTime.now();createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getContent(){return content;} public void setContent(String v){content=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public boolean isFavorite(){return favorite;} public void setFavorite(boolean v){favorite=v;} public String getReviewStatus(){return reviewStatus;} public void setReviewStatus(String v){reviewStatus=v;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
