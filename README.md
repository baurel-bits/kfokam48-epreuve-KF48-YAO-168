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
| Formateur | `/formateur` | EF1 — ouvrir une session, obtenir le code de présence |
| Étudiant | `/etudiant` | EF2/EF3 — choisir son nom, marquer sa présence, déposer son exercice |
| Relecteur | `/relecteur` | EF6 — retrouver l'exercice confié, rendre sa note et son commentaire |

## Vérifications

```bash
# Tests backend : 60 tests sur H2, sans PostgreSQL (B6)
cd backend && ./mvnw test

# Frontend : typage et build de production
cd frontend && npx tsc --noEmit && npm run build

# Parcours navigateur réel, serveurs démarrés (Chrome requis, aucune dépendance)
node scripts/parcours-navigateur.mjs
```

`scripts/parcours-navigateur.mjs` pilote Chrome en headless par le DevTools
Protocol et déroule le parcours complet — accueil → formateur → étudiant →
relecteur — en cliquant réellement sur les formulaires. Il vérifie ce que les
tests MockMvc ne peuvent pas voir : hydratation React, appels API depuis
l'origine du navigateur (CORS), affichage des erreurs du contrat, mise en page
mobile (ENF1) et non-divulgation de l'identité de l'auteur au relecteur (RG6).
Il se termine par un code de sortie non nul en cas d'échec.

## Organisation

```
api/contrat.yaml        Contrat d'API OpenAPI (référence des chemins et statuts)
docs/                   Cahier des charges, backlog, diagrammes D1-D4
backend/src/main/java   Couches controller / service / repository, DTO en sortie
backend/src/main/resources/db/migration   Migrations Flyway versionnées
frontend/src/app        Écrans (App Router)
frontend/src/core/api   Couche d'appel API unique (aucun fetch dans un composant)
frontend/src/features   Appels API par domaine fonctionnel
scripts/                Outillage (backlog GitHub, parcours navigateur)
```

## Limites connues

- **Pas d'authentification** : le sujet exclut le couple identifiant/mot de passe
  (Q1) ; l'étudiant se choisit dans une liste. La restriction « réservé au
  relecteur » (RG6) repose donc sur la comparaison d'identifiants, et non sur un
  véritable contrôle d'accès (voir `docs/CAHIER_DES_CHARGES.md`, section 11).
- **Aucun envoi d'e-mail** : hors périmètre du sujet.
- L'écran **« notes reçues »** (EF8, issue #13) n'est pas encore livré : la note
  rendue par un relecteur n'est pas encore consultable côté étudiant.
