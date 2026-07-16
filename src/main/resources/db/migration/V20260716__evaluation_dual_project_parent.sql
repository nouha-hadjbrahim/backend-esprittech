-- Evaluations can target either a SujetProjet or a ProjetCatalogue, never both.
-- This migration is MySQL/MariaDB oriented because the runtime datasource uses MySQL.

SET @schema_name := DATABASE();

SET @has_projet_catalogue_id := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'evaluations'
      AND COLUMN_NAME = 'projet_catalogue_id'
);

SET @sql := IF(
    @has_projet_catalogue_id = 0,
    'ALTER TABLE evaluations ADD COLUMN projet_catalogue_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE evaluations
    MODIFY COLUMN sujet_projet_id BIGINT NULL;

ALTER TABLE evaluations
    MODIFY COLUMN projet_catalogue_id BIGINT NULL;

DROP PROCEDURE IF EXISTS validate_evaluation_parent_integrity;

DELIMITER //
CREATE PROCEDURE validate_evaluation_parent_integrity()
BEGIN
    DECLARE invalid_sujet_fk_count INT DEFAULT 0;
    DECLARE invalid_catalogue_fk_count INT DEFAULT 0;
    DECLARE invalid_parent_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO invalid_sujet_fk_count
    FROM evaluations e
    LEFT JOIN sujet_projets s ON s.id = e.sujet_projet_id
    WHERE e.sujet_projet_id IS NOT NULL
      AND s.id IS NULL;

    IF invalid_sujet_fk_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refused: evaluations contain sujet_projet_id values without matching sujet_projets rows.';
    END IF;

    SELECT COUNT(*)
    INTO invalid_catalogue_fk_count
    FROM evaluations e
    LEFT JOIN projet_catalogue p ON p.id = e.projet_catalogue_id
    WHERE e.projet_catalogue_id IS NOT NULL
      AND p.id IS NULL;

    IF invalid_catalogue_fk_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refused: evaluations contain projet_catalogue_id values without matching projet_catalogue rows.';
    END IF;

    SELECT COUNT(*)
    INTO invalid_parent_count
    FROM evaluations
    WHERE (sujet_projet_id IS NULL AND projet_catalogue_id IS NULL)
       OR (sujet_projet_id IS NOT NULL AND projet_catalogue_id IS NOT NULL);

    IF invalid_parent_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refused: evaluations must have exactly one parent (SujetProjet or ProjetCatalogue).';
    END IF;
END//
DELIMITER ;

CALL validate_evaluation_parent_integrity();
DROP PROCEDURE validate_evaluation_parent_integrity;

SET @has_idx_sujet := (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'evaluations'
      AND COLUMN_NAME = 'sujet_projet_id'
);

SET @sql := IF(
    @has_idx_sujet = 0,
    'CREATE INDEX idx_evaluations_sujet_projet ON evaluations (sujet_projet_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_catalogue := (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'evaluations'
      AND COLUMN_NAME = 'projet_catalogue_id'
);

SET @sql := IF(
    @has_idx_catalogue = 0,
    'CREATE INDEX idx_evaluations_projet_catalogue ON evaluations (projet_catalogue_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_sujet := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'evaluations'
      AND COLUMN_NAME = 'sujet_projet_id'
      AND REFERENCED_TABLE_SCHEMA = @schema_name
      AND REFERENCED_TABLE_NAME = 'sujet_projets'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @sql := IF(
    @has_fk_sujet = 0,
    'ALTER TABLE evaluations ADD CONSTRAINT fk_evaluations_sujet_projet FOREIGN KEY (sujet_projet_id) REFERENCES sujet_projets (id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_catalogue := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'evaluations'
      AND COLUMN_NAME = 'projet_catalogue_id'
      AND REFERENCED_TABLE_SCHEMA = @schema_name
      AND REFERENCED_TABLE_NAME = 'projet_catalogue'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @sql := IF(
    @has_fk_catalogue = 0,
    'ALTER TABLE evaluations ADD CONSTRAINT fk_evaluations_projet_catalogue FOREIGN KEY (projet_catalogue_id) REFERENCES projet_catalogue (id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @version_core := SUBSTRING_INDEX(@@version, '-', 1);
SET @version_major := CAST(SUBSTRING_INDEX(@version_core, '.', 1) AS UNSIGNED);
SET @version_minor := CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(@version_core, '.', 2), '.', -1) AS UNSIGNED);
SET @version_patch := CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(@version_core, '.', 3), '.', -1) AS UNSIGNED);
SET @is_mariadb := LOCATE('MariaDB', @@version) > 0 OR LOCATE('MariaDB', @@version_comment) > 0;
SET @supports_enforced_check := IF(
    @is_mariadb
        OR @version_major > 8
        OR (@version_major = 8 AND (@version_minor > 0 OR @version_patch >= 16)),
    1,
    0
);

SET @sql := IF(
    @supports_enforced_check = 1,
    'ALTER TABLE evaluations ADD CONSTRAINT ck_evaluations_exactly_one_project_parent CHECK ((sujet_projet_id IS NOT NULL AND projet_catalogue_id IS NULL) OR (sujet_projet_id IS NULL AND projet_catalogue_id IS NOT NULL))',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
