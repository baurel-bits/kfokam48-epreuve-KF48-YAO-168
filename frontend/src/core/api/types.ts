/**
 * Format d'erreur imposé par le contrat (api/contrat.yaml) :
 * exactement deux champs, jamais de stack trace.
 */
export interface ErreurApi {
  code: string;
  message: string;
}

/**
 * Codes d'erreur que les écrans peuvent afficher : ceux du contrat pour chaque
 * opération, plus les incidents réseau normalisés par la couche d'appel.
 * Ils servent à l'affichage, jamais à décider d'une règle métier (F3).
 */
export const CODES_ERREUR = {
  VALIDATION_INVALIDE: "VALIDATION_INVALIDE",
  REQUETE_ILLISIBLE: "REQUETE_ILLISIBLE",
  PROMOTION_INCONNUE: "PROMOTION_INCONNUE",
  DEMANDE_INVALIDE: "DEMANDE_INVALIDE",
  ERREUR_INTERNE: "ERREUR_INTERNE",
  ETUDIANT_INCONNU: "ETUDIANT_INCONNU",
  CODE_INCONNU: "CODE_INCONNU",
  CODE_EXPIRE: "CODE_EXPIRE",
  DEJA_PRESENT: "DEJA_PRESENT",
  TROP_DE_TENTATIVES: "TROP_DE_TENTATIVES",
  SESSION_INCONNUE: "SESSION_INCONNUE",
  LIEN_INVALIDE: "LIEN_INVALIDE",
  EXERCICE_DEJA_DEPOSE: "EXERCICE_DEJA_DEPOSE",
  SESSION_CLOTUREE: "SESSION_CLOTUREE",
  RELECTURE_INCONNUE: "RELECTURE_INCONNUE",
  APPELANT_NON_AUTORISE: "APPELANT_NON_AUTORISE",
  NOTE_INVALIDE: "NOTE_INVALIDE",
  AUTO_RELECTURE: "AUTO_RELECTURE",
  RELECTURE_DEJA_RENDUE: "RELECTURE_DEJA_RENDUE",
  SERVEUR_INJOIGNABLE: "SERVEUR_INJOIGNABLE",
  DELAI_DEPASSE: "DELAI_DEPASSE",
} as const;

export type CodeErreur = (typeof CODES_ERREUR)[keyof typeof CODES_ERREUR];

/** Erreur normalisée : tout appel API raté lève une ApiError, jamais une exception brute. */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(message: string, code: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

export type MethodeHttp = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface RequeteOptions {
  method?: MethodeHttp;
  params?: Record<string, string | number | boolean | undefined | null>;
  body?: unknown;
  entetes?: Record<string, string>;
  delaiMs?: number;
  signal?: AbortSignal;
}

/** Corps de POST /api/sessions — champs imposés par le contrat (EF1). */
export interface CreationSessionRequete {
  titre: string;
  promotionId: number;
}

/**
 * Réponse 201 de POST /api/sessions (EF1).
 * Les deux instants sont du RFC 3339 avec décalage (`2026-09-25T13:49:57.12+01:00`),
 * conformément au `format: date-time` du contrat.
 */
export interface SessionOuverteReponse {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

/** Origine d'une présence : jamais choisie par le client (D2). */
export type SourcePresence = "ETUDIANT" | "FORMATEUR";

/** Élément de GET /api/promotions/{promotionId}/etudiants (prérequis Q1). */
export interface EtudiantResume {
  id: number;
  prenom: string;
  nom: string;
}

/** Corps de POST /api/presences (EF2) — noms de champs imposés par le contrat. */
export interface MarquagePresenceRequete {
  code: string;
  etudiantId: number;
}

/** Réponse 201 de POST /api/presences (EF2). */
export interface PresenceReponse {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: SourcePresence;
}

/** Cycle de vie d'un exercice (D4) — toujours déterminé par le serveur. */
export type StatutExercice = "DEPOSE" | "EN_ATTENTE_RELECTURE" | "RELU";

/** Corps de POST /api/exercices (EF3) — noms de champs imposés par le contrat. */
export interface DepotExerciceRequete {
  sessionId: number;
  etudiantId: number;
  lien: string;
}

/** Réponse 201 de POST /api/exercices (EF3). */
export interface ExerciceDeposeReponse {
  id: number;
  statut: StatutExercice;
}

/**
 * État d'une relecture (D2) : `EN_ATTENTE` tant que la note n'est pas rendue,
 * `RENDUE` ensuite (EF6). Distinct de `StatutExercice`, où l'exercice passe à
 * `RELU`.
 */
export type StatutRelecture = "EN_ATTENTE" | "RENDUE";

/**
 * Élément de GET /api/relecteurs/{etudiantId}/relectures-en-attente (EF6).
 * L'identité de l'auteur n'y figure jamais (RG6).
 */
export interface MissionRelecteur {
  relectureId: number;
  exerciceId: number;
  sessionId: number;
  lien: string;
  statut: StatutRelecture;
}

/** Corps de POST /api/relectures/{id} (EF6) — noms de champs imposés par le contrat. */
export interface SoumissionRelectureRequete {
  note: number;
  commentaire: string;
}

/** Réponse 200 de POST /api/relectures/{id} (EF6). */
export interface RelectureRendueReponse {
  relectureId: number;
  exerciceId: number;
  note: number;
  commentaire: string;
  statut: StatutRelecture;
}

/**
 * Élément de GET /api/etudiants/{etudiantId}/relectures-recues (EF8).
 * L'identité du relecteur n'y figure jamais (RG6) ; `note` et `commentaire`
 * restent nuls tant que la relecture est `EN_ATTENTE` (RG9).
 */
export interface NoteRecue {
  exerciceId: number;
  statut: StatutRelecture;
  note: number | null;
  commentaire: string | null;
}
