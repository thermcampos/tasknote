ALTER TABLE tasknote.notes
  ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_notes_archived ON tasknote.notes (archived);
