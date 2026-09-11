ALTER TABLE note_revisions ADD COLUMN title VARCHAR(255);

UPDATE note_revisions revisions
SET title = notes.title
FROM notes
WHERE revisions.note_id = notes.id;

ALTER TABLE note_revisions ALTER COLUMN title SET NOT NULL;
