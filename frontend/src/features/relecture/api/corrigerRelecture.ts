import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type {
  RelectureRendueReponse,
  SoumissionRelectureRequete,
} from "@/core/api/types";

/**
 * EF7 — `PUT /api/relectures/{id}/correction`.
 *
 * Remplace la note d'une relecture **déjà rendue**, tant que la session n'est pas
 * clôturée (RG8). Le serveur archive la note remplacée dans l'historique : rien
 * n'est effacé, et le client n'a aucune règle à reproduire (F3).
 */
export function corrigerRelecture(
  relectureId: number,
  requete: SoumissionRelectureRequete,
): Promise<RelectureRendueReponse> {
  return api.put<RelectureRendueReponse>(
    ENDPOINTS.RELECTURES.CORRIGER(relectureId),
    requete,
  );
}
