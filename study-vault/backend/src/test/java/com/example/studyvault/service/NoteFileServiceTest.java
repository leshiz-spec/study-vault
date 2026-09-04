package com.example.studyvault.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.InvalidMarkdownFileException;
import com.example.studyvault.exception.FileTooLargeException;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class NoteFileServiceTest {
    private NoteRepository notes;
    private NoteService noteService;
    private NoteFileService files;
    private User alice;

    @BeforeEach
    void setUp() {
        notes = mock(NoteRepository.class);
        noteService = mock(NoteService.class);
        files = new NoteFileService(notes, noteService);
        alice = new User();
        alice.setUsername("alice");
    }

    @Test
    void singleExportUsesOwnedNoteAndMarkdownFormat() {
        Note note = note(7L, alice, "My / Notes", "**body**");
        when(notes.findByIdAndUser(7L, alice)).thenReturn(Optional.of(note));
        assertEquals("# My / Notes\n\n**body**\n", files.markdown(alice, 7L));
        assertEquals("My Notes-7.md", NoteFileService.safeFilename("My / Notes", 7L));
        verify(notes).findByIdAndUser(7L, alice);
    }

    @Test
    void exportDoesNotRevealAnotherUsersNote() {
        when(notes.findByIdAndUser(7L, alice)).thenReturn(Optional.empty());
        assertThrows(NoteNotFoundException.class, () -> files.markdown(alice, 7L));
    }

    @Test
    void zipExportContainsOnlyOwnedNotes() throws Exception {
        User bob = new User();
        bob.setUsername("bob");
        when(notes.findAllByUserOrderByUpdatedAtDesc(alice)).thenReturn(List.of(
                note(1L, alice, "First", "one"), note(2L, bob, "Other user", "private"),
                note(3L, alice, "Second", "two")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        files.writeZip(alice, output);
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(output.toByteArray()))) {
            assertEquals("First-1.md", zip.getNextEntry().getName());
            assertTrue(new String(zip.readAllBytes(), StandardCharsets.UTF_8).contains("# First"));
            assertEquals("Second-3.md", zip.getNextEntry().getName());
            assertTrue(new String(zip.readAllBytes(), StandardCharsets.UTF_8).contains("# Second"));
            assertNull(zip.getNextEntry());
        }
        verify(notes).findAllByUserOrderByUpdatedAtDesc(alice);
    }

    @Test
    void importMarkdownCreatesOwnedNoteFromHeading() {
        var file = new MockMultipartFile("file", "course.md", "text/markdown", "# Imported\n\nLesson body".getBytes(StandardCharsets.UTF_8));
        when(noteService.create(eq(alice), any())).thenReturn(new NoteResponse(3L, "Imported", "Lesson body", null, "active", false, "not_started", null, null));
        NoteResponse result = files.importMarkdown(alice, file);
        assertEquals("Imported", result.title());
        verify(noteService).create(eq(alice), argThat(request -> request.title().equals("Imported") && request.content().equals("Lesson body")));
    }

    @Test
    void importRejectsNonMarkdownFile() {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "body".getBytes(StandardCharsets.UTF_8));
        assertThrows(InvalidMarkdownFileException.class, () -> files.importMarkdown(alice, file));
        verifyNoInteractions(noteService);
    }

    @Test
    void importRejectsFilesLargerThanConfiguredLimit() {
        byte[] oversized = new byte[(int) NoteFileService.MAX_IMPORT_BYTES + 1];
        var file = new MockMultipartFile("file", "large.md", "text/markdown", oversized);
        assertThrows(FileTooLargeException.class, () -> files.importMarkdown(alice, file));
        verifyNoInteractions(noteService);
    }

    @Test
    void generatedNamesCannotContainPathTraversal() {
        String generated = NoteFileService.safeFilename("../../private/secret", 11L);
        assertFalse(generated.contains("/"));
        assertFalse(generated.contains("\\"));
        assertTrue(generated.endsWith("-11.md"));
    }

    private static Note note(Long id, User user, String title, String content) {
        Note note = new Note();
        try { var field = Note.class.getDeclaredField("id"); field.setAccessible(true); field.set(note, id); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        note.setUser(user); note.setTitle(title); note.setContent(content); return note;
    }
}
