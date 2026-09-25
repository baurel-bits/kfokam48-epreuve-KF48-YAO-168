import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { EtudiantResume } from "@/core/api/types";

/**
 * Prérequis Q1 — `GET /api/promotions/{promotionId}/etudiants`.
 *
 * Le sujet exclut l'authentification : l'étudiant se choisit dans cette liste.
 * Réutilisé par l'écran étudiant (EF2, EF3) et l'écran formateur (EF9, EF10).
 */
export function listerEtudiants(promotionId: number): Promise<EtudiantResume[]> {
  return api.get<EtudiantResume[]>(ENDPOINTS.PROMOTIONS.ETUDIANTS(promotionId));
}
