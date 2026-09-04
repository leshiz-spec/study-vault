package com.example.studyvault.service;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteSummaryResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates ownership checks, draft generation, and explicit summary persistence. */
@Service
public class NoteSummaryService {
    private final NoteRepository notes;
    private final AiSummaryProvider provider;
    private final NoteService noteService;

    public NoteSummaryService(NoteRepository notes, AiSummaryProvider provider, NoteService noteService) {
        this.notes = notes;
        this.provider = provider;
        this.noteService = noteService;
    }

    @Transactional(readOnly = true)
    public NoteSummaryResponse summarize(User user, Long id) {
        Note note = owned(user, id);
        // Ownership is verified before any note content reaches the provider.
        return new NoteSummaryResponse(id, provider.summarize(note.getContent()));
    }

    @Transactional
    public NoteResponse save(User user, Long id, String summary) {
        Note note = owned(user, id);
        note.setSummary(summary.trim());
        notes.save(note);
        return noteService.get(user, id);
    }

    private Note owned(User user, Long id) {
        return notes.findByIdAndUser(id, user).orElseThrow(() -> new NoteNotFoundException(id));
    }
}
