package com.example.studyvault.service;

import com.example.studyvault.dto.NoteCreateRequest;
import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.NoteUpdateRequest;
import com.example.studyvault.dto.ReviewStatusRequest;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteRevision;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteRevisionRepository;
import com.example.studyvault.repository.NoteTagRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.studyvault.dto.NoteSearchResponse;
import com.example.studyvault.repository.NoteSpecifications;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {
    private static final Set<String> REVIEW_STATUSES = Set.of("not_started", "learning", "review", "mastered");
    private final NoteRepository notes; private final NoteTagRepository noteTags; private final NoteRevisionRepository revisions;

    @Autowired
    public NoteService(NoteRepository notes, NoteTagRepository noteTags, NoteRevisionRepository revisions) { this.notes = notes; this.noteTags = noteTags; this.revisions = revisions; }
    public NoteService(NoteRepository notes, NoteTagRepository noteTags) { this(notes, noteTags, null); }
    public NoteService(NoteRepository notes) { this(notes, null, null); }

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
        boolean changed = !Objects.equals(note.getTitle(), request.title())
                || !Objects.equals(note.getContent(), request.content());
        if (changed && revisions != null) {
            NoteRevision revision = new NoteRevision();
            revision.setNote(note);
            revision.setTitle(note.getTitle());
            revision.setContent(note.getContent());
            revisions.save(revision);
        }
        note.setTitle(request.title());
        note.setContent(request.content());
        return response(notes.save(note));
    }

    @Transactional
    public NoteResponse updateReviewStatus(User user, Long id, ReviewStatusRequest request) {
        if (!REVIEW_STATUSES.contains(request.reviewStatus())) {
            throw new com.example.studyvault.exception.InvalidReviewStatusException(request.reviewStatus());
        }
        Note note = findOwned(user, id);
        note.setReviewStatus(request.reviewStatus());
        return response(notes.save(note));
    }

    @Transactional
    public void delete(User user, Long id) {
        Note note = findOwned(user, id);
        note.setStatus("trash");
        notes.save(note);
    }

    /** Permanently removes a note that is already in the owner's trash. */
    @Transactional
    public void permanentlyDelete(User user, Long id) {
        Note note = findOwned(user, id);
        // Keep this endpoint safe if it is called outside the Trash view: active notes are
        // intentionally indistinguishable from missing notes instead of being hard deleted.
        if (!"trash".equals(note.getStatus())) throw new NoteNotFoundException(id);
        notes.delete(note);
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
