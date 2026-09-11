package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRevisionRepository extends JpaRepository<NoteRevision, Long> {
    List<NoteRevision> findAllByNote_IdAndNote_User_IdOrderByCreatedAtDesc(Long noteId, Long userId);
    java.util.Optional<NoteRevision> findByIdAndNote_IdAndNote_User_Id(Long revisionId, Long noteId, Long userId);
}
