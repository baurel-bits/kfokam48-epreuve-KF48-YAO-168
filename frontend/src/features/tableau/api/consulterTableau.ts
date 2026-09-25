import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { LigneTableau } from "@/core/api/types";

/**
 * EF9 — `GET /api/tableau?promotionId=` (opération imposée du contrat).
 *
 * Renvoie une ligne par étudiant de la promotion, y compris ceux qui n'ont
 * aucune activité. Compteurs et moyennes sont calculés par le serveur : cette
 * couche ne fait que transporter la réponse, sans la retoucher (F3).
 */
export function consulterTableau(promotionId: number): Promise<LigneTableau[]> {
  return api.get<LigneTableau[]>(ENDPOINTS.TABLEAU.CONSULTER, {
    params: { promotionId },
  });
}
