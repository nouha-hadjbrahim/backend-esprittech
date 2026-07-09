CREATE TABLE historique (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT,
    summary TEXT NOT NULL,
    old_values JSON,
    new_values JSON,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_historique_entity (entity_type, entity_id),
    INDEX idx_historique_actor (actor_id),
    INDEX idx_historique_actor_date (actor_id, created_at DESC),
    INDEX idx_historique_action (action),
    INDEX idx_historique_created (created_at),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);
