package com.example.studyvault.repository;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {
  List<Note> findAllByUserOrderByUpdatedAtDesc(User user);

  List<Note> findAllByUserAndStatusOrderByUpdatedAtDesc(User user, String status);

  List<Note> findAllByUserAndFavoriteOrderByUpdatedAtDesc(User user, boolean favorite);

  Optional<Note> findByIdAndUser(Long id, User user);
}
