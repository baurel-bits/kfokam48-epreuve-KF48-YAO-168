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
- Note entière entre 0 et 20 + commentaire → `200`, la relecture passe à `RENDUE` **et l'exercice à `RELU`** (D4) : deux énumérations distinctes, écrites dans la même transaction.
- Bornes acceptées : `0` et `20`. Refusées : `21`, `-1`, valeurs décimales → `400 NOTE_INVALIDE`.
- Relecture de son propre exercice → `403 AUTO_RELECTURE` (RG4). Cas **inatteignable par l'API** (l'assignation écarte l'auteur et `CHECK (relecteur_id <> auteur_id)` interdit la ligne) : garde-fou couvert par un test **unitaire** seulement.
- Une seconde note sur la même relecture → `409 RELECTURE_DEJA_RENDUE` : la correction d'une note rendue relève de l'EF7 (`PUT /api/relectures/{id}/correction`).
- Identifiant de relecture inconnu → `404 RELECTURE_INCONNUE` (cas non prévu par le contrat, ajouté pour ne pas renvoyer `500`).
- Le relecteur retrouve sa mission via `GET /api/relecteurs/{etudiantId}/relectures-en-attente`.
- La réponse destinée au relecteur ne divulgue pas l'identité de l'auteur (RG6).

### Contraintes techniques
- RG7 (entier 0–20) garantie à la fois par la validation d'entrée et par `CHECK (note BETWEEN 0 AND 20)`.
- L'identité du relecteur n'est jamais renvoyée à l'étudiant relu (filtrage du DTO de sortie, RG6).

### Livrables
- Endpoint + service + DTO ; tests d'intégration `200` / `400` / `409` / `404` (+ `403` en unitaire) ; documentation OpenAPI (contrat inchangé : l'opération imposée y figurait déjà).
- Frontend : écran **relecteur** (`/relecteur`) — la 3ᵉ vue exigée par F2 — qui liste les missions assignées et rend la note.

---

## EF8

### Tests backend
- Relecture `RENDUE` → l'étudiant relu obtient sa note entière et son commentaire (`200`).
- Relecture encore `EN_ATTENTE` → la ligne figure dans la liste avec `note` et `commentaire` **nuls** (RG9) : l'exercice apparaît « en attente » au lieu de sembler ignoré.
- **L'identité du relecteur n'apparaît dans aucune réponse** (RG6) : le DTO de sortie ne porte que `exerciceId`, `statut`, `note`, `commentaire`, et une assertion vérifie explicitement qu'aucune clé n'évoque le relecteur.
- Étudiant sans relecture → `200` avec une **liste vide** : le contrat ne déclare que `200` sur cette opération, l'absence de relecture n'est pas une erreur.
- Ordre de la liste : de la plus récente à la plus ancienne.

### Tests frontend
- Étape **« 4. Mes notes reçues »** de l'écran étudiant : bouton d'affichage, état de chargement, bouton désactivé pendant l'appel.
- Liste vide → « Aucune relecture reçue pour l'instant », pas de message d'erreur.
- Relecture en attente → `EN_ATTENTE — note à venir` ; relecture rendue → `15/20 — RENDUE` suivi du commentaire.
- Changer d'étudiant à l'étape 1 vide les notes affichées : celles de l'étudiant précédent ne restent jamais à l'écran.
- Aucune interprétation côté client (F3) : note et statut sont affichés tels que renvoyés par l'API.

### Contraintes techniques
- Lecture seule (`@Transactional(readOnly = true)`), une requête, pas de N+1.
- Le contrat de sortie est **figé à quatre champs** : ajouter un champ exposant le relecteur romprait RG6 — la protection est structurelle, pas seulement conventionnelle.
- `auteurId` est dénormalisé depuis l'exercice et maintenu par la clé étrangère composite : la lecture est exacte sans jointure.

### ⚠️ Limite assumée
Un exercice **sans relecture assignée** (aucun étudiant éligible au dépôt — décision EF5, équivalence `DEPOSE` ⟺ aucune relecture) n'apparaît pas dans cette liste : l'opération liste des **relectures**, pas des exercices. Il n'en reste pas moins visible comme « non rendu » dans le tableau du formateur (RG9).

### Livrables
- Endpoint + service + DTO de sortie ; tests unitaires et d'intégration (`200`, liste vide, absence du relecteur) ; documentation OpenAPI (contrat inchangé : l'opération y figurait déjà).
- Frontend : étape 4 « Mes notes reçues » de l'écran étudiant, branchée sur la couche d'appel API dédiée.

---

## EF9

### Métriques attendues (par étudiant de la promotion)
- Nombre de présences.
- Nombre d'exercices déposés.
- Moyenne des notes reçues (`null` si aucune note).
- Nombre de relectures en attente.
- Les étudiants **sans aucune activité** apparaissent quand même dans le tableau.

### Définition de `relecturesEnAttente` (décision)
Le contrat nomme le champ sans préciser de quel côté il se place. Il compte les relectures **des exercices de l'étudiant** qui ne sont pas encore rendues (`auteurId`, statut `EN_ATTENTE`) : c'est la lecture de RG9 (« un exercice sans relecture rendue doit apparaître comme tel dans le tableau »), cohérente avec la « moyenne des notes **reçues** » de la même ligne. Conséquence assumée : un exercice resté **sans relecteur** (aucun étudiant éligible au dépôt, décision EF5) n'est pas compté ici — l'opération liste des relectures, pas des exercices.

### Contraintes
- Seules les évaluations rendues portent une note : le statut est filtré dans la requête.
- La moyenne est **calculée par le backend**, arrondie à deux décimales **par le serveur** ; le frontend n'effectue **aucun** calcul métier.
- Un **remplacement de lien** ne doit pas augmenter le nombre de dépôts (pas de double comptage).
- Éviter le problème **N+1** : agrégations SQL/JPA, projections/DTO, index appropriés.
- Temps de réponse : **< 1 s** (ENF3) ; cible interne indicative < 200 ms.
- Le nombre de requêtes est **constant** : six requêtes SQL au total (existence de la promotion, identités des étudiants, puis quatre agrégations), quel que soit l'effectif de la promotion.

### Tests backend
- Promotion valide → `200` ; promotion inconnue → `404 PROMOTION_INCONNUE` ; `promotionId` absent → `400 DEMANDE_INVALIDE`.
- Comptage correct des présences, des dépôts, des relectures rendues et en attente.
- Aucune note → moyenne `null` (jamais `0`).
- Aucun problème N+1, **mesuré** et non simplement affirmé : les compteurs Hibernate (`generate_statistics` activé dans le profil de test) montrent le même nombre de requêtes pour une promotion d'un étudiant et pour une de douze.
- Une promotion sans étudiant renvoie une liste vide sans lancer d'agrégation.

### Tests frontend
- Quatre états : chargement, données disponibles, aucune donnée, erreur.
- Aucune agrégation côté client ; moyennes affichées telles que renvoyées par l'API, et **« — »** quand elles sont nulles (jamais « 0/20 », qui laisserait croire à une évaluation ratée).

### ⚠️ Limite assumée
L'ébauche d'issue décrivait un tableau **par session** (`?sessionId=`) et un contrôle `403 ACCES_REFUSE` empêchant un formateur de consulter la promotion d'un autre. Le premier point contredit l'opération **imposée** (qui prend `promotionId` et renvoie une ligne par étudiant) ; le second est inapplicable sans authentification (Q1), et le contrat ne déclare aucun `403` sur cette opération. Les deux sont abandonnés (cahier des charges, section 11).

### Livrables
- Endpoint imposé + 5 requêtes de projection/agrégation + DTO de sortie ; tests unitaires (dont l'absence de N+1) et d'intégration.
- Frontend : tableau de bord intégré à l'écran formateur (`/formateur`), qui reste l'un des trois écrans exigés par F2.

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

### Décisions d'implémentation (issue #18)
- **Ordre des contrôles** : session (`404 SESSION_INCONNUE`), étudiant (`404 ETUDIANT_INCONNU`), doublon (`409 DEJA_PRESENT`). La contrainte d'unicité `uk_presence_session_etudiant` est en plus rattrapée (`DataIntegrityViolationException`) : deux clics simultanés produisent le `409` du contrat, pas un `500`.
- **`404` ici, `400` sur l'opération imposée** : le contrat déclare `ETUDIANT_INCONNU` en `404` sur `/api/presences/manuelles`, mais en `400` sur `POST /api/presences`. Chaque opération suit sa propre déclaration (B2) : même règle métier, deux guichets, deux statuts.
- **`source` jamais reçue du client** : c'est l'opération appelée qui la fixe à `FORMATEUR` (RG12), jamais le corps de la requête.
- **Ni code, ni expiration, ni clôture** : l'ajout ne consulte pas le code de présence — il fonctionne sans code et après les 15 minutes de RG1. La clôture ne le bloque pas non plus : RG14 ne gèle que les **dépôts** et les **notes**, et le contrat le formule ainsi.
- **RG13 vérifié à la source** : le pool des relecteurs est lu par `PresenceRepository.findBySessionId`, **sans filtrer la source**. Une présence manuelle rend donc bien l'étudiant éligible au tirage, comme une présence marquée avec le code.
- **Écart frontend assumé** : « action masquée ou désactivée pour un étudiant déjà présent » n'est pas réalisable — aucune opération ne liste les présences d'une session (même famille de limite que l'absence de relecture d'une session, section 11 du cahier des charges). L'écran affiche donc l'état **après** la réponse du serveur (`201`, ou `409` reflété en « Déjà présent ») au lieu de le deviner, ce qui évite tout recalcul de règle côté client (F3).

---

## EF11

### Points de gel (RG14)
- **Dépôt d'exercice** → `409 SESSION_CLOTUREE`. En place depuis l'EF3 : le contrôle de clôture précède celui du doublon, si bien qu'un étudiant ayant déjà déposé reçoit bien `SESSION_CLOTUREE`.
- **Notation** → `409 SESSION_CLOTUREE` sur `POST /api/relectures/{id}`. Le contrôle est placé **avant** `RELECTURE_DEJA_RENDUE` : une session clôturée interdit aussi la correction de l'EF7 (RG8), donc renvoyer l'appelant vers l'EF7 serait trompeur.
- **Remplacement de lien** (EF4) et **correction de note** (EF7) : le même gel les attend (RG11, RG8). Ces deux issues étant hors de la v0.1, le contrôle arrivera avec elles.

### Ce que la clôture ne fait pas
- Elle ne touche ni aux présences, ni aux exercices, ni aux relectures déjà enregistrés : le contrat dit « gèle tout **dépôt** et toute **notation** », et l'issue de référence ne mentionne que ces deux points. Marquer sa présence avec un code encore valide reste donc possible.
- Elle ne « solde » pas les relectures en attente : elles le restent définitivement, sans erreur et sans disparaître des missions du relecteur.

### Décision — reclôturer une session
Le contrat ne déclare que `200` et `404` sur cette opération : clôturer une session **déjà** clôturée répond donc `200` sans rien réécrire, plutôt qu'un `409` qui n'y figure pas. Un `409` aurait de toute façon été ambigu, ce code servant déjà à refuser une action *sur* une session clôturée.

### Tests backend
- Clôture valide → `200 { id, cloturee: true }`, et `session.cloturee` vaut bien `true` en base.
- Session inconnue → `404 SESSION_INCONNUE`, au format `{ code, message }` sans stack trace.
- Reclôture → `200`, sans réécriture (aucun `save` supplémentaire).
- Dépôt d'exercice après clôture → `409 SESSION_CLOTUREE` (couvert par l'EF3).
- Notation après clôture → `409 SESSION_CLOTUREE`, et **rien n'est écrit** : la relecture reste `EN_ATTENTE`, l'exercice ne passe pas à `RELU` (D4).
- Sur une session clôturée, la clôture prime sur « déjà rendue » (décision ci-dessus).
- Une relecture restée en attente à la clôture reste listée dans les missions du relecteur.

### Contraintes techniques
- **Aucune migration** : `session.cloturee` existe depuis `V1`, B5 n'est donc pas sollicité.
- **Aucune opération ajoutée au contrat** : `POST /api/sessions/{id}/cloture` y figurait déjà.
- Le `409` de la notation ne fait qu'ajouter un `code` à un statut **déjà déclaré** sur `POST /api/relectures/{id}`.

### Livrables
- Endpoint + service + DTO de sortie ; tests unitaires (clôture, idempotence, gel de la notation) et d'intégration (200 / 404, notation refusée, dépôt refusé).
- Frontend : bouton **Clôturer** sur les sessions de l'écran formateur, et état « Clôturée — dépôts et notes gelés » tel que renvoyé par le serveur.
