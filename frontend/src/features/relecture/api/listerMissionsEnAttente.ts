import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { MissionRelecteur } from "@/core/api/types";

/**
 * EF6 — `GET /api/relecteurs/{etudiantId}/relectures-en-attente`.
 *
 * Le serveur ne renvoie que les relectures à rendre, dans l'ordre d'assignation,
 * et jamais l'identité de l'auteur (RG6).
 */
export function listerMissionsEnAttente(
  etudiantId: number,
): Promise<MissionRelecteur[]> {
  return api.get<MissionRelecteur[]>(
    ENDPOINTS.RELECTEURS.MISSIONS_EN_ATTENTE(etudiantId),
  );
}
