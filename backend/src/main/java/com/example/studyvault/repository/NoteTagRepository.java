package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.NoteTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteTagRepository extends JpaRepository<NoteTag, NoteTagId> {
  List<NoteTag> findAllByNote(Note note);

  boolean existsByNoteAndTag(Note note, com.example.studyvault.entity.Tag tag);
}
