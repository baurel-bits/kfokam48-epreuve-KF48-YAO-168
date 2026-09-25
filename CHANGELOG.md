# Journal des modifications — KFOKAM48 (matricule 168)

Dépôt : <https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168>

Ce fichier suit l'esprit de *Keep a Changelog* (une section par version, catégories
`Ajouté` / `Modifié` / `Corrigé`). Le projet étant une épreuve à jalons, chaque
version correspond à une **étape de la démarche** du cahier des charges (section 10)
et non à une release publiée. Chaque entrée cite l'issue GitHub, la pull request qui
l'a fermée et le commit de fusion, afin que toute affirmation soit vérifiable dans
l'historique :

```bash
git log --oneline --decorate main
```

## [v1.0] — 2026-09-25 — finalisation

Étape 4 et 5 de la démarche : documentation de fin, relecture du dépôt et soumission.

### Ajouté
- `CHANGELOG.md` : ce journal, rétrospectif des jalons `analyse`, `depart`, `v0.1`
  et de l'enveloppe.
- `SOUMISSION.md` : fiche de soumission (matricule, dépôt, hash relevé, procédure de
  démarrage et de vérification, choix techniques, écarts assumés).
- `docs/JOURNAL.md` : complété pour couvrir les **cinq** étapes de la démarche ; le
  fichier ne contenait que l'étape d'analyse.
- `scripts/fermer-issues.mjs` : fermeture des issues terminées par label `efN`, avec
  un commentaire de preuve (livrable, tests, branche, PR de fusion, écarts de la
  section 11). Rejouable, `--apercu` fonctionne sans jeton.

### Modifié
- `README.md` : tableau des trois écrans (F2) et section « Vérifications » mis à jour
  pour EF4, EF7, EF10 et EF11 ; prérequis, variables d'environnement et limites
  connues complétés. Le parcours d'installation a été rejoué depuis un **clone
  vierge** (voir `SOUMISSION.md`).
- Traçabilité des issues : les commits de fusion sont exclus du décompte de preuves
  (`--no-merges`), un merge n'apportant pas de preuve qui lui soit propre.

### Traçabilité des issues
Les 11 issues du backlog (#8 à #18) ont été fermées avec un commentaire de preuve,
après fusion de leur pull request respective sur `main` :
`120c831` → `bc2dc13` → `6956fba`.

## [Enveloppe] — issues Should, correctif de session

Étape 3 de la démarche. Trois exigences classées **Should** ont été réalisées après
le périmètre `v0.1`, chacune avec sa propre issue ouverte avant le code, ainsi que le
correctif d'un défaut d'usage constaté en recette.

### Corrigé
- **Le code de session ne survivait pas au rechargement de la page** (PR #26,
  fusion `1fda521`, commit `5287022`). Le contrat n'expose aucune opération qui
  relirait une session, et le code de présence reste valable 15 min (RG1) : le
  navigateur du formateur mémorise donc localement les sessions qu'il a ouvertes et
  les réaffiche (`frontend/src/features/session/sessionsOuvertes.ts`). La validité
  d'un code reste décidée par le serveur (`410 CODE_EXPIRE`) ; l'écran ne recalcule
  aucune règle métier (F3).

### Ajouté
- **EF4 — remplacer le lien de son exercice** (issue #16, PR #30, fusion `212f45b`,
  commit `d50f20d`) : `PUT /api/exercices/{id}/lien`, refusé en
  `409 RELECTURE_COMMENCEE` dès qu'un relecteur a été assigné (RG11, Q13). Conformément
  à RG11 et au contrat, l'ébauche d'issue qui ouvrait le remplacement jusqu'à la note
  *rendue* a été abandonnée : voir l'écart documenté en section 11 du cahier des charges.
- **EF7 — corriger sa note avant la clôture** (issue #17, PR #29, fusion `a6ac841`,
  commit `a02f52d`) : `PUT /api/relectures/{id}/correction`. L'ancienne note est
  archivée dans `correction_relecture` avant remplacement, `rendu_at` reste
  inchangé, et une relecture encore `EN_ATTENTE` est refusée en
  `409 RELECTURE_NON_RENDUE` (statut absent du contrat, ajouté comme symétrique du
  `409 RELECTURE_DEJA_RENDUE` de l'EF6).
- **EF10 — ajouter une présence manuellement** (issue #18, PR #28, fusion `4a5eb1d`,
  commit `26fb013`) : `POST /api/presences/manuelles`. La `source = FORMATEUR` (RG12)
  est décidée par l'opération appelée, jamais reçue du client ; une présence déjà
  saisie répond `409 DEJA_PRESENT`.

## [v0.1] — périmètre Must

Étape 2 de la démarche : les huit exigences `Must`, **une branche par issue** et une
PR liée à l'issue fermée.

| EF | Issue | PR | Fusion | Contenu |
|---|---|---|---|---|
| EF1 | #8 | #19 | `c82db62` | Ouverture d'une session et code de présence à 6 caractères (`SecureRandom`), expiration à +15 min (RG1) |
| EF2 | #9 | #20 | `4a48257` | Marquage de présence par code : `400 CODE_INCONNU`, `410 CODE_EXPIRE`, `409 DEJA_PRESENT`, puis `429 TROP_DE_TENTATIVES` (RG3) |
| EF3 | #10 | #21 | `55ab013` | Dépôt du lien d'exercice (`http(s)`, ≤ 500 caractères), statut `DEPOSE`, assignation de l'EF5 en effet de bord |
| EF5 | #11 | #22 | `45e7f8f` | Assignation automatique et aléatoire d'un relecteur parmi les présents, différent de l'auteur (RG4, RG5, RG13) |
| EF6 | #12 | #23 | `8248320` | Note entière 0–20 et commentaire ; `409 RELECTURE_DEJA_RENDUE`, `404 RELECTURE_INCONNUE` |
| EF8 | #13 | #24 | `38468b4` | L'étudiant relu consulte sa note et son commentaire, jamais l'identité du relecteur (RG6, Q8) |
| EF9 | #14 | #25 | `28034b5` | Tableau de bord par promotion : présences, exercices déposés, moyenne des notes, relectures en attente (RG9) |
| EF11 | #15 | #27 | `9bc04ff` | Clôture de session : gel des dépôts et de la notation (RG14), reclôture idempotente en `200` |

### Corrigé
- EF1 — conformité du format des instants de la réponse au contrat (`date-time`) :
  `4d66351`.
- RG3 — le compteur d'échecs ne concernait pas les codes **inconnus** (le cas le plus
  fréquent) : portée à deux niveaux, par couple (étudiant, session) et par étudiant
  (`session_id IS NULL`), avec la migration `V3__rg3_compteur_par_etudiant.sql` :
  `2b92806`.
- Flyway — démarrage fiabilisé sur un schéma préexistant non vide
  (`baseline-version: 0`, sans quoi `V1` serait considérée comme déjà appliquée) :
  `fb6a983`.
- Frontend — page d'accueil servie à la racine `/` (elle répondait `404`) : `2e03728`.

### Ajouté (outillage de vérification)
- `scripts/parcours-navigateur.mjs` : parcours complet piloté par le DevTools
  Protocol, sans aucune dépendance (44 vérifications) — hydratation React, CORS,
  erreurs du contrat, mise en page mobile (ENF1), non-divulgation du relecteur (RG6) :
  `76ca42f`.
- `scripts/reinitialiser-donnees-demo.mjs` : remise à zéro des données d'usage, avec
  sauvegarde préalable dans `backup/` et jeu de référence `V2` toujours conservé :
  `d575b4f`.

> **Nota sur le jalon v0.1** : les huit issues `Must` ont été fusionnées sur `main`
> par les PR #19 à #25 et #27 (dernière : `9bc04ff`). Le commit `[JALON] v0.1` qui
> matérialise le jalon a été posé après coup, une fois la documentation de fin
> rédigée : il marque le jalon sans se limiter au descriptif des issues `Must`, que
> la colonne « Fusion » du tableau ci-dessus permet de retrouver.

## [JALON] depart — `ebcea83`

Début de l'implémentation, après validation de l'analyse.

### Ajouté
- Squelette d'architecture backend (Java / Spring Boot, Maven avec wrapper `mvnw`)
  et frontend (Next.js, App Router) : `cbb6510`.
- Outillage du backlog GitHub (création et enrichissement des issues) et règles
  d'exclusion Git locales : `94b9ec1`.

## [JALON] analyse — `6a26d60`

Étape 1 de la démarche : conception, avant toute ligne de code applicatif.

### Ajouté
- `docs/CAHIER_DES_CHARGES.md` : périmètre, 11 exigences fonctionnelles, 4 exigences
  non fonctionnelles, 14 règles de gestion, zones d'ombre tranchées, contraintes
  techniques et démarche en cinq étapes : `a4e3524`.
- `docs/diagrammes/` : D1 (cas d'utilisation), D2 (classes / modèle de données),
  D3 (séquence présence), D4 (état d'un exercice), en Mermaid : `a4e3524`.
- `docs/JOURNAL.md` : journal de bord, étape 1 : `a4e3524`.
- `api/contrat.yaml` : contrat OpenAPI complété à **13 opérations**, dont les 5
  imposées par le sujet, ainsi que `docs/BACKLOG.md` et `docs/BACKLOG_DETAIL.md`
  (11 issues, priorisées et reliées aux EFx / RGx) : `05290b0`.
- Fusion de la conception sur `main` : PR #7, `9f19ac3`.

## [initial] — `8204fc5`

Premier commit : dépôt de la structure imposée (`/docs`, `/api`, `/backend`,
`/frontend`).
