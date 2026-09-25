# kfokam48-epreuve-KF48-YAO-168

Application de **prise de présence**, de **dépôt d'exercices** et de **relecture
par les pairs** (épreuve KFOKAM48, matricule 168).

- Backend : Java 21, Spring Boot 3.4.3, Maven (wrapper), PostgreSQL, migrations **Flyway**.
- Frontend : Next.js 16 (App Router), React 19, Tailwind 4, TypeScript.

Le contrat d'API est la référence : [`api/contrat.yaml`](api/contrat.yaml)
(13 opérations, dont les 5 imposées par le sujet).

## Prérequis

| Outil | Version |
|---|---|
| JDK | 21 ou plus récent |
| Node.js | 20 ou plus récent (le script de parcours navigateur utilise `WebSocket` natif : **22+**) |
| PostgreSQL | 16 (ou Docker pour le conteneur ci-dessous) |

## Démarrage

### 1. Base de données

```bash
docker run --name exam-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=exam_db -p 5432:5432 -d postgres:16
```

Le schéma est créé par Flyway au démarrage du backend (`V1` à `V3` dans
`backend/src/main/resources/db/migration`). Aucun `ddl-auto` n'est utilisé :
Hibernate est en `validate` et ne crée jamais de table (B5).

### 2. Backend — http://localhost:8080

```bash
cd backend
./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run
```

Le premier lancement télécharge Maven 3.9.16 (wrapper). Si un schéma préexistant
non vide est rencontré, Flyway le met en base (`baseline-version: 0`) puis
applique les migrations.

Configuration surchargée par variables d'environnement :
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`.

Documentation interactive : http://localhost:8080/swagger-ui.html

### 3. Frontend — http://localhost:3000

```bash
cd frontend
npm install
npm run dev
```

L'URL du backend se règle par `NEXT_PUBLIC_API_URL` (défaut `http://localhost:8080`,
voir `frontend/.env.example`).

L'application s'ouvre sur la page d'accueil `/`, qui mène aux trois écrans exigés
par F2 :

| Écran | Chemin | Contenu |
|---|---|---|
| Formateur | `/formateur` | EF1 — ouvrir une session et obtenir le code · EF11 — clôturer (gel des dépôts et des notes) · EF9 — tableau de bord de la promotion · EF10 — ajouter une présence manuellement |
| Étudiant | `/etudiant` | EF2/EF3 — choisir son nom, marquer sa présence, déposer son exercice · EF4 — remplacer le lien déposé · EF8 — consulter la note reçue |
| Relecteur | `/relecteur` | EF6 — retrouver l'exercice confié, rendre sa note et son commentaire · EF7 — corriger cette note avant la clôture |

## Vérifications

```bash
# Tests backend : 161 tests sur H2, sans PostgreSQL (B6)
cd backend && ./mvnw test

# Frontend : typage et build de production
cd frontend && npx tsc --noEmit && npm run build

# Parcours navigateur réel, serveurs démarrés (Chrome requis, aucune dépendance)
node scripts/parcours-navigateur.mjs
```

`scripts/parcours-navigateur.mjs` pilote Chrome en headless par le DevTools
Protocol et déroule le parcours complet — accueil → formateur → étudiant →
relecteur, puis le retour au formateur pour la clôture et la présence manuelle
(44 vérifications) — en cliquant réellement sur les formulaires. Il vérifie ce que les
tests MockMvc ne peuvent pas voir : hydratation React, appels API depuis
l'origine du navigateur (CORS), affichage des erreurs du contrat, mise en page
mobile (ENF1) et non-divulgation de l'identité de l'auteur au relecteur (RG6).
Il se termine par un code de sortie non nul en cas d'échec.

## Données de démonstration

```bash
node scripts/reinitialiser-donnees-demo.mjs --apercu   # montre ce qui serait supprimé
node scripts/reinitialiser-donnees-demo.mjs            # supprime les sessions des runs automatisés
node scripts/reinitialiser-donnees-demo.mjs --tout     # vide toutes les données d'usage
```

Le jeu de données de référence créé par la migration (`V2` : une promotion, cinq
étudiants) est **toujours conservé** ; seules les données d'usage sont visées, et
une sauvegarde des tables concernées est écrite dans `backup/` avant toute
suppression. Par défaut, le script ne cible que les sessions produites par les
vérifications automatisées, jamais celles ouvertes à la main dans l'interface.

## Organisation

```
api/contrat.yaml        Contrat d'API OpenAPI (référence des chemins et statuts)
docs/                   Cahier des charges, backlog, diagrammes D1-D4
backend/src/main/java   Couches controller / service / repository, DTO en sortie
backend/src/main/resources/db/migration   Migrations Flyway versionnées
frontend/src/app        Écrans (App Router)
frontend/src/core/api   Couche d'appel API unique (aucun fetch dans un composant)
frontend/src/features   Appels API par domaine fonctionnel
scripts/                Outillage (backlog GitHub : création, enrichissement, fermeture des
                        issues ; remise à zéro des données ; parcours navigateur)
```

## Jalons

Le projet est livré par jalons, conformément à la démarche du cahier des charges
(section 10). Les deux jalons de version sont en plus publiés sous forme de tags
annotés :

| Jalon | Tag | Commit | Contenu |
|---|---|---|---|
| `[JALON] analyse` | — | `6a26d60` | Cahier des charges, diagrammes D1-D4, backlog et contrat d'API complété |
| `[JALON] depart` | — | `ebcea83` | Squelette backend / frontend et outillage du backlog |
| `[JALON] v0.1` | `v0.1` | `fc15ecb` | Périmètre `Must` : EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11 |
| `[JALON] v1.0` | `v1.0` | `4338ff1` | Finalisation : `CHANGELOG.md`, `SOUMISSION.md`, README rejoué depuis un clone vierge |

`v0.1` et `v1.0` sont des commits **marqueurs** : le code du périmètre `Must` est
fusionné sur `main` par les PR #19 à #25 et #27 (dernière issue `Must` : `9bc04ff`),
le marqueur ayant été posé au moment de la finalisation du livrable. Le détail par
jalon — issues, correctifs, écarts — est dans [`CHANGELOG.md`](CHANGELOG.md), et la
fiche de soumission dans [`SOUMISSION.md`](SOUMISSION.md).

Visualisation : `git log --oneline --decorate` et `git tag -n`.

## Limites connues

- **Pas d'authentification** : le sujet exclut le couple identifiant/mot de passe
  (Q1) ; l'étudiant se choisit dans une liste. La restriction « réservé au
  relecteur » (RG6) repose donc sur la comparaison d'identifiants, et non sur un
  véritable contrôle d'accès (voir `docs/CAHIER_DES_CHARGES.md`, section 11).
- **Remplacement de lien restreint aux exercices sans relecteur** (EF4, issue #16) :
  RG11 (Q13) parle d'« aucune relecture **commencée** » et le contrat déclare
  `409 RELECTURE_COMMENCEE` ; comme un relecteur est tiré dès le dépôt (EF5), le
  remplacement n'est possible que pour un exercice resté `DEPOSE`, donc déposé
  sans autre étudiant présent. L'ébauche d'issue, qui ouvrait le remplacement
  jusqu'à la note rendue, a été abandonnée au profit de la règle source (voir
  `docs/CAHIER_DES_CHARGES.md`, section 11).
- **Correction d'une note limitée à la relecture courante** (EF7, issue #17) :
  `GET /api/relecteurs/{etudiantId}/relectures-en-attente` ne renvoie, par
  définition contractuelle, que les relectures **non rendues**. Une note déjà
  rendue n'est donc corrigeable que dans la foulée de son rendu, tant que
  l'écran la garde ; après un rechargement, aucune opération ne permet de la
  retrouver (voir `docs/CAHIER_DES_CHARGES.md`, section 11).
- **Aucune relecture d'une session** : le contrat n'offre pas d'opération qui
  renverrait une session (ni sa liste), et ses opérations additionnelles sont
  réservées aux EF du cahier des charges. Le formateur ne peut donc revoir que
  les sessions ouvertes **depuis son navigateur** : leur code y est mémorisé
  localement, ce qui le préserve d'un rechargement de page sans ajouter de route
  au contrat (voir `docs/CAHIER_DES_CHARGES.md`, section 11). L'EF10 en dépend
  aussi : l'ajout manuel de présence se fait sur une session connue de ce
  navigateur, faute d'opération qui listerait les sessions du serveur. Et aucune
  opération ne listant les présences d'une session, l'écran apprend qu'un
  étudiant est déjà présent par le `409 DEJA_PRESENT` du serveur, jamais en le
  supposant.
- **Aucun envoi d'e-mail** : hors périmètre du sujet.
- **Tableau de bord sans contrôle d'accès** (EF9, issue #14) : l'ébauche
  d'issue prévoyait qu'un formateur ne puisse pas consulter la promotion d'un
  autre (`403 ACCES_REFUSE`). Faute d'authentification (Q1), il n'existe aucune
  notion de « sa » promotion : la restriction est abandonnée, et l'opération
  imposée `GET /api/tableau` ne déclare d'ailleurs aucun `403` (voir
  `docs/CAHIER_DES_CHARGES.md`, section 11).
