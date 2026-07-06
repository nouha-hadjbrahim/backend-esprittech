ALTER TABLE evaluations
    ADD COLUMN IF NOT EXISTS trigger VARCHAR(80),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS engine_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS model_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pipeline_version VARCHAR(120),
    ADD COLUMN IF NOT EXISTS criteria_version VARCHAR(80),
    ADD COLUMN IF NOT EXISTS project_snapshot_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS deliverable_snapshot_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS input_snapshot_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(80),
    ADD COLUMN IF NOT EXISTS eligibility_status VARCHAR(80);

ALTER TABLE resultats_criteres
    ADD COLUMN IF NOT EXISTS criterion_status VARCHAR(80),
    ADD COLUMN IF NOT EXISTS evidence_quality DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS extraction_quality DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS source_coverage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS eliminatory_state VARCHAR(80),
    ADD COLUMN IF NOT EXISTS analysis_methods TEXT,
    ADD COLUMN IF NOT EXISTS evidence_json TEXT;

CREATE INDEX IF NOT EXISTS idx_evaluations_input_snapshot_hash
    ON evaluations (input_snapshot_hash);

UPDATE evaluations
SET pipeline_version = COALESCE(pipeline_version, ml_model_version),
    engine_type = COALESCE(engine_type, 'LEGACY_CONTENT_ANALYSIS'),
    processing_status = COALESCE(processing_status, ml_status),
    eligibility_status = COALESCE(eligibility_status,
        CASE
            WHEN COALESCE(bloque_par_eliminatoire, FALSE) THEN 'NON_ELIGIBLE_EN_L_ETAT'
            WHEN COALESCE(eligible_industrialisation, FALSE) THEN 'ELIGIBLE'
            ELSE 'NOT_EVALUABLE'
        END);
