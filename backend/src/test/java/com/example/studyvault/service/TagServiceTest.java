package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.TagCreateRequest;
import com.example.studyvault.dto.TagUpdateRequest;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.TagAlreadyExistsException;
import com.example.studyvault.exception.TagNotFoundException;
import com.example.studyvault.repository.TagRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagServiceTest {
  private TagRepository repository;
  private TagService service;
  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    repository = mock(TagRepository.class);
    service = new TagService(repository);
    alice = user(1L);
    bob = user(2L);
  }

  @Test
  void createAssignsAuthenticatedOwner() {
    when(repository.findByUserAndName(alice, "java")).thenReturn(Optional.empty());
    when(repository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));
    service.create(alice, new TagCreateRequest("java", "#fff"));
    verify(repository).save(argThat(t -> t.getUser() == alice && t.getName().equals("java")));
  }

  @Test
  void duplicateNameIsRejected() {
    when(repository.findByUserAndName(alice, "java")).thenReturn(Optional.of(tag(alice, "java")));
    assertThrows(
        TagAlreadyExistsException.class,
        () -> service.create(alice, new TagCreateRequest("java", null)));
  }

  @Test
  void anotherUsersTagIsNotFound() {
    when(repository.findByIdAndUser(3L, alice)).thenReturn(Optional.empty());
    assertThrows(
        TagNotFoundException.class,
        () -> service.update(alice, 3L, new TagUpdateRequest("x", null)));
    assertThrows(TagNotFoundException.class, () -> service.delete(alice, 3L));
  }

  private static User user(Long id) {
    User u = new User();
    try {
      var f = User.class.getDeclaredField("id");
      f.setAccessible(true);
      f.set(u, id);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    return u;
  }

  private static Tag tag(User u, String name) {
    Tag t = new Tag();
    t.setUser(u);
    t.setName(name);
    return t;
  }
}
