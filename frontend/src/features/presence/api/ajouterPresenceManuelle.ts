import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { AjoutPresenceManuelleRequete, PresenceReponse } from "@/core/api/types";

/**
 * EF10 — `POST /api/presences/manuelles`.
 *
 * Le formateur ajoute une présence sans code : elle est enregistrée avec
 * `source = FORMATEUR`, donc distinguable de celles que les étudiants marquent
 * eux-mêmes (RG12). Le serveur répond `201`, refuse un doublon en `409
 * DEJA_PRESENT` et une référence inconnue en `404`.
 *
 * La source n'est pas transmise : c'est le serveur qui la décide.
 */
export function ajouterPresenceManuelle(
  requete: AjoutPresenceManuelleRequete,
): Promise<PresenceReponse> {
  return api.post<PresenceReponse>(ENDPOINTS.PRESENCES.MARQUER_MANUELLE, requete);
}
