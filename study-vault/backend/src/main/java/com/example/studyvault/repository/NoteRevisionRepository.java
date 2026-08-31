package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRevisionRepository extends JpaRepository<NoteRevision, Long> {
    List<NoteRevision> findAllByNoteOrderByCreatedAtDesc(Note note);
}
