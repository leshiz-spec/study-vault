package com.example.studyvault.service;

import com.example.studyvault.dto.StudyTaskCreateRequest;
import com.example.studyvault.dto.StudyTaskResponse;
import com.example.studyvault.dto.StudyTaskUpdateRequest;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.StudyTask;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.InvalidTaskStatusException;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.exception.TaskNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.StudyTaskRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyTaskService {
  private static final Set<String> TASK_STATUSES = Set.of("todo", "in_progress", "done");
  private final StudyTaskRepository tasks;
  private final NoteRepository notes;

  public StudyTaskService(StudyTaskRepository tasks, NoteRepository notes) {
    this.tasks = tasks;
    this.notes = notes;
  }

  @Transactional(readOnly = true)
  public List<StudyTaskResponse> list(
      User user, String status, LocalDate dueDate, LocalDate dueAfter, LocalDate dueBefore) {
    validateStatus(status);
    LocalDate today = LocalDate.now();
    return tasks.findForUser(user, status, dueDate, dueAfter, dueBefore).stream()
        .sorted(
            Comparator.comparingInt((StudyTask task) -> displayPriority(task, today))
                .thenComparing(
                    StudyTask::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(StudyTaskResponse::from)
        .toList();
  }

  @Transactional
  public StudyTaskResponse create(User user, StudyTaskCreateRequest request) {
    String status =
        request.status() == null || request.status().isBlank() ? "todo" : request.status();
    validateStatus(status);
    StudyTask task = new StudyTask();
    task.setUser(user);
    apply(
        task,
        user,
        request.title(),
        request.dueDate(),
        status,
        request.noteIds(),
        request.noteId());
    return StudyTaskResponse.from(tasks.save(task));
  }

  @Transactional
  public StudyTaskResponse update(User user, Long id, StudyTaskUpdateRequest request) {
    StudyTask task = findOwned(user, id);
    String status =
        request.status() == null || request.status().isBlank()
            ? task.getStatus()
            : request.status();
    validateStatus(status);
    apply(
        task,
        user,
        request.title(),
        request.dueDate(),
        status,
        request.noteIds(),
        request.noteId());
    return StudyTaskResponse.from(tasks.save(task));
  }

  @Transactional
  public void delete(User user, Long id) {
    tasks.delete(findOwned(user, id));
  }

  private void apply(
      StudyTask task,
      User user,
      String title,
      LocalDate dueDate,
      String status,
      List<Long> noteIds,
      Long legacyNoteId) {
    task.setTitle(title);
    task.setDueDate(dueDate);
    task.setStatus(status);
    List<Long> requestedIds = noteIds == null ? List.of() : noteIds;
    if (requestedIds.isEmpty() && legacyNoteId != null) requestedIds = List.of(legacyNoteId);
    Set<Note> linkedNotes = new LinkedHashSet<>();
    for (Long noteId : requestedIds) {
      if (noteId != null) linkedNotes.add(findOwnedNote(user, noteId));
    }
    task.setNotes(linkedNotes);
  }

  private Note findOwnedNote(User user, Long id) {
    return notes.findByIdAndUser(id, user).orElseThrow(() -> new NoteNotFoundException(id));
  }

  private StudyTask findOwned(User user, Long id) {
    return tasks.findByIdAndUser(id, user).orElseThrow(() -> new TaskNotFoundException(id));
  }

  private int displayPriority(StudyTask task, LocalDate today) {
    if (!"done".equals(task.getStatus())
        && task.getDueDate() != null
        && task.getDueDate().isBefore(today)) {
      return 3;
    }
    return switch (task.getStatus()) {
      case "in_progress" -> 0;
      case "todo" -> 1;
      case "done" -> 2;
      default -> 3;
    };
  }

  private void validateStatus(String status) {
    if (status != null && !TASK_STATUSES.contains(status))
      throw new InvalidTaskStatusException(status);
  }
}
