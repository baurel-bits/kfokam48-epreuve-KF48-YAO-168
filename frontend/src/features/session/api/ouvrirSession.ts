import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { CreationSessionRequete, SessionOuverteReponse } from "@/core/api/types";

/**
 * EF1 — ouvre une session de présence et renvoie son code
 * (`POST /api/sessions`, contrat api/contrat.yaml).
 *
 * Seul point d'entrée de cet appel : l'écran formateur ne connaît ni l'URL
 * ni le verbe, et ne calcule rien lui-même (F3).
 */
export function ouvrirSession(
  requete: CreationSessionRequete,
): Promise<SessionOuverteReponse> {
  return api.post<SessionOuverteReponse>(ENDPOINTS.SESSIONS.OUVRIR, requete);
}
