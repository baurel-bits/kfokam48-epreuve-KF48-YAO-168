# Backlog — détails complémentaires

Substance reprise des issues d'origine (sauvegardées dans `backup/issues-avant-suppression.json`),
**réalignée** sur le cahier des charges et sur `api/contrat.yaml` :

- numérotation **EF/RG** du cahier des charges (les anciennes issues en utilisaient une autre) ;
- endpoints du contrat (`POST /api/presences/manuelles`, `GET /api/tableau?promotionId=`, …) ;
- cible de performance du cahier des charges : **< 1 s** (ENF3) — la cible interne de 200 ms des
  anciennes issues est conservée comme objectif indicatif, pas comme exigence.

---

## EF2

### Tests backend
- Code valide et non expiré → `201`, présence enregistrée avec `source = ETUDIANT`.
- Code inconnu → `400 CODE_INCONNU`.
- Code de plus de 15 minutes → `410 CODE_EXPIRE`.
- Présence déjà existante pour le couple (session, étudiant) → `409 DEJA_PRESENT`.
- 5 échecs de saisie sur le couple (étudiant, session) → `429 TROP_DE_TENTATIVES`, blocage 2 minutes.
- Le compteur d'échecs est cloisonné par session : être bloqué sur une session n'empêche pas une autre.

### Tests frontend
- Saisie du code, état de chargement, bouton désactivé pendant l'appel.
- Affichage explicite des erreurs `400` / `409` / `410` / `429`.
- Message de confirmation après succès.
- Aucun double appel lors de clics rapides.

### Contraintes techniques
- `source` ne peut pas être choisie par le frontend : elle est déterminée par l'opération appelée.
- Règles métier centralisées dans le service, aucune règle métier côté frontend.
- Format d'erreur homogène `{ code, message }`.

### Livrables
- Service de présence + DTO de requête/réponse.
- Gestion centralisée des erreurs métier.
- Tests unitaires (expiration du code, compteur RG3) et test d'intégration de l'endpoint.

---

## EF3

### Tests backend
- Dépôt valide → `201 { id, statut: DEPOSE }`.
- Lien mal formé → `400 LIEN_INVALIDE`.
- Deuxième dépôt sur la même session → `409 EXERCICE_DEJA_DEPOSE`.
- Dépôt encore possible après expiration du code, tant que la session n'est pas clôturée (RG10).
- Dépôt refusé après clôture de la session (RG14).
- Le dépôt déclenche l'assignation d'un relecteur (EF5).

### Tests frontend
- État de chargement, gestion `400` / `409`, confirmation du dépôt.
- Distinction visuelle entre « aucun dépôt » et « exercice déposé, en attente de relecture ».

### Contraintes techniques
- Le lien est validé comme URI côté backend (validation d'entrée), pas seulement côté frontend.
- Unicité `(session_id, etudiant_id)` garantie en base.

### Livrables
- Endpoint + service + DTO ; test d'intégration ; documentation OpenAPI à jour.

---

## EF5

### Tests backend
- Le relecteur choisi est bien **présent** à la session.
- L'auteur n'est **jamais** désigné comme relecteur de son propre exercice (RG4).
- Le pool est **recalculé au moment de l'assignation** (RG13), présences manuelles antérieures incluses.
- Aucun étudiant éligible → **aucun relecteur n'est attaché et l'exercice reste `DEPOSE`** : la transition de D4 est gardée par « Attribution d'un relecteur », et cette équivalence `DEPOSE` ⟺ aucune relecture est ce qui rendra RG11 (EF4) vérifiable sans requête supplémentaire. Il apparaît malgré tout comme « non rendu » dans le tableau (RG9). Décision révisée à l'implémentation de l'issue #11, voir D4, section EF5.
- Un exercice n'a **qu'un seul** relecteur (RG5).
- `GET /api/exercices/{exerciceId}/relecteur` → `200` pour le relecteur assigné, `403` pour tout autre appelant, `404` si aucune relecture n'est assignée (RG6).

### Contraintes techniques
- Le tirage est aléatoire côté backend ; le frontend ne choisit ni ne propose de relecteur.
- Contrainte SQL d'auto-relecture : `CHECK (relecteur_id <> auteur_id)` + FK composite vers l'exercice.

### Livrables
- Algorithme d'attribution (pool recalculé) ; endpoint de consultation réservé au relecteur ; tests unitaires.

---

## EF6

### Tests backend
- Note entière entre 0 et 20 + commentaire → `200`, la relecture passe à `RELU`.
- Bornes acceptées : `0` et `20`. Refusées : `21`, `-1`, valeurs décimales → `400 NOTE_INVALIDE`.
- Relecture de son propre exercice → `403 AUTO_RELECTURE` (RG4).
- Le relecteur retrouve sa mission via `GET /api/relecteurs/{etudiantId}/relectures-en-attente`.
- La réponse destinée au relecteur ne divulgue pas l'identité de l'auteur (RG6).

### Contraintes techniques
- RG7 (entier 0–20) garantie à la fois par la validation d'entrée et par `CHECK (note BETWEEN 0 AND 20)`.
- L'identité du relecteur n'est jamais renvoyée à l'étudiant relu (filtrage du DTO de sortie, RG6).

### Livrables
- Endpoint + service + DTO ; tests d'intégration `200` / `400` / `403` ; documentation OpenAPI.

---

## EF9

### Métriques attendues (par étudiant de la promotion)
- Nombre de présences.
- Nombre d'exercices déposés.
- Moyenne des notes reçues (`null` si aucune note).
- Nombre de relectures en attente.
- Les étudiants **sans aucune activité** apparaissent quand même dans le tableau.

### Contraintes
- Seules les évaluations rendues comptent dans la moyenne.
- La moyenne est **calculée par le backend** ; le frontend n'effectue **aucun** calcul métier.
- Un **remplacement de lien** ne doit pas augmenter le nombre de dépôts (pas de double comptage).
- Éviter le problème **N+1** : agrégations SQL/JPA, projections/DTO, index appropriés.
- Temps de réponse : **< 1 s** (ENF3) ; cible interne indicative < 200 ms.

### Tests backend
- Promotion valide → `200` ; promotion inconnue → `404 PROMOTION_INCONNUE`.
- Comptage correct des présences, des dépôts, des relectures rendues et en attente.
- Aucune note → moyenne `null` (jamais `0`).
- Aucun problème N+1 ; statistiques calculées côté backend.
- Test de performance documenté (dataset de test).

### Tests frontend
- Quatre états : chargement, données disponibles, aucune donnée, erreur.
- Aucune agrégation côté client ; moyennes affichées telles que renvoyées par l'API.

### Livrables
- Endpoint + requêtes d'agrégation + DTO de projection ; tests d'intégration et de performance.

---

## EF10

### Tests backend
- Ajout valide → `201` avec `source = FORMATEUR`.
- Doublon pour le couple (session, étudiant) → `409 DEJA_PRESENT`.
- Session inexistante → `404 SESSION_INCONNUE`. Étudiant inexistant → `404 ETUDIANT_INCONNU`.
- Fonctionne **sans code** et **même si le code de présence est expiré**.
- `source` ne peut pas être falsifiée : elle est déterminée par l'opération appelée.
- Gestion des requêtes concurrentes (pas de doublon créé en cas d'appels simultanés).

### Tests frontend
- Action « Marquer présent » affichée pour un étudiant absent.
- Confirmation avant l'action ; état de chargement ; bouton désactivé pendant le traitement.
- Mise à jour du statut après succès ; affichage de la source « Formateur ».
- Action masquée/désactivée pour un étudiant déjà présent ; gestion des `404` et `409`.
- Aucun double appel lors de clics rapides.

### Contraintes techniques
- Endpoint du contrat : **`POST /api/presences/manuelles`** (⚠️ et non `/api/presences/manuel`).
- Unicité `(session_id, etudiant_id)` garantie en base.
- Règles métier centralisées dans le service ; erreurs au format `{ code, message }`.

### ⚠️ Limite assumée
Le sujet exclut l'authentification (Q1). Il n'existe donc **ni « formateur autorisé » ni `403`**, et la
**traçabilité du formateur n'est pas modélisée** (pas de `formateurId` dans `docs/diagrammes/D2_class_diagram.md`).
Ces points des anciennes issues sont **abandonnés tant qu'ils ne sont pas tranchés**.

### Livrables
- Endpoint + service + DTO ; gestion des doublons ; tests unitaires et d'intégration.
