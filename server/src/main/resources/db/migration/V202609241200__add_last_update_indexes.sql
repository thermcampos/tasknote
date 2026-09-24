CREATE INDEX IF NOT EXISTS idx_tasks_last_update ON tasknote.tasks (last_update);

CREATE INDEX IF NOT EXISTS idx_notes_last_update ON tasknote.notes (last_update);
