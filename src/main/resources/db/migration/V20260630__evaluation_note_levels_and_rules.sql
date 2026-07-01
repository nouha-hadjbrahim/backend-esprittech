CREATE TABLE IF NOT EXISTS evaluation_note_levels (
    id BIGSERIAL PRIMARY KEY,
    level_value INTEGER NOT NULL,
    label VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_miseajour TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_note_level_value UNIQUE (level_value),
    CONSTRAINT chk_note_level_value CHECK (level_value BETWEEN 1 AND 5)
);

INSERT INTO evaluation_note_levels (level_value, label, description, active, display_order, date_creation, date_miseajour)
SELECT 1, 'Très insatisfait', 'Niveau 1 sur 5', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM evaluation_note_levels WHERE level_value = 1);

INSERT INTO evaluation_note_levels (level_value, label, description, active, display_order, date_creation, date_miseajour)
SELECT 2, 'Insatisfait', 'Niveau 2 sur 5', TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM evaluation_note_levels WHERE level_value = 2);

INSERT INTO evaluation_note_levels (level_value, label, description, active, display_order, date_creation, date_miseajour)
SELECT 3, 'Peu satisfait', 'Niveau 3 sur 5', TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM evaluation_note_levels WHERE level_value = 3);

INSERT INTO evaluation_note_levels (level_value, label, description, active, display_order, date_creation, date_miseajour)
SELECT 4, 'Satisfait', 'Niveau 4 sur 5', TRUE, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM evaluation_note_levels WHERE level_value = 4);

INSERT INTO evaluation_note_levels (level_value, label, description, active, display_order, date_creation, date_miseajour)
SELECT 5, 'Très satisfait', 'Niveau 5 sur 5', TRUE, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM evaluation_note_levels WHERE level_value = 5);

ALTER TABLE criteres_notes
    ADD COLUMN IF NOT EXISTS default_note_value INTEGER DEFAULT 3;

UPDATE criteres_notes
SET default_note_value = 3
WHERE default_note_value IS NULL;

UPDATE criteres_notes
SET bareme = 5
WHERE bareme IS NULL OR bareme <> 5;

UPDATE criteres_notes
SET seuil = 3
WHERE seuil IS NULL;

CREATE TABLE IF NOT EXISTS critere_note_rules (
    id BIGSERIAL PRIMARY KEY,
    critere_note_id BIGINT NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    metadata_key VARCHAR(255) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    expected_value VARCHAR(1000),
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    note_value INTEGER NOT NULL,
    priority_order INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_miseajour TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_critere_note_rules_critere
        FOREIGN KEY (critere_note_id) REFERENCES criteres_notes (id),
    CONSTRAINT chk_critere_note_rule_note CHECK (note_value BETWEEN 1 AND 5)
);

ALTER TABLE resultats_criteres
    ADD COLUMN IF NOT EXISTS note_value INTEGER,
    ADD COLUMN IF NOT EXISTS note_label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS matched_rule VARCHAR(255),
    ADD COLUMN IF NOT EXISTS metadata_used VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS explanation VARCHAR(2000);

UPDATE resultats_criteres
SET note_value = GREATEST(1, LEAST(5, ROUND(COALESCE(note_obtenue, 3))::INTEGER))
WHERE type_critere = 'NOTE' AND note_value IS NULL;

UPDATE resultats_criteres rc
SET note_label = nl.label
FROM evaluation_note_levels nl
WHERE rc.note_value = nl.level_value
  AND rc.note_label IS NULL;

ALTER TABLE evaluations
    ADD COLUMN IF NOT EXISTS has_eliminatory_warnings BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS eliminatory_warnings_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS eliminatory_warnings_details VARCHAR(4000);

UPDATE evaluations
SET has_eliminatory_warnings = COALESCE(bloque_par_eliminatoire, FALSE),
    eliminatory_warnings_count = CASE WHEN COALESCE(bloque_par_eliminatoire, FALSE) THEN 1 ELSE 0 END
WHERE has_eliminatory_warnings IS NULL;

ALTER TABLE sujet_projets
    ADD COLUMN IF NOT EXISTS has_eliminatory_warnings BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS eliminatory_warnings_count INTEGER DEFAULT 0;

UPDATE sujet_projets
SET has_eliminatory_warnings = COALESCE(bloque_par_eliminatoire, FALSE),
    eliminatory_warnings_count = CASE WHEN COALESCE(bloque_par_eliminatoire, FALSE) THEN 1 ELSE 0 END
WHERE has_eliminatory_warnings IS NULL;

ALTER TABLE candidatures_industrialisation
    ADD COLUMN IF NOT EXISTS has_eliminatory_warnings BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS eliminatory_warnings_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS eliminatory_warnings_details VARCHAR(4000);
