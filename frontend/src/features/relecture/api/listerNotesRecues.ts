import { api } from "@/core/api/client";
import { ENDPOINTS } from "@/core/api/endpoints";
import type { NoteRecue } from "@/core/api/types";

/**
 * EF8 — `GET /api/etudiants/{etudiantId}/relectures-recues`.
 *
 * Le serveur décide seul de ce qui est visible : il renvoie la note et le
 * commentaire quand la relecture est rendue, et rien d'autre qu'un statut
 * d'attente sinon (RG9). L'identité du relecteur n'est jamais transmise (RG6),
 * donc rien ne peut la laisser fuiter à l'écran.
 */
export function listerNotesRecues(etudiantId: number): Promise<NoteRecue[]> {
  return api.get<NoteRecue[]>(
    ENDPOINTS.ETUDIANTS.RELECTURES_RECUES(etudiantId),
  );
}
