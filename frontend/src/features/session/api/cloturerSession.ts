import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { SessionClotureeReponse } from "@/core/api/types";

/**
 * EF11 — `POST /api/sessions/{id}/cloture`.
 *
 * Clôture définitive : le serveur refuse ensuite tout dépôt d'exercice et toute
 * note sur cette session (RG14), sans détruire ce qui a déjà été enregistré. Les
 * relectures encore en attente le restent.
 *
 * L'opération n'a pas de corps et répond `200` (et non `201` : la session
 * préexistait à sa clôture).
 */
export function cloturerSession(sessionId: number): Promise<SessionClotureeReponse> {
  return api.post<SessionClotureeReponse>(ENDPOINTS.SESSIONS.CLOTURER(sessionId));
}
