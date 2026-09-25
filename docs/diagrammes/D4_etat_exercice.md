# D4 — Cycle de vie d'un exercice
```mermaid
stateDiagram-v2
    [*] --> DEPOSE : Dépôt du lien par l'étudiant
    DEPOSE --> EN_ATTENTE_RELECTURE : Attribution d'un relecteur
    EN_ATTENTE_RELECTURE --> RELU : Soumission de la note (0-20)
    RELU --> [*]