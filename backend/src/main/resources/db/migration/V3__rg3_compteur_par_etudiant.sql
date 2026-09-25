-- =====================================================================
-- V3 — RG3 : compteur d'échecs pour les codes non attribuables
--
-- Constat : le contrat n'envoie que `{ code, etudiantId }`. Un code inconnu ne
-- peut donc être rattaché à AUCUNE session, alors que `tentative_saisie`
-- exigeait une session (RG3, « compteur par couple (étudiant, session) »).
-- Conséquence : cinq codes erronés — l'échec de saisie le plus courant — ne
-- déclenchaient aucun blocage et RG3 restait inobservable.
--
-- Décision : le compteur accepte désormais une ligne SANS session
-- (`session_id IS NULL`), qui porte les échecs non attribuables à une session.
-- La portée par couple (étudiant, session) reste inchangée pour les codes
-- identifiables (expirés), conformément à la décision de la section 7.
-- =====================================================================

ALTER TABLE tentative_saisie ALTER COLUMN session_id DROP NOT NULL;
