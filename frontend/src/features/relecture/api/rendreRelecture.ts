import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type {
  RelectureRendueReponse,
  SoumissionRelectureRequete,
} from "@/core/api/types";

/**
 * EF6 — `POST /api/relectures/{id}` (opération imposée par le contrat).
 *
 * La note est envoyée telle qu'elle a été saisie : c'est le serveur qui décide
 * si elle est valide (RG7) et qui fait évoluer les statuts. Le client ne
 * reproduit jamais cette règle (F3).
 */
export function rendreRelecture(
  relectureId: number,
  requete: SoumissionRelectureRequete,
): Promise<RelectureRendueReponse> {
  return api.post<RelectureRendueReponse>(
    ENDPOINTS.RELECTURES.RENDRE(relectureId),
    requete,
  );
}
