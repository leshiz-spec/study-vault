package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findAllByUserOrderByUpdatedAtDesc(User user);
    List<Note> findAllByUserAndStatusOrderByUpdatedAtDesc(User user, String status);
    List<Note> findAllByUserAndFavoriteOrderByUpdatedAtDesc(User user, boolean favorite);
}
