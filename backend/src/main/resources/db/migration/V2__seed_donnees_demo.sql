-- =====================================================================
-- V2 — Données de démonstration
-- Le sujet exclut l'authentification (Q1) : l'étudiant est choisi dans une
-- liste. Ces données alimentent GET /api/promotions/{promotionId}/etudiants.
-- Les sessions ne sont pas pré-remplies : elles sont créées par le formateur
-- (leur code n'a qu'une durée de vie de 15 minutes, RG1).
-- =====================================================================

INSERT INTO promotion (nom, annee) VALUES
    ('KFOKAM48 — Promotion 2026', 2026);

INSERT INTO etudiant (prenom, nom, email, promotion_id) VALUES
    ('Alice',  'Martin',  'alice.martin@kfokam48.local',  1),
    ('Bilal',  'Ndiaye',  'bilal.ndiaye@kfokam48.local',  1),
    ('Chloé',  'Bernard', 'chloe.bernard@kfokam48.local', 1),
    ('David',  'Koffi',   'david.koffi@kfokam48.local',   1),
    ('Emma',   'Diallo',  'emma.diallo@kfokam48.local',   1);
