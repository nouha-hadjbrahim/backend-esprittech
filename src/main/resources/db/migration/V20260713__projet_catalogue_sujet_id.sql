ALTER TABLE projet_catalogue
    ADD COLUMN sujet_id BIGINT NULL;

ALTER TABLE projet_catalogue
    ADD CONSTRAINT uk_projet_catalogue_sujet UNIQUE (sujet_id);

ALTER TABLE projet_catalogue
    ADD CONSTRAINT fk_projet_catalogue_sujet
        FOREIGN KEY (sujet_id) REFERENCES sujet_projets (id);
