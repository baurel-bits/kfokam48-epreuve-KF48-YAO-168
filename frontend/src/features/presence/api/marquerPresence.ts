import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { MarquagePresenceRequete, PresenceReponse } from "@/core/api/types";

/**
 * EF2 — l'étudiant marque sa présence avec le code de la session
 * (`POST /api/presences`, contrat api/contrat.yaml).
 *
 * Aucune règle métier n'est appliquée ici : le code est transmis tel quel et
 * validé uniquement par le serveur (expiration RG1, doublon RG2, blocage RG3).
 * Un échec lève une `ApiError` portant le `code` imposé par le contrat.
 */
export function marquerPresence(
  requete: MarquagePresenceRequete,
): Promise<PresenceReponse> {
  return api.post<PresenceReponse>(ENDPOINTS.PRESENCES.MARQUER, requete);
}
