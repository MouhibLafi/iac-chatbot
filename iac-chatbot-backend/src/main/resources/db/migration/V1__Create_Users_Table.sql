-- ==============================================================================
-- Migration V1: Création de la table users
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index pour améliorer les performances
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);

-- ==============================================================================
-- Données de test: Admin et User par défaut
-- Mot de passe pour les deux: password123
-- ==============================================================================

-- Admin (password: password123)
-- Hash BCrypt du mot de passe
INSERT INTO users (username, email, password, role, enabled, created_at, updated_at)
VALUES ('admin', 'admin@iacchatbot.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- User (password: password123)
INSERT INTO users (username, email, password, role, enabled, created_at, updated_at)
VALUES ('user', 'user@iacchatbot.com', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================================================
-- Notes:
-- - Le mot de passe par défaut est 'password123' pour les tests
-- - À changer en production!
-- - L'admin peut gérer les utilisateurs
-- - L'user peut uniquement utiliser le chatbot
-- ==============================================================================
