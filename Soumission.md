# 🎓 Soumission — Épreuve Finale Fullstack KFOKAM48

> **Candidat :** DOMGUIA BEMMO  
> **Matricule :** `KF48-YAO-168`  
> **Centre d'examen :** Yaoundé  
> **Date de soumission :** Septembre 2026

---

## 📌 Identification du Projet

| Paramètre | Valeur |
| :--- | :--- |
| **Dépôt GitHub (Public)** | [baurel-bits/kfokam48-epreuve-KF48-YAO-168](https://github.com/baurel-bits/kfokam48-epreuve-KF48-YAO-168.git) |
| **Commit Final (SHA-1)** | `[]` |
| **Statut du Build** | 🟢 PRÊT POUR ÉVALUATION |

---

## 🛠️ Stack Technique & Architecture

* **Backend :** Java 17 · Spring Boot 3 · Spring Data JPA · Flyway Migration · H2/PostgreSQL
* **Frontend :** Next.js 14 (App Router) · TypeScript · Tailwind CSS · Axios
* **Contrat API :** Spécification OpenAPI / Swagger (`api/contrat.yaml`)
* **Qualité & CI :** Architecture basée sur les principes SOLID, gestion globale des exceptions et migrations SQL versionnées.

---

## 🚀 Guide de Démarrage Rapide

### Option 1 — Démarrage combiné (Recommandé)

```bash
# Lancement simultané du Backend Spring Boot et du Frontend Next.js
(cd backend && ./mvnw spring-boot:run) & (cd frontend && npm run dev)