package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.StudyTaskCreateRequest;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudyTaskServiceTest {
  private StudyTaskRepository tasks;
  private NoteRepository notes;
  private StudyTaskService service;
  private User alice;
  private Note note;

  @BeforeEach
  void setUp() {
    tasks = mock(StudyTaskRepository.class);
    notes = mock(NoteRepository.class);
    service = new StudyTaskService(tasks, notes);
    alice = user(1L, "alice");
    note = note(10L, alice, "Algorithms");
  }

  @Test
  void createTaskScopesOwnerAndCanLinkOwnedNote() {
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(tasks.save(any(StudyTask.class)))
        .thenAnswer(
            invocation -> {
              StudyTask task = invocation.getArgument(0);
              setId(task, 5L);
              return task;
            });

    var result =
        service.create(
            alice,
            new StudyTaskCreateRequest("Review graphs", LocalDate.of(2026, 9, 10), "todo", 10L));

    assertEquals(5L, result.id());
    assertEquals(10L, result.noteId());
    verify(tasks)
        .save(
            argThat(
                task ->
                    task.getUser() == alice
                        && task.getNote() == note
                        && task.getStatus().equals("todo")));
  }

  @Test
  void createTaskCanLinkMultipleOwnedNotesWithoutDuplicates() {
    Note secondNote = note(11L, alice, "Databases");
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(notes.findByIdAndUser(11L, alice)).thenReturn(Optional.of(secondNote));
    when(tasks.save(any(StudyTask.class)))
        .thenAnswer(
            invocation -> {
              StudyTask task = invocation.getArgument(0);
              setId(task, 6L);
              return task;
            });

    var result =
        service.create(
            alice,
            new StudyTaskCreateRequest(
                "Review topics", null, "todo", null, List.of(10L, 11L, 10L)));

    assertEquals(List.of(10L, 11L), result.noteIds());
    assertEquals(List.of("Algorithms", "Databases"), result.noteTitles());
    verify(tasks)
        .save(
            argThat(
                task ->
                    task.getNotes().size() == 2
                        && task.getNotes().containsAll(List.of(note, secondNote))));
  }

  @Test
  void listUsesAuthenticatedOwnerAndFilters() {
    when(tasks.findForUser(alice, "in_progress", LocalDate.of(2026, 9, 10), null, null))
        .thenReturn(List.of());

    service.list(alice, "in_progress", LocalDate.of(2026, 9, 10), null, null);

    verify(tasks).findForUser(alice, "in_progress", LocalDate.of(2026, 9, 10), null, null);
  }

  @Test
  void listOrdersActiveTasksByStatusAndDueDateThenCompletedAndOverdue() {
    LocalDate today = LocalDate.now();
    StudyTask todoSoon = task(1L, alice, "Todo soon", "todo", today.plusDays(1));
    StudyTask overdue = task(2L, alice, "Overdue", "in_progress", today.minusDays(1));
    StudyTask completed = task(3L, alice, "Completed", "done", today.minusDays(2));
    StudyTask inProgressLater =
        task(4L, alice, "In progress later", "in_progress", today.plusDays(4));
    StudyTask inProgressSoon =
        task(5L, alice, "In progress soon", "in_progress", today.plusDays(2));
    StudyTask todoWithoutDate = task(6L, alice, "Todo without date", "todo", null);
    StudyTask todoLater = task(7L, alice, "Todo later", "todo", today.plusDays(3));
    when(tasks.findForUser(alice, null, null, null, null))
        .thenReturn(
            List.of(
                overdue,
                todoWithoutDate,
                todoLater,
                completed,
                inProgressLater,
                todoSoon,
                inProgressSoon));

    var result = service.list(alice, null, null, null, null);

    assertEquals(
        List.of(
            "In progress soon",
            "In progress later",
            "Todo soon",
            "Todo later",
            "Todo without date",
            "Completed",
            "Overdue"),
        result.stream().map(response -> response.title()).toList());
  }

  @Test
  void updateTaskAndDeleteOnlyUseOwnedTask() {
    StudyTask task = task(5L, alice, "Old task");
    when(tasks.findByIdAndUser(5L, alice)).thenReturn(Optional.of(task));
    when(tasks.save(task)).thenReturn(task);

    var updated =
        service.update(
            alice,
            5L,
            new StudyTaskUpdateRequest("New task", LocalDate.of(2026, 9, 12), "done", null));
    service.delete(alice, 5L);

    assertEquals("New task", updated.title());
    assertEquals("done", updated.status());
    assertNull(updated.noteId());
    verify(tasks).delete(task);
  }

  @Test
  void invalidTaskStatusIsRejected() {
    assertThrows(
        InvalidTaskStatusException.class,
        () -> service.create(alice, new StudyTaskCreateRequest("Task", null, "later", null)));
    verifyNoInteractions(tasks, notes);
  }

  @Test
  void linkingAnotherUsersNoteFails() {
    when(notes.findByIdAndUser(99L, alice)).thenReturn(Optional.empty());

    assertThrows(
        NoteNotFoundException.class,
        () -> service.create(alice, new StudyTaskCreateRequest("Task", null, null, 99L)));
    verifyNoInteractions(tasks);
  }

  @Test
  void linkingMultipleNotesFailsIfAnyNoteIsNotOwned() {
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(notes.findByIdAndUser(99L, alice)).thenReturn(Optional.empty());

    assertThrows(
        NoteNotFoundException.class,
        () ->
            service.create(
                alice, new StudyTaskCreateRequest("Task", null, null, null, List.of(10L, 99L))));
    verify(tasks, never()).save(any());
  }

  @Test
  void missingTaskUsesStableNotFound() {
    when(tasks.findByIdAndUser(404L, alice)).thenReturn(Optional.empty());
    assertThrows(TaskNotFoundException.class, () -> service.delete(alice, 404L));
  }

  private static User user(Long id, String username) {
    User user = new User();
    setId(user, id);
    user.setUsername(username);
    return user;
  }

  private static Note note(Long id, User user, String title) {
    Note note = new Note();
    setId(note, id);
    note.setUser(user);
    note.setTitle(title);
    note.setContent("Body");
    return note;
  }

  private static StudyTask task(Long id, User user, String title) {
    StudyTask task = new StudyTask();
    setId(task, id);
    task.setUser(user);
    task.setTitle(title);
    task.setStatus("todo");
    return task;
  }

  private static StudyTask task(
      Long id, User user, String title, String status, LocalDate dueDate) {
    StudyTask task = task(id, user, title);
    task.setStatus(status);
    task.setDueDate(dueDate);
    return task;
  }

  private static void setId(Object target, Long id) {
    try {
      var field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }
}
