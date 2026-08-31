package com.example.studyvault.repository;

import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.NoteTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteTagRepository extends JpaRepository<NoteTag, NoteTagId> { }
