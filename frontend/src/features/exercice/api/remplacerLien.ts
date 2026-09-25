import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type {
  ExerciceDeposeReponse,
  RemplacementLienRequete,
} from "@/core/api/types";

/**
 * EF4 — `PUT /api/exercices/{id}/lien`.
 *
 * Remplace le lien d'un exercice déjà déposé, tant qu'aucune relecture n'a été
 * commencée dessus (RG11) et que la session n'est pas clôturée (RG14). Le lien
 * est envoyé tel quel : c'est le serveur qui juge son format (`400
 * LIEN_INVALIDE`) et qui décide si le remplacement est encore permis (F3).
 */
export function remplacerLien(
  exerciceId: number,
  requete: RemplacementLienRequete,
): Promise<ExerciceDeposeReponse> {
  return api.put<ExerciceDeposeReponse>(
    ENDPOINTS.EXERCICES.REMPLACER_LIEN(exerciceId),
    requete,
  );
}
