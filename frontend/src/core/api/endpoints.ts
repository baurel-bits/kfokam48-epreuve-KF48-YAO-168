export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Chemins du contrat d'API (api/contrat.yaml), centralisés : aucun chemin
 * n'est écrit directement dans un composant (F3).
 */
export const ENDPOINTS = {
  /** EF1 — le formateur ouvre une session et obtient un code de présence. */
  SESSIONS: {
    OUVRIR: "/api/sessions",
  },
} as const;
