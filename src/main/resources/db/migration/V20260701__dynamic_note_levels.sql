ALTER TABLE evaluation_note_levels
    DROP CONSTRAINT IF EXISTS chk_note_level_value;

ALTER TABLE critere_note_rules
    DROP CONSTRAINT IF EXISTS chk_critere_note_rule_note;

CREATE UNIQUE INDEX IF NOT EXISTS uk_note_level_order
    ON evaluation_note_levels (display_order);
