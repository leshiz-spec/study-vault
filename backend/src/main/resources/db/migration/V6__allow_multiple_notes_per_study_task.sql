CREATE TABLE study_task_notes (
    task_id BIGINT NOT NULL,
    note_id BIGINT NOT NULL,
    CONSTRAINT pk_study_task_notes PRIMARY KEY (task_id, note_id),
    CONSTRAINT fk_study_task_notes_task FOREIGN KEY (task_id) REFERENCES study_tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_task_notes_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
);

INSERT INTO study_task_notes (task_id, note_id)
SELECT id, note_id FROM study_tasks WHERE note_id IS NOT NULL;

CREATE INDEX idx_study_task_notes_note ON study_task_notes (note_id);
