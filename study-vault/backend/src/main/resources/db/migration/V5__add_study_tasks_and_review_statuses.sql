ALTER TABLE notes DROP CONSTRAINT ck_notes_review_status;

UPDATE notes SET review_status = 'learning' WHERE review_status = 'in_progress';
UPDATE notes SET review_status = 'review' WHERE review_status = 'reviewed';

ALTER TABLE notes
    ADD CONSTRAINT ck_notes_review_status
    CHECK (review_status IN ('not_started', 'learning', 'review', 'mastered'));

CREATE TABLE study_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT,
    title VARCHAR(255) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'todo',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_tasks_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE SET NULL,
    CONSTRAINT ck_study_tasks_status CHECK (status IN ('todo', 'in_progress', 'done'))
);

CREATE INDEX idx_study_tasks_user_status ON study_tasks (user_id, status);
CREATE INDEX idx_study_tasks_user_due_date ON study_tasks (user_id, due_date);
