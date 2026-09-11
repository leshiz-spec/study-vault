package com.example.studyvault.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteRevisionResponse;
import com.example.studyvault.dto.NoteSummaryResponse;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.GlobalExceptionHandler;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.service.NoteService;
import com.example.studyvault.service.NoteTagService;
import com.example.studyvault.service.NoteSummaryService;
import com.example.studyvault.service.NoteRevisionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NoteControllerTest {
    private MockMvc mvc;
    private NoteService service;
    private NoteTagService noteTags;
    private NoteSummaryService summaries;
    private NoteRevisionService revisions;
    private User alice;

    @BeforeEach
    void setUp() {
        service = mock(NoteService.class); noteTags = mock(NoteTagService.class); summaries = mock(NoteSummaryService.class); revisions = mock(NoteRevisionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new NoteController(service, noteTags, null, summaries, revisions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        alice = new User();
        alice.setUsername("alice");
    }

    @Test
    void createNote() throws Exception {
        when(service.create(eq(alice), any(NoteCreateRequest.class))).thenReturn(response(1L, "Title", "Body", "active"));
        mvc.perform(post("/api/notes").principal(auth(alice)).contentType("application/json")
                        .content("{\"title\":\"Title\",\"content\":\"Body\",\"user_id\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Title"))
                .andExpect(content().string(not(containsString("user_id"))));
        verify(service).create(eq(alice), argThat(request -> request.title().equals("Title") && request.content().equals("Body")));
    }

    @Test
    void listOwnNotes() throws Exception {
        when(service.list(alice)).thenReturn(List.of(response(1L, "Mine", "Body", "active")));
        mvc.perform(get("/api/notes").principal(auth(alice))).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].title").value("Mine"));
        verify(service).list(alice);
    }

    @Test
    void listOwnTrash() throws Exception {
        when(service.trash(alice)).thenReturn(List.of(response(2L, "Deleted", "Body", "trash")));
        mvc.perform(get("/api/notes/trash").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].status").value("trash"));
        verify(service).trash(alice);
    }

    @Test
    void updateOwnNote() throws Exception {
        when(service.update(eq(alice), eq(1L), any(NoteUpdateRequest.class))).thenReturn(response(1L, "New", "Body", "active"));
        mvc.perform(put("/api/notes/1").principal(auth(alice)).contentType("application/json").content("{\"title\":\"New\",\"content\":\"Body\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("New"));
    }

    @Test
    void updateReviewStatus() throws Exception {
        when(service.updateReviewStatus(eq(alice), eq(1L), any(com.example.studyvault.dto.ReviewStatusRequest.class)))
                .thenReturn(new NoteResponse(1L, "Title", "Body", null, "active", false, "mastered", null, null));
        mvc.perform(put("/api/notes/1/review-status").principal(auth(alice)).contentType("application/json")
                        .content("{\"reviewStatus\":\"mastered\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.reviewStatus").value("mastered"));
    }

    @Test
    void listNoteRevisions() throws Exception {
        when(revisions.list(alice, 1L)).thenReturn(List.of(new NoteRevisionResponse(2L, 1L, "Old", "Old body", null)));
        mvc.perform(get("/api/notes/1/revisions").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Old"));
        verify(revisions).list(alice, 1L);
    }

    @Test
    void restoreNoteRevision() throws Exception {
        when(revisions.restore(alice, 1L, 2L)).thenReturn(response(1L, "Old", "Old body", "active"));
        mvc.perform(post("/api/notes/1/revisions/2/restore").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Old"));
        verify(revisions).restore(alice, 1L, 2L);
    }

    @Test
    void deleteOwnNote() throws Exception {
        mvc.perform(delete("/api/notes/1").principal(auth(alice))).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).delete(alice, 1L);
    }

    @Test
    void permanentlyDeleteTrashedNote() throws Exception {
        mvc.perform(delete("/api/notes/1/permanent").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(service).permanentlyDelete(alice, 1L);
    }

    @Test
    void favoriteOwnNote() throws Exception {
        when(service.favorite(alice, 1L)).thenReturn(response(1L, "Title", "Body", "active", true));
        mvc.perform(post("/api/notes/1/favorite").principal(auth(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.favorite").value(true));
        verify(service).favorite(alice, 1L);
    }

    @Test
    void restoreOwnNote() throws Exception {
        when(service.restore(alice, 1L)).thenReturn(response(1L, "Title", "Body", "active", false));
        mvc.perform(post("/api/notes/1/restore").principal(auth(alice)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("active"));
        verify(service).restore(alice, 1L);
    }

    @Test
    void summarizeReturnsDraftWithoutChangingNote() throws Exception {
        when(summaries.summarize(alice, 1L)).thenReturn(new NoteSummaryResponse(1L, "A concise draft"));
        mvc.perform(post("/api/notes/1/summarize").principal(auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noteId").value(1))
                .andExpect(jsonPath("$.data.summary").value("A concise draft"));
        verify(summaries).summarize(alice, 1L);
    }

    @Test
    void summarizeOtherUsersNoteUsesNotFound() throws Exception {
        when(summaries.summarize(alice, 2L)).thenThrow(new NoteNotFoundException(2L));
        mvc.perform(post("/api/notes/2/summarize").principal(auth(alice)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void saveSummaryRequiresExplicitRequestAndReturnsUpdatedNote() throws Exception {
        when(summaries.save(alice, 1L, "Draft summary")).thenReturn(
                responseWithSummary(1L, "Title", "Body", "Draft summary"));
        mvc.perform(put("/api/notes/1/summary").principal(auth(alice)).contentType("application/json")
                        .content("{\"summary\":\"Draft summary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("Draft summary"));
        verify(summaries).save(alice, 1L, "Draft summary");
    }

    @Test
    void nonexistentNoteReturnsNoteNotFound() throws Exception {
        when(service.get(alice, 404L)).thenThrow(new NoteNotFoundException(404L));
        mvc.perform(get("/api/notes/404").principal(auth(alice))).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void anotherUsersNoteIsAlsoNotFound() throws Exception {
        when(service.get(alice, 2L)).thenThrow(new NoteNotFoundException(2L));
        mvc.perform(get("/api/notes/2").principal(auth(alice))).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOTE_NOT_FOUND"));
    }

    @Test
    void emptyNoteTitleReturnsValidationError() throws Exception {
        mvc.perform(post("/api/notes").principal(auth(alice)).contentType("application/json")
                        .content("{\"title\":\"  \",\"content\":\"Body\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.title").exists());
        verifyNoInteractions(service);
    }

    private static UsernamePasswordAuthenticationToken auth(User user) { return new UsernamePasswordAuthenticationToken(user, null, List.of()); }
    private static NoteResponse response(Long id, String title, String content, String status) { return new NoteResponse(id, title, content, null, status, false, "not_started", null, null); }
    private static NoteResponse response(Long id, String title, String content, String status, boolean favorite) { return new NoteResponse(id, title, content, null, status, favorite, "not_started", null, null); }
    private static NoteResponse responseWithSummary(Long id, String title, String content, String summary) { return new NoteResponse(id, title, content, summary, "active", false, "not_started", null, null); }
}
