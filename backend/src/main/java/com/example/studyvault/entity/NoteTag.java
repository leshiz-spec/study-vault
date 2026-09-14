package com.example.studyvault.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "note_tags",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_note_tags_note_tag",
            columnNames = {"note_id", "tag_id"}))
public class NoteTag {
  @EmbeddedId private NoteTagId id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("noteId")
  @JoinColumn(name = "note_id")
  private Note note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("tagId")
  @JoinColumn(name = "tag_id")
  private Tag tag;

  public NoteTag() {}

  public NoteTag(Note note, Tag tag) {
    this.note = note;
    this.tag = tag;
    this.id = new NoteTagId(note.getId(), tag.getId());
  }

  public NoteTagId getId() {
    return id;
  }

  public Note getNote() {
    return note;
  }

  public Tag getTag() {
    return tag;
  }
}
