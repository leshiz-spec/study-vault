package com.example.studyvault.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="tags", uniqueConstraints=@UniqueConstraint(name="uk_tags_user_name", columnNames={"user_id","name"}))
public class Tag {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false, length=80) private String name;
    @Column(length=20) private String color;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @PrePersist void onCreate(){createdAt=OffsetDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getColor(){return color;} public void setColor(String v){color=v;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
