package com.example.studyvault.repository;

import com.example.studyvault.entity.StudyTask;
import com.example.studyvault.entity.User;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTaskRepository
    extends JpaRepository<StudyTask, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<StudyTask> {
  /** Builds only requested predicates so PostgreSQL never has to type a nullable parameter. */
  default List<StudyTask> findForUser(
      User user, String status, LocalDate dueDate, LocalDate dueAfter, LocalDate dueBefore) {
    Specification<StudyTask> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(criteriaBuilder.equal(root.get("user"), user));
          if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
          if (dueDate != null) predicates.add(criteriaBuilder.equal(root.get("dueDate"), dueDate));
          if (dueAfter != null)
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueAfter));
          if (dueBefore != null)
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueBefore));
          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    Sort sort = Sort.by(Sort.Order.asc("dueDate").nullsLast(), Sort.Order.desc("updatedAt"));
    return findAll(specification, sort);
  }

  Optional<StudyTask> findByIdAndUser(Long id, User user);
}
