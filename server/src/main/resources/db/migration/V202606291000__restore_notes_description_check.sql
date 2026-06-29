ALTER TABLE tasknote.notes
    ADD CONSTRAINT chk_notes_description_max_length CHECK (length(description) <= 50000) NOT VALID;
