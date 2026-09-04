package com.example.studyvault.service;

import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteTagRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.studyvault.dto.NoteSearchResponse;
import com.example.studyvault.repository.NoteSpecifications;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {
    private final NoteRepository notes; private final NoteTagRepository noteTags;

    @Autowired
    public NoteService(NoteRepository notes, NoteTagRepository noteTags) { this.notes = notes; this.noteTags = noteTags; }
    public NoteService(NoteRepository notes) { this(notes, null); }

    @Transactional
    public NoteResponse create(User user, NoteCreateRequest request) {
        Note note = new Note();
        note.setUser(user);
        note.setTitle(request.title());
        note.setContent(request.content());
        return response(notes.save(note));
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(User user) {
        return notes.findAllByUserAndStatusOrderByUpdatedAtDesc(user, "active").stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> trash(User user) {
        return notes.findAllByUserAndStatusOrderByUpdatedAtDesc(user, "trash").stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public NoteSearchResponse search(User user, String query, String tag, Boolean favorite, String status, Pageable pageable) {
        Page<Note> result = notes.findAll(NoteSpecifications.search(user, query, tag, favorite, status), pageable);
        return new NoteSearchResponse(result.getContent().stream().map(this::response).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public NoteResponse get(User user, Long id) { return response(findOwned(user, id)); }

    @Transactional
    public NoteResponse update(User user, Long id, NoteUpdateRequest request) {
        Note note = findOwned(user, id);
        note.setTitle(request.title());
        note.setContent(request.content());
        return response(notes.save(note));
    }

    @Transactional
    public void delete(User user, Long id) {
        Note note = findOwned(user, id);
        note.setStatus("trash");
        notes.save(note);
    }

    /** Marks an owned note as a favorite. */
    @Transactional
    public NoteResponse favorite(User user, Long id) {
        Note note = findOwned(user, id);
        note.setFavorite(!note.isFavorite());
        return response(notes.save(note));
    }

    /** Restores an owned note from the trash to the active state. */
    @Transactional
    public NoteResponse restore(User user, Long id) {
        Note note = findOwned(user, id);
        note.setStatus("active");
        return response(notes.save(note));
    }

    private Note findOwned(User user, Long id) {
        return notes.findByIdAndUser(id, user).orElseThrow(() -> new NoteNotFoundException(id));
    }

    private NoteResponse response(Note note) {
        if (noteTags == null) return NoteResponse.from(note);
        var assigned = noteTags.findAllByNote(note).stream().map(com.example.studyvault.entity.NoteTag::getTag).map(com.example.studyvault.dto.TagResponse::from).toList();
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getSummary(), note.getStatus(), note.isFavorite(), note.getReviewStatus(), note.getCreatedAt(), note.getUpdatedAt(), assigned);
    }
}
