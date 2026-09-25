# D3 — Séquence : Marquer sa présence
```mermaid
sequenceDiagram
    participant E as Étudiant
    participant F as Frontend
    participant API as PresenceController
    participant S as PresenceService

    E->>F: Saisit le code de présence
    F->>API: POST /api/presences { code, etudiantId }
    API->>S: enregistrerPresence(code, etudiantId)
    
    alt Code expiré (RG1)
        S-->>API: Exception CodeExpire
        API-->>F: 410 { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
    else Déjà présent
        S-->>API: Exception DejaPresent
        API-->>F: 409 { "code": "DEJA_PRESENT", "message": "Étudiant déjà marqué présent." }
    else Code Inconnu
        S-->>API: Exception CodeInconnu
        API-->>F: 400 { "code": "CODE_INCONNU", "message": "Code de présence invalide." }
    else Succès (Nominal)
        S-->>API: Entity Presence
        API-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
    end