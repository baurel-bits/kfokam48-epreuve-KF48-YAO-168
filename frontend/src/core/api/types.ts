/**
 * Format d'erreur imposé par le contrat (api/contrat.yaml) :
 * exactement deux champs, jamais de stack trace.
 */
export interface ErreurApi {
  code: string;
  message: string;
}

/** Codes que l'écran de l'EF1 peut recevoir (contrat + incidents réseau). */
export const CODES_ERREUR = {
  VALIDATION_INVALIDE: "VALIDATION_INVALIDE",
  REQUETE_ILLISIBLE: "REQUETE_ILLISIBLE",
  PROMOTION_INCONNUE: "PROMOTION_INCONNUE",
  DEMANDE_INVALIDE: "DEMANDE_INVALIDE",
  ERREUR_INTERNE: "ERREUR_INTERNE",
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
