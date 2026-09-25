# 📌 Informations Utiles du Projet — Fullstack Exam

Ce document regroupe tous les liens, accès, identifiants par défaut et commandes utiles pour exploiter et tester l'architecture complète.

---

## 🔑 1. Identifiants par Défaut (Seed Automatique)

Au démarrage du backend, deux comptes sont automatiquement créés en base de données :

| Rôle | Adresse Email | Mot de passe | Permissions |
| :--- | :--- | :--- | :--- |
| **Super Administrateur** | `admin@example.com` | `Admin1234!` | `ROLE_ADMIN` (accès complet, création notifications) |
| **Utilisateur Standard** | `user@example.com` | `User1234!` | `ROLE_USER` (profil, consultation notifications) |

> ⚙️ Ces identifiants sont personnalisables via les variables d'environnement `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `USER_EMAIL`, `USER_PASSWORD` dans votre fichier `.env`.

---

## 🌐 2. Liens de l'Application Frontend (Next.js)

Le frontend est servi sur le port **3000** avec préfixe de langue automatique (`/[locale]`) :

| Page | URL | Description |
| :--- | :--- | :--- |
| **Accueil (FR)** | [http://localhost:3000/fr](http://localhost:3000/fr) | Page d'accueil en français (redirection auto depuis `/`) |
| **Accueil (EN)** | [http://localhost:3000/en](http://localhost:3000/en) | Page d'accueil en anglais |
| **Connexion** | [http://localhost:3000/fr/login](http://localhost:3000/fr/login) | Formulaire de login JWT |
| **Inscription** | [http://localhost:3000/fr/register](http://localhost:3000/fr/register) | Formulaire de création de compte |
| **Tableau de bord** | [http://localhost:3000/fr/dashboard](http://localhost:3000/fr/dashboard) | Espace sécurisé (profil, notifications) |

---

## ⚙️ 3. Liens de l'API Backend & Documentation (Spring Boot)

Le backend est servi sur le port **8080** :

| Service | URL | Description |
| :--- | :--- | :--- |
| **Base API REST** | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) | Racine des endpoints |
| **Documentation Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Interface interactive pour tester tous les endpoints |
| **Spécification OpenAPI (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Schéma JSON OpenAPI v3 |

### Endpoints Principaux :
- `POST /api/v1/auth/register` : Inscription d'un utilisateur
- `POST /api/v1/auth/login` : Connexion, retourne les jetons JWT (`accessToken`, `refreshToken`)
- `POST /api/v1/auth/refresh` : Rafraîchissement automatique du token d'accès
- `POST /api/v1/auth/logout` : Déconnexion sécurisée
- `GET /api/v1/auth/me` : Informations du profil connecté (Bearer Token)
- `GET /api/v1/notifications` : Liste des notifications de l'utilisateur connecté
- `GET /api/v1/notifications/unread-count` : Nombre de notifications non lues
- `PATCH /api/v1/notifications/{id}/read` : Marquer une notification comme lue
- `PATCH /api/v1/notifications/read-all` : Marquer toutes les notifications comme lues
- `POST /api/v1/notifications` : Créer une notification (`ROLE_ADMIN` requis)

---

## 🗄️ 4. Base de Données (PostgreSQL Docker)

| Propriété | Valeur |
| :--- | :--- |
| **Hôte** | `localhost` |
| **Port** | `5432` |
| **Nom de la Base** | `exam_db` |
| **Utilisateur** | `postgres` |
| **Mot de passe** | `postgres` |
| **URL JDBC** | `jdbc:postgresql://localhost:5432/exam_db` |
| **Conteneur Docker** | `exam-postgres` |

### Commandes Utiles PostgreSQL :
```bash
# Se connecter au shell PostgreSQL dans le conteneur
docker exec -it exam-postgres psql -U postgres -d exam_db

# Lister les tables
\dt

# Voir les utilisateurs créés
SELECT id, email, first_name, last_name, role FROM users;

# Quitter
\q
```

---

## 📧 5. Service d'Emails (SMTP / MailHog)

Le backend est configuré pour communiquer avec un serveur SMTP (MailHog par défaut en développement) :

| Service | Port / URL | Description |
| :--- | :--- | :--- |
| **Serveur SMTP (Spring Mail)** | `localhost:1025` | Port SMTP configuré dans `application.yml` |
| **Interface Web MailHog** | [http://localhost:8025](http://localhost:8025) | Boîte de réception web pour visualiser les emails sans envoi réel |

### Lancer MailHog (si besoin de visualiser les emails en local) :
```bash
docker run -d --name exam-mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

---

## 🚀 6. Commandes de Démarrage Rapide

### 1. Démarrer la base de données :
```bash
docker start exam-postgres
# ou via docker-compose :
docker compose up -d postgres
```

### 2. Démarrer le Backend :
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Démarrer le Frontend :
```bash
cd frontend
npm run dev
```

### 4. Tests et Compilations de Vérification :
```bash
# Backend : Compilation & Tests
cd backend && ./mvnw test

# Frontend : Build Next.js & Vérification TypeScript
cd frontend && npm run build
```
