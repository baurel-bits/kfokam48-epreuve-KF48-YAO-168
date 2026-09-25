import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { DepotExerciceRequete, ExerciceDeposeReponse } from "@/core/api/types";

/**
 * EF3 — l'étudiant dépose le lien de son exercice
 * (`POST /api/exercices`, contrat api/contrat.yaml).
 *
 * Le statut renvoyé (`DEPOSE`) vient exclusivement du serveur : le client ne
 * décide ni de la validité du lien ni du cycle de vie de l'exercice (F3).
 */
export function deposerExercice(
  requete: DepotExerciceRequete,
): Promise<ExerciceDeposeReponse> {
  return api.post<ExerciceDeposeReponse>(ENDPOINTS.EXERCICES.DEPOSER, requete);
}
