package com.example.studyvault.repository;

import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.NoteTagId;
import com.example.studyvault.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteTagRepository extends JpaRepository<NoteTag, NoteTagId> {
    List<NoteTag> findAllByNote(Note note);
    boolean existsByNoteAndTag(Note note, com.example.studyvault.entity.Tag tag);
}
