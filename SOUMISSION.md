# Fiche de soumission — épreuve KFOKAM48

## Identité

| | |
|---|---|
| **Matricule** | `168` |
| **Nom de l'application** | kfokam48-epreuve-KF48-YAO-168 |
| **Dépôt GitHub (public)** | <https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168> |
| **Branche soumise** | `main` |
| **Hash du commit `[JALON] v1.0`** | `4338ff1` |
| **Date de soumission** | 25 septembre 2026, avant 18h00 |
| **Frontend choisi** | Next.js (App Router) — justification en §3 |

Le hash ci-dessus identifie le commit `[JALON] v1.0`, qui clôt la documentation de
fin. Seul ce fichier est ensuite modifié pour le relever : la pointe du dépôt soumis
est donc le commit suivant, et les deux sont visibles avec `git log --oneline -3`.

## 1. Contenu du dépôt

Structure imposée par le sujet, complète :

```
api/contrat.yaml     Contrat OpenAPI : 13 opérations, dont les 5 imposées, respectées à la lettre
backend/             Java 21 · Spring Boot 3.4.3 · Maven (wrapper mvnw committé) · PostgreSQL · Flyway V1→V3
frontend/            Next.js 16 · React 19 · TypeScript · Tailwind 4 — trois écrans (F2)
docs/                CAHIER_DES_CHARGES.md · JOURNAL.md · BACKLOG.md · BACKLOG_DETAIL.md · diagrammes D1-D4
scripts/             Vérifications et outillage : parcours navigateur, remise à zéro des données, backlog GitHub
README.md            Installation testée, variables d'environnement, organisation, limites connues
CHANGELOG.md         Rétrospectif par jalon (analyse · depart · v0.1 · enveloppe · v1.0)
SOUMISSION.md        Ce document
```

## 2. Couverture des exigences

Les **11 exigences fonctionnelles** du cahier des charges sont implémentées, chacune
par une issue GitHub ouverte avant le code, fermée par une pull request fusionnée sur
`main` :

| EF | Issue | PR | EF | Issue | PR |
|---|---|---|---|---|---|
| EF1 — ouvrir une session | [#8](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/8) | #19 | EF7 — corriger sa note | [#17](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/17) | #29 |
| EF2 — marquer sa présence | [#9](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/9) | #20 | EF8 — consulter sa note | [#13](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/13) | #24 |
| EF3 — déposer son exercice | [#10](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/10) | #21 | EF9 — tableau de bord | [#14](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/14) | #25 |
| EF4 — remplacer le lien | [#16](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/16) | #30 | EF10 — présence manuelle | [#18](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/18) | #28 |
| EF5 — assigner un relecteur | [#11](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/11) | #22 | EF11 — clôturer une session | [#15](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/15) | #27 |
| EF6 — noter un exercice | [#12](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168/issues/12) | #23 | | | |

Les contraintes techniques imposées sont respectées et vérifiables :

- **B1-B2** — Maven avec `mvnw` committé ; `api/contrat.yaml` jamais modifié, tous
  les chemins et statuts déclarés sont ceux servis.
- **B3** — séparation `controller` / `service` / `repository`, DTO en sortie
  uniquement (`backend/src/main/java/com/kfokam48/app/features/*/`).
- **B4** — `@RestControllerAdvice` centralisé : toute erreur métier sort au format
  `{ code, message }`, jamais de stack trace (`common/error/`).
- **B5** — trois migrations versionnées (`V1__init_schema`, `V2__seed_donnees_demo`,
  `V3__rg3_compteur_par_etudiant`), Hibernate en `ddl-auto: validate`.
- **B6** — **161 tests** (unitaires sur les règles métier + intégration MockMvc sur
  les endpoints), exécutables sans PostgreSQL (H2).
- **F1-F3** — Next.js justifié ci-dessous ; trois écrans ; couche d'appel API unique
  (`frontend/src/core/api/`) sans aucun `fetch` dans un composant, et aucun recalcul
  de règle métier côté client (la moyenne des notes est fournie par l'API).

## 3. Démarrage depuis un clone vierge

Trois commandes, testées sur un clone neuf (voir §4). Toutes les valeurs sont
surchargeables par variables d'environnement.

**1. Base de données** (PostgreSQL 16 ; le schéma est créé par Flyway au démarrage)

```bash
docker run --name exam-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=exam_db -p 5432:5432 -d postgres:16
```

**2. Backend** — <http://localhost:8080>, documentation <http://localhost:8080/swagger-ui.html>

```bash
cd backend && ./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run
```

**3. Frontend** — <http://localhost:3000>

```bash
cd frontend && npm install && npm run dev
```

Prérequis : JDK 21+, Node.js 20+ (22+ pour le script de parcours). Les données de
démonstration (une promotion, cinq étudiants) sont chargées automatiquement par la
migration `V2`.

## 4. Vérifications exécutées

| Vérification | Commande | Résultat |
|---|---|---|
| Tests backend + intégration | `cd backend && ./mvnw test` | **161 tests, 0 échec** |
| Typage et build frontend | `cd frontend && npx tsc --noEmit && npm run build` | succès |
| Parcours navigateur réel (Chrome, sans dépendance) | `node scripts/parcours-navigateur.mjs` | **44/44** |
| Installation depuis un clone vierge | les 3 commandes du §3 | rejouées sur un clone de `main` (`fc15ecb`) : conteneur `postgres:16`, migrations `V1`→`V3` appliquées sur un schéma **vide**, données de démonstration présentes, backend et frontend démarrés, puis parcours navigateur **44/44** rejoué contre ce clone |

Le parcours navigateur traverse les trois écrans dans l'ordre du besoin
(accueil → formateur → étudiant → relecteur → retour formateur) et vérifie ce que
les tests MockMvc ne peuvent pas voir : hydratation React, appels API depuis
l'origine du navigateur (CORS), affichage des erreurs du contrat, mise en page
mobile ≤ 390 px (ENF1) et non-divulgation de l'identité du relecteur (RG6).

## 5. Choix techniques et justifications

**Next.js plutôt que React seul** (F1). Le sujet laissait le choix en demandant de le
justifier. Next.js a été retenu parce qu'il fournit le routage de fichiers — les
trois écrans exigés par F2 deviennent trois routes (`/formateur`, `/etudiant`,
`/relecteur`) — ainsi qu'un Proxy de développement intégré pour joindre l'API sans
configurer de serveur web séparé ni de CORS de développement.

**Séparation stricte des couches plutôt que JPA exposé** (B3). Les entités ne
franchissent jamais la frontière HTTP : chaque réponse est un DTO, ce qui évite les
fuites de schéma et l'énumération paresseuse non maîtrisée.

**Gestion des erreurs par exception métier** (B4). Les services lèvent une
`ExceptionMetier` portant un `CodeErreur` ; le `@RestControllerAdvice` la traduit en
`{ code, message }` avec le statut déclaré au contrat. Le contrôle des statuts vit à
un seul endroit, et le contrat reste la source de vérité.

**Une couture par interface pour l'assignation** (`AssignateurRelecteur`, EF5). Le
tirage du relecteur est isolé derrière une interface : les tests d'intégration
peuvent le rendre déterministe sans dépendre du hasard, et les tests unitaires
peuvent l'observer sans se coupler au repository.

**Le code de session mémorisé côté navigateur** (correctif de l'enveloppe). Le
contrat n'offre aucune opération qui relirait une session, et ses opérations
additionnelles étaient réservées aux EF du cahier des charges. Le code de présence
est donc mémorisé localement par le navigateur du formateur et réaffiché au
rechargement, sans ajouter de route au contrat. La validité reste décidée par le
serveur (`410 CODE_EXPIRE`).

## 6. Écarts et interprétations tranchées

Les points que le sujet ou le contrat ne tranchaient pas sont documentés et justifiés
en **section 11 du cahier des charges** (`docs/CAHIER_DES_CHARGES.md`). Les
principaux :

| Point | Décision |
|---|---|
| RG3 — blocage après 5 échecs | `429 TROP_DE_TENTATIVES` (statut **ajouté**, les codes imposés restent inchangés) ; compteur à deux portées, migration `V3` |
| Liste des étudiants d'une promotion | Ajout de `GET /api/promotions/{promotionId}/etudiants`, prérequis de EF2/EF3/EF10 (Q1) |
| EF4 — « aucune relecture commencée » | RG11 et le `409 RELECTURE_COMMENCEE` du contrat font foi : une relecture est commencée dès l'assignation, donc l'EF4 n'est atteignable que sur un exercice resté `DEPOSE` |
| EF6 / EF7 — statuts non déclarés | `404 RELECTURE_INCONNUE` (EF6) et `409 RELECTURE_NON_RENDUE` (EF7), ajoutés pour qu'une erreur de saisie ne devienne pas un `500` |
| EF9 — portée du tableau | `promotionId` (opération imposée), une ligne par étudiant ; le `403` d'ébauche est abandonné, faute d'authentification (Q1) |
| EF10 — étudiant inconnu | `404` sur `POST /api/presences/manuelles`, `400` sur `POST /api/presences` : chaque opération suit sa propre déclaration (B2) |
| EF11 — reclôture | Idempotente, `200` avec `cloturee: true`, sans réécrire la session |
| Sessions ouvertes | Aucune opération ajoutée : le code est mémorisé par le navigateur du formateur |

## 7. Limites connues et assumées

- **Pas d'authentification** (Q1) : l'étudiant se choisit dans une liste. La
  restriction « réservé au relecteur » (RG6) repose sur la comparaison
  d'identifiants et non sur un véritable contrôle d'accès — limite explicitement
  assumée en section 11 du cahier des charges.
- **EF4** restreint aux exercices sans relecteur assigné (conséquence de RG11, §6).
- **EF7** limité à la relecture qui vient d'être rendue : aucune opération du contrat
  ne liste les relectures déjà rendues.
- **Le formateur ne revoit que les sessions ouvertes depuis son navigateur** :
  aucune opération ne liste les sessions côté serveur. L'EF10 en dépend.
- **Aucun envoi d'e-mail** : hors périmètre du sujet.
- **Habillage graphique volontairement sobre** : hors barème selon le sujet.
