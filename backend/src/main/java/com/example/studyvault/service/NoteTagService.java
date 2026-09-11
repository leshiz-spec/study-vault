package com.example.studyvault.service;

import com.example.studyvault.dto.NoteResponse;
import com.example.studyvault.dto.TagResponse;
import com.example.studyvault.entity.Note;
import com.example.studyvault.entity.NoteTag;
import com.example.studyvault.entity.Tag;
import com.example.studyvault.entity.User;
import com.example.studyvault.exception.NoteNotFoundException;
import com.example.studyvault.exception.NoteTagAlreadyExistsException;
import com.example.studyvault.repository.NoteRepository;
import com.example.studyvault.repository.NoteTagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteTagService {
    private final NoteRepository notes; private final TagService tags; private final NoteTagRepository noteTags;
    public NoteTagService(NoteRepository notes, TagService tags, NoteTagRepository noteTags) { this.notes = notes; this.tags = tags; this.noteTags = noteTags; }

    @Transactional
    public NoteResponse add(User user, Long noteId, Long tagId) {
        Note note = notes.findByIdAndUser(noteId, user).orElseThrow(() -> new NoteNotFoundException(noteId));
        Tag tag = tags.findOwned(user, tagId);
        if (noteTags.existsByNoteAndTag(note, tag)) throw new NoteTagAlreadyExistsException();
        noteTags.save(new NoteTag(note, tag));
        return response(note);
    }

    @Transactional
    public void remove(User user, Long noteId, Long tagId) {
        Note note = notes.findByIdAndUser(noteId, user).orElseThrow(() -> new NoteNotFoundException(noteId));
        Tag tag = tags.findOwned(user, tagId);
        noteTags.deleteById(new com.example.studyvault.entity.NoteTagId(note.getId(), tag.getId()));
    }

    public NoteResponse response(Note note) {
        List<TagResponse> assigned = noteTags.findAllByNote(note).stream().map(NoteTag::getTag).map(TagResponse::from).toList();
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getSummary(), note.getStatus(), note.isFavorite(), note.getReviewStatus(), note.getCreatedAt(), note.getUpdatedAt(), assigned);
    }
}
