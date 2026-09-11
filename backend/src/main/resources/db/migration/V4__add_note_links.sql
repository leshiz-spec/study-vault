CREATE TABLE note_links (
    id BIGSERIAL PRIMARY KEY,
    source_note_id BIGINT NOT NULL,
    target_note_id BIGINT NOT NULL,
    CONSTRAINT uk_note_links_source_target UNIQUE (source_note_id, target_note_id),
    CONSTRAINT ck_note_links_different_notes CHECK (source_note_id <> target_note_id),
    CONSTRAINT fk_note_links_source FOREIGN KEY (source_note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_links_target FOREIGN KEY (target_note_id) REFERENCES notes (id) ON DELETE CASCADE
);

CREATE INDEX idx_note_links_source ON note_links (source_note_id);
CREATE INDEX idx_note_links_target ON note_links (target_note_id);
