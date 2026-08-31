CREATE TABLE note_revisions (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_revisions_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
);

CREATE INDEX idx_note_revisions_note_created_at
    ON note_revisions (note_id, created_at DESC);
