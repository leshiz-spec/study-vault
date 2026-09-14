package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteRevisionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteRevisionServiceTest {
  private NoteRepository notes;
  private NoteRevisionRepository revisions;
  private NoteService noteService;
  private NoteRevisionService service;
  private User alice;

  @BeforeEach
  void setUp() {
    notes = mock(NoteRepository.class);
    revisions = mock(NoteRevisionRepository.class);
    noteService = mock(NoteService.class);
    service = new NoteRevisionService(notes, revisions, noteService);
    alice = user(1L, "alice");
  }

  @Test
  void listUsesNoteAndUserOwnershipQuery() {
    Note note = note(10L, alice, "Current", "Current body");
    NoteRevision revision = revision(20L, note, "Old", "Old body");
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(revisions.findAllByNote_IdAndNote_User_IdOrderByCreatedAtDesc(10L, 1L))
        .thenReturn(List.of(revision));

    var result = service.list(alice, 10L);

    assertEquals(1, result.size());
    assertEquals("Old", result.get(0).title());
    verify(revisions).findAllByNote_IdAndNote_User_IdOrderByCreatedAtDesc(10L, 1L);
  }

  @Test
  void anotherUsersNoteCannotExposeRevisions() {
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.empty());

    assertThrows(NoteNotFoundException.class, () -> service.list(alice, 10L));
    verifyNoInteractions(revisions);
  }

  @Test
  void revisionFromAnotherNoteOrUserUsesStableNotFound() {
    Note note = note(10L, alice, "Current", "Current body");
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(revisions.findByIdAndNote_IdAndNote_User_Id(99L, 10L, 1L)).thenReturn(Optional.empty());

    assertThrows(NoteNotFoundException.class, () -> service.get(alice, 10L, 99L));
  }

  @Test
  void restoreDelegatesToNoteUpdateSoCurrentStateBecomesNewRevision() {
    Note note = note(10L, alice, "Current", "Current body");
    NoteRevision revision = revision(20L, note, "Old", "Old body");
    NoteResponse restored =
        new NoteResponse(10L, "Old", "Old body", null, "active", false, "not_started", null, null);
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(revisions.findByIdAndNote_IdAndNote_User_Id(20L, 10L, 1L))
        .thenReturn(Optional.of(revision));
    when(noteService.update(eq(alice), eq(10L), any())).thenReturn(restored);

    var result = service.restore(alice, 10L, 20L);

    assertEquals("Old", result.title());
    verify(noteService)
        .update(
            eq(alice),
            eq(10L),
            argThat(
                request -> request.title().equals("Old") && request.content().equals("Old body")));
  }

  private static User user(Long id, String username) {
    User user = new User();
    setId(user, id);
    user.setUsername(username);
    return user;
  }

  private static Note note(Long id, User user, String title, String content) {
    Note note = new Note();
    setId(note, id);
    note.setUser(user);
    note.setTitle(title);
    note.setContent(content);
    return note;
  }

  private static NoteRevision revision(Long id, Note note, String title, String content) {
    NoteRevision revision = new NoteRevision();
    setId(revision, id);
    revision.setNote(note);
    revision.setTitle(title);
    revision.setContent(content);
    return revision;
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
