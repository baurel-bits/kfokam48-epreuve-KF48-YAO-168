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
    /** EF11 — clôture définitive : dépôts et notes de la session sont gelés (RG14). */
    CLOTURER: (sessionId: number) => `/api/sessions/${sessionId}/cloture`,
  },

  /** EF2 — l'étudiant marque sa présence avec le code dicté par le formateur. */
  PRESENCES: {
    MARQUER: "/api/presences",
    /** EF10 — le formateur ajoute une présence (source FORMATEUR, RG12). */
    MARQUER_MANUELLE: "/api/presences/manuelles",
  },

  /** EF3 — l'étudiant dépose le lien de son exercice. */
  EXERCICES: {
    DEPOSER: "/api/exercices",
  },

  /**
   * Prérequis Q1 — l'étudiant est choisi dans une liste, le sujet excluant
   * l'authentification. Alimente les écrans étudiant (EF2, EF3) et formateur
   * (EF9, EF10).
   */
  PROMOTIONS: {
    ETUDIANTS: (promotionId: number) => `/api/promotions/${promotionId}/etudiants`,
  },

  /** EF6 — le relecteur rend sa note et son commentaire (opération imposée). */
  RELECTURES: {
    RENDRE: (relectureId: number) => `/api/relectures/${relectureId}`,
  },

  /** EF6 — relectures assignées à un relecteur et non encore rendues. */
  RELECTEURS: {
    MISSIONS_EN_ATTENTE: (etudiantId: number) =>
      `/api/relecteurs/${etudiantId}/relectures-en-attente`,
  },

  /** EF8 — notes reçues par un étudiant relu (jamais l'identité du relecteur). */
  ETUDIANTS: {
    RELECTURES_RECUES: (etudiantId: number) =>
      `/api/etudiants/${etudiantId}/relectures-recues`,
  },

  /**
   * EF9 — tableau de bord du formateur (opération imposée). `promotionId` est
   * obligatoire et transmis en paramètre de requête par la couche d'appel.
   */
  TABLEAU: {
    CONSULTER: "/api/tableau",
  },
} as const;
