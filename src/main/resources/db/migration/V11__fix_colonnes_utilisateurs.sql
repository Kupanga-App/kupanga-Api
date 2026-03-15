-- =====================================================
-- Fix nommage colonnes table utilisateurs
-- Passage en snake_case pour alignement Hibernate
-- =====================================================

ALTER TABLE utilisateurs RENAME COLUMN "motDePasse"        TO mot_de_passe;
ALTER TABLE utilisateurs RENAME COLUMN "A_completer_profil" TO a_completer_profil;