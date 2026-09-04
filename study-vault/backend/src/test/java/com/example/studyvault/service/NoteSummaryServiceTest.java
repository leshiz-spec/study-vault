package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.AiTimeoutException;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteSummaryServiceTest {
    private NoteRepository notes;
    private AiSummaryProvider provider;
    private NoteService noteService;
    private NoteSummaryService service;
    private User alice;
    private Note note;

    @BeforeEach
    void setUp() {
        notes = mock(NoteRepository.class);
        provider = mock(AiSummaryProvider.class);
        noteService = mock(NoteService.class);
        service = new NoteSummaryService(notes, provider, noteService);
        alice = new User();
        setId(alice, 1L);
        alice.setUsername("alice");
        note = new Note();
        setId(note, 10L);
        note.setUser(alice);
        note.setTitle("Title");
        note.setContent("Private note content");
    }

    @Test
    void summarizeChecksOwnershipBeforeCallingProviderAndReturnsDraft() {
        when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
        when(provider.summarize("Private note content")).thenReturn("Draft summary");

        var result = service.summarize(alice, 10L);

        assertEquals(new com.example.studyvault.dto.NoteSummaryResponse(10L, "Draft summary"), result);
        verify(provider).summarize("Private note content");
    }

    @Test
    void otherUsersNoteIsNotSentToProvider() {
        when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> service.summarize(alice, 10L));
        verifyNoInteractions(provider);
    }

    @Test
    void providerTimeoutIsPropagatedAsStableError() {
        when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
        when(provider.summarize(any())).thenThrow(new AiTimeoutException());

        assertThrows(AiTimeoutException.class, () -> service.summarize(alice, 10L));
    }

    @Test
    void savePersistsOnlyExplicitDraftForOwnedNote() {
        when(notes.findByIdAndUser(10L, alice)).thenReturn(Optional.of(note));
        NoteResponse saved = new NoteResponse(10L, "Title", "Private note content", "Saved summary", "active", false, "not_started", null, null);
        when(noteService.get(alice, 10L)).thenReturn(saved);

        var result = service.save(alice, 10L, "  Saved summary  ");

        assertEquals("Saved summary", note.getSummary());
        assertEquals("Saved summary", result.summary());
        verify(notes).save(note);
    }

    private static void setId(Object target, Long id) {
        try {
            var field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
