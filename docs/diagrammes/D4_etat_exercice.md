# D4 — Cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : Dépôt du lien par l'étudiant
    DEPOSE --> EN_ATTENTE_RELECTURE : Attribution d'un relecteur
    EN_ATTENTE_RELECTURE --> RELU : Soumission de la note (0-20)
    RELU --> [*]
```

## Étape 2 — Implémentation de la v0.1

* **Fait :** Initialisation des projets Spring Boot (Java 17) et React, création de la migration Flyway `V1__init_schema.sql`, implémentation de la gestion centralisée des erreurs API, mise en place des endpoints `/api/presences`, `/api/exercices` et `/api/tableau`, ainsi que le développement des 3 vues frontend.

* **Bloqué :** Gestion de l'attribution aléatoire d'un relecteur lors du dépôt d'un exercice lorsqu'aucun étudiant n'est encore enregistré. Le problème a été résolu par une attribution différée lors de la requête de relecture.

* **IA :** Utilisation de l'IA pour générer le boilerplate du `GlobalExceptionHandler` et du schéma Flyway SQL. Le code généré a ensuite été vérifié et aligné avec `contrat.yaml` et les exigences ENF2.
