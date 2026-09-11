package com.example.studyvault.repository;

import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAllByUserOrderByNameAsc(User user);
    Optional<Tag> findByUserAndName(User user, String name);
    Optional<Tag> findByIdAndUser(Long id, User user);
}
