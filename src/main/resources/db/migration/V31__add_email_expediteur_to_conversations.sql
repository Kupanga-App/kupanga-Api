-- V31__add_email_expediteur_to_conversations.sql

-- 1. Ajouter la colonne email_expediteur
ALTER TABLE conversations
ADD COLUMN email_expediteur VARCHAR(255);

-- 2.  Ajouter un index pour les recherches rapides
CREATE INDEX idx_conversations_email_expediteur
ON conversations(email_expediteur);