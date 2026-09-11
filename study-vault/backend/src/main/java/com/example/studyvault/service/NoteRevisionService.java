package com.example.studyvault.service;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteRevisionResponse;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteRevisionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns revision access and restore rules; every query is scoped to the note owner. */
@Service
public class NoteRevisionService {
    private final NoteRepository notes;
    private final NoteRevisionRepository revisions;
    private final NoteService noteService;

    public NoteRevisionService(NoteRepository notes, NoteRevisionRepository revisions, NoteService noteService) {
        this.notes = notes;
        this.revisions = revisions;
        this.noteService = noteService;
    }

    @Transactional(readOnly = true)
    public List<NoteRevisionResponse> list(User user, Long noteId) {
        ownedNote(user, noteId);
        return revisions.findAllByNote_IdAndNote_User_IdOrderByCreatedAtDesc(noteId, user.getId())
                .stream().map(NoteRevisionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NoteRevisionResponse get(User user, Long noteId, Long revisionId) {
        ownedNote(user, noteId);
        return revisions.findByIdAndNote_IdAndNote_User_Id(revisionId, noteId, user.getId())
                .map(NoteRevisionResponse::from)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
    }

    @Transactional
    public NoteResponse restore(User user, Long noteId, Long revisionId) {
        ownedNote(user, noteId);
        NoteRevision revision = revisions.findByIdAndNote_IdAndNote_User_Id(revisionId, noteId, user.getId())
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        return noteService.update(user, noteId, new NoteUpdateRequest(revision.getTitle(), revision.getContent()));
    }

    private Note ownedNote(User user, Long noteId) {
        return notes.findByIdAndUser(noteId, user).orElseThrow(() -> new NoteNotFoundException(noteId));
    }
}
