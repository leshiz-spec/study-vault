package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.exception.NoteTagAlreadyExistsException;
import com.example.studyvault.exception.TagNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteTagRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteTagServiceTest {
  private NoteRepository notes;
  private NoteTagRepository noteTags;
  private TagService tags;
  private NoteTagService service;
  private User alice;
  private User bob;
  private Note note;
  private Tag tag;

  @BeforeEach
  void setUp() {
    notes = mock(NoteRepository.class);
    noteTags = mock(NoteTagRepository.class);
    tags = mock(TagService.class);
    service = new NoteTagService(notes, tags, noteTags);
    alice = user(1L);
    bob = user(2L);
    note = note(10L, alice);
    tag = tag(20L, alice);
  }

  @Test
  void duplicateRelationshipIsRejected() {
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(tags.findOwned(alice, 20L)).thenReturn(tag);
    when(noteTags.existsByNoteAndTag(note, tag)).thenReturn(true);
    assertThrows(NoteTagAlreadyExistsException.class, () -> service.add(alice, 10L, 20L));
    verify(noteTags, never()).save(any());
  }

  @Test
  void anotherUsersNoteIsRejectedBeforeRelationshipLookup() {
    when(notes.findByIdAndUser(10L, bob)).thenReturn(Optional.empty());
    assertThrows(NoteNotFoundException.class, () -> service.add(bob, 10L, 20L));
    verifyNoInteractions(tags, noteTags);
  }

  @Test
  void anotherUsersTagIsRejected() {
    when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
    when(tags.findOwned(alice, 20L)).thenThrow(new TagNotFoundException(20L));
    assertThrows(TagNotFoundException.class, () -> service.add(alice, 10L, 20L));
    verifyNoInteractions(noteTags);
  }

  private static User user(Long id) {
    User u = new User();
    setId(u, id);
    return u;
  }

  private static Note note(Long id, User user) {
    Note n = new Note();
    setId(n, id);
    n.setUser(user);
    n.setTitle("Note");
    n.setContent("Body");
    return n;
  }

  private static Tag tag(Long id, User user) {
    Tag t = new Tag();
    setId(t, id);
    t.setUser(user);
    t.setName("java");
    return t;
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
