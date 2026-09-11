package com.example.studyvault.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.studyvault.exception.GlobalExceptionHandler;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.service.NoteFileService;
import com.example.studyvault.service.NoteService;
import com.example.studyvault.service.NoteTagService;
import com.example.studyvault.entity.User;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

class NoteFileControllerTest {
    private MockMvc mvc;
    private NoteFileService files;
    private User alice;

    @BeforeEach
    void setUp() {
        files = mock(NoteFileService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NoteController(mock(NoteService.class), mock(NoteTagService.class), files))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        alice = new User(); alice.setUsername("alice");
    }

    @Test
    void singleExportSetsMarkdownDownloadHeaders() throws Exception {
        when(files.filename(alice, 7L)).thenReturn("My-7.md");
        doAnswer(invocation -> { ((OutputStream) invocation.getArgument(2)).write("# My\n".getBytes()); return null; })
                .when(files).writeMarkdown(eq(alice), eq(7L), any(OutputStream.class));
        mvc.perform(get("/api/notes/7/export").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("My-7.md")))
                .andExpect(content().string("# My\n"));
    }

    @Test
    void zipExportSetsArchiveHeadersAndStreamsArchive() throws Exception {
        doAnswer(invocation -> {
            ((OutputStream) invocation.getArgument(1)).write("zip-bytes".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(files).writeZip(eq(alice), any(OutputStream.class));

        var response = new NoteController(mock(NoteService.class), mock(NoteTagService.class), files)
                .exportAll(auth(alice));
        var output = new java.io.ByteArrayOutputStream();
        response.getBody().writeTo(output);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/zip", response.getHeaders().getFirst("Content-Type"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("studyvault-notes.zip"));
        assertArrayEquals("zip-bytes".getBytes(StandardCharsets.UTF_8), output.toByteArray());
        verify(files).writeZip(eq(alice), any(OutputStream.class));
    }

    @Test
    void importEndpointAcceptsMarkdownMultipartAndReturnsCreatedNote() throws Exception {
        when(files.importMarkdown(eq(alice), any(MockMultipartFile.class)))
                .thenReturn(new NoteResponse(3L, "Imported", "Body", null, "active", false, "not_started", null, null));
        MockMultipartFile upload = new MockMultipartFile("file", "lesson.md", "text/markdown",
                "# Imported\n\nBody".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/notes/import").file(upload).principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.title").value("Imported"));
        verify(files).importMarkdown(eq(alice), any(MockMultipartFile.class));
    }

    @Test
    void importEndpointReturnsStableErrorForInvalidUpload() throws Exception {
        when(files.importMarkdown(eq(alice), any(MockMultipartFile.class)))
                .thenThrow(new com.example.studyvault.exception.InvalidMarkdownFileException("Only .md Markdown files are supported"));
        MockMultipartFile upload = new MockMultipartFile("file", "lesson.txt", "text/plain", "Body".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/notes/import").file(upload).principal(auth(alice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_MARKDOWN_FILE"));
    }

    private static UsernamePasswordAuthenticationToken auth(User user) { return new UsernamePasswordAuthenticationToken(user, null, List.of()); }
}
