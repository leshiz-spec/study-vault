package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.dto.ReviewStatusRequest;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteRevisionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteServiceTest {
    private NoteRepository repository;
    private NoteRevisionRepository revisions;
    private NoteService service;
    private User user;

    @BeforeEach
    void setUp() {
        repository = mock(NoteRepository.class);
        revisions = mock(NoteRevisionRepository.class);
        service = new NoteService(repository, null, revisions);
        user = user(1L, "alice");
    }

    @Test
    void createSetsAuthenticatedOwnerAndReturnsDto() {
        when(repository.save(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            setId(note, 10L);
            return note;
        });

        var result = service.create(user, new NoteCreateRequest("Title", "Body"));

        assertEquals(10L, result.id());
        assertEquals("Title", result.title());
        verify(repository).save(argThat(note -> note.getUser() == user && note.getTitle().equals("Title")));
    }

    @Test
    void listOnlyQueriesAuthenticatedUser() {
        Note note = note(10L, user, "Mine", "Body");
        when(repository.findAllByUserAndStatusOrderByUpdatedAtDesc(user, "active")).thenReturn(List.of(note));
        assertEquals(List.of("Mine"), service.list(user).stream().map(r -> r.title()).toList());
        verify(repository).findAllByUserAndStatusOrderByUpdatedAtDesc(user, "active");
    }

    @Test
    void searchUsesPagedDatabaseSpecification() {
        Note note = note(10L, user, "Algorithms", "Graph search");
        var pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "updatedAt"));
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(note), pageable, 6));

        var result = service.search(user, "graph", "math", true, "active", pageable);

        assertEquals(List.of("Algorithms"), result.content().stream().map(r -> r.title()).toList());
        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(6, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void updateOwnNote() {
        Note note = note(10L, user, "Old", "Old body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);
        var result = service.update(user, 10L, new NoteUpdateRequest("New", "New body"));
        assertEquals("New", result.title());
        assertEquals("New body", result.content());
    }

    @Test
    void updateChangedNoteCreatesRevisionFromPreviousState() {
        Note note = note(10L, user, "Old", "Old body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);
        when(revisions.save(any(NoteRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(user, 10L, new NoteUpdateRequest("New", "New body"));

        verify(revisions).save(argThat(revision -> revision.getNote() == note
                && revision.getTitle().equals("Old")
                && revision.getContent().equals("Old body")));
    }

    @Test
    void updateWithoutChangesDoesNotCreateRevision() {
        Note note = note(10L, user, "Title", "Body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);

        service.update(user, 10L, new NoteUpdateRequest("Title", "Body"));

        verifyNoInteractions(revisions);
    }

    @Test
    void deleteOwnNoteSoftDeletes() {
        Note note = note(10L, user, "Title", "Body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        service.delete(user, 10L);
        assertEquals("trash", note.getStatus());
        verify(repository).save(note);
    }

    @Test
    void permanentlyDeleteOwnTrashedNote() {
        Note note = note(10L, user, "Title", "Body");
        note.setStatus("trash");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));

        service.permanentlyDelete(user, 10L);

        verify(repository).delete(note);
        verify(repository, never()).save(any(Note.class));
    }

    @Test
    void permanentlyDeleteDoesNotRemoveActiveNote() {
        Note note = note(10L, user, "Title", "Body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));

        assertThrows(NoteNotFoundException.class, () -> service.permanentlyDelete(user, 10L));
        verify(repository, never()).delete(any(Note.class));
    }

    @Test
    void favoriteOwnNote() {
        Note note = note(10L, user, "Title", "Body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);
        var result = service.favorite(user, 10L);
        assertTrue(note.isFavorite());
        assertTrue(result.favorite());
        verify(repository).save(note);
    }

    @Test
    void restoreOwnNote() {
        Note note = note(10L, user, "Title", "Body");
        note.setStatus("trash");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);
        var result = service.restore(user, 10L);
        assertEquals("active", note.getStatus());
        assertEquals("active", result.status());
        verify(repository).save(note);
    }

    @Test
    void updateReviewStatusScopesNoteToOwner() {
        Note note = note(10L, user, "Title", "Body");
        when(repository.findByIdAndUser(10L, user)).thenReturn(Optional.of(note));
        when(repository.save(note)).thenReturn(note);

        var result = service.updateReviewStatus(user, 10L, new ReviewStatusRequest("mastered"));

        assertEquals("mastered", result.reviewStatus());
        verify(repository).findByIdAndUser(10L, user);
    }

    @Test
    void invalidReviewStatusIsRejected() {
        assertThrows(com.example.studyvault.exception.InvalidReviewStatusException.class,
                () -> service.updateReviewStatus(user, 10L, new ReviewStatusRequest("reviewed")));
        verifyNoInteractions(repository);
    }

    @Test
    void favoriteAndRestoreMissingNoteUseStableNotFound() {
        when(repository.findByIdAndUser(404L, user)).thenReturn(Optional.empty());
        assertThrows(NoteNotFoundException.class, () -> service.favorite(user, 404L));
        assertThrows(NoteNotFoundException.class, () -> service.restore(user, 404L));
    }

    @Test
    void nonexistentOrOtherUsersNoteUsesStableNotFound() {
        when(repository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());
        assertThrows(NoteNotFoundException.class, () -> service.get(user, 99L));
        assertThrows(NoteNotFoundException.class, () -> service.update(user, 99L, new NoteUpdateRequest("x", "y")));
        assertThrows(NoteNotFoundException.class, () -> service.delete(user, 99L));
    }

    private static User user(Long id, String username) { User u = new User(); setId(u, id); u.setUsername(username); return u; }
    private static Note note(Long id, User user, String title, String content) { Note n = new Note(); setId(n, id); n.setUser(user); n.setTitle(title); n.setContent(content); return n; }
    private static void setId(Object target, Long id) { try { var field = target.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(target, id); } catch (ReflectiveOperationException e) { throw new AssertionError(e); } }
}
