# D3 — Séquence : « marquer sa présence »

Cas d'usage : **EF2 — L'étudiant marque sa présence avec un code**.
Endpoint imposé : `POST /api/presences` (`{ code, etudiantId }`).
Règles couvertes : **RG1** (expiration 15 min), **RG2** (code invalide/expiré refusé), **RG3** (blocage après 5 échecs).

Format d'erreur imposé : `{ "code": "...", "message": "..." }`.

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Frontend étudiant
    participant API as PresenceController
    participant S as PresenceService
    participant R as PresenceRepository
    participant DB as PostgreSQL

    E->>F: Saisit le code de présence
    F->>API: POST /api/presences { code, etudiantId }
    API->>API: Validation @Valid (code, etudiantId requis)
    API->>S: marquerPresence(code, etudiantId)

    S->>R: findByCode(code)
    R->>DB: SELECT ... FROM session WHERE code = :code
    DB-->>R: ligne session
    R-->>S: Session (ou vide)

    alt Blocage actif — RG3
        Note over S: 5 échecs atteints pour ce couple en moins de 2 min
        S-->>API: throw TropDeTentativesException
        API-->>F: 429 { "code": "TROP_DE_TENTATIVES", "message": "Trop de tentatives : réessayez dans 2 minutes." }
        F-->>E: Blocage affiché (2 minutes)
    else Code expiré — RG1 / RG2
        Note over S: expirationAt < maintenant
        S->>R: incrementerEchec(etudiantId, sessionId)
        Note right of R: RG3 — compteur (étudiant, session)
        S-->>API: throw CodeExpireException
        API-->>F: 410 { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
        F-->>E: Affiche le message d'erreur
    else Déjà présent — RG2
        S->>R: existsBySessionIdAndEtudiantId(sessionId, etudiantId)
        R->>DB: SELECT 1 FROM presence WHERE session_id = ... AND etudiant_id = ...
        DB-->>R: true
        R-->>S: true
        S-->>API: throw DejaPresentException
        API-->>F: 409 { "code": "DEJA_PRESENT", "message": "Étudiant déjà marqué présent." }
        F-->>E: Affiche le message d'erreur
    else Cas nominal
        S->>R: save(Presence{ source: ETUDIANT, datePresence: maintenant })
        R->>DB: INSERT INTO presence (session_id, etudiant_id, source, date_presence)
        DB-->>R: id généré
        R-->>S: Presence
        S-->>API: Presence
        API-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        F-->>E: Confirme la présence enregistrée
    end
```

## Codes de statut couverts

| Branche | Statut | `code` | Origine |
|---|---|---|---|
| Blocage actif (5 échecs / 2 min) | `429` | `TROP_DE_TENTATIVES` | **RG3** |
| Cas nominal | `201` | — | **EF2** : `source = ETUDIANT` |
| Code expiré | `410` | `CODE_EXPIRE` | **RG1** (expiration 15 min) + **RG2** |
| Déjà présent | `409` | `DEJA_PRESENT` | **RG2** (unicité `(sessionId, etudiantId)`) |

## Points de vigilance

- **RG3 (blocage) — tranché** : le contrôle du blocage s'effectue **avant** la validation du code et renvoie **`429 TROP_DE_TENTATIVES`**. Le compteur d'échecs (`tentative_saisie`) est incrémenté sur tout code refusé (expiré ou inconnu) ; à 5 échecs, le couple (étudiant, session) est bloqué 2 minutes. Les codes imposés `400`/`409`/`410` restent **inchangés** : le `429` est un **ajout** documenté dans `api/contrat.yaml`.
- **Priorité des contrôles** : le diagramme évalue l'expiration **avant** l'unicité de présence ; si un code expiré concerne un étudiant déjà présent, le client reçoit `410` (et non `409`).
- **`{ code, message }`** : ce format imposé est **différent** du `ApiResponse` du socle actuel (`success/message/data/errors`). Le `GlobalExceptionHandler` devra exposer une réponse dédiée pour respecter le contrat.
- **Style** : le diagramme suit la convention du D3 existant (participants `Étudiant / Frontend / Controller / Service`). L'**Annexe C** n'étant pas fournie dans le contexte, je m'aligne sur ce style — dis-moi si elle impose une autre convention (noms de participants, `Note`, `rect`).
