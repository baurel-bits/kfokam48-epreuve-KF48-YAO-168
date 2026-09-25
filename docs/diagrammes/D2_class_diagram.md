# D2 — Diagramme de classes
```mermaid
classDiagram
    class Session {
        +Long id
        +String titre
        +Long promotionId
        +String code
        +LocalDateTime ouvertureAt
        +LocalDateTime expirationAt
        +boolean cloturee
    }

    class Presence {
        +Long id
        +Long sessionId
        +Long etudiantId
        +SourcePresence source
        +LocalDateTime datePresence
    }

    class Exercice {
        +Long id
        +Long sessionId
        +Long etudiantId
        +String lien
        +StatutExercice statut
    }

    class Relecture {
        +Long id
        +Long exerciceId
        +Long relecteurId
        +Integer note
        +String commentaire
        +boolean validee
    }

    Session "1" -- "0..*" Presence : contient
    Session "1" -- "0..*" Exercice : contient
    Exercice "1" -- "0..1" Relecture : fait l'objet de