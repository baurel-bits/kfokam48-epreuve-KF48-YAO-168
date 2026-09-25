import type { SessionOuverteReponse } from "@/core/api/types";

/**
 * Session ouverte par le formateur, telle que conservée par son navigateur.
 *
 * Le contrat ne prévoit **aucune** opération qui relirait une session : le code
 * de présence n'existe que dans la réponse de `POST /api/sessions`. Sans cette
 * mémorisation, un rechargement de la page — ou une fermeture d'onglet — le
 * perdrait définitivement, alors qu'il reste valable quinze minutes (RG1).
 *
 * `cloturee` recopie une réponse du serveur (`POST /api/sessions/{id}/cloture`)
 * et n'est jamais déduit ici : l'écran ne fait qu'afficher un état qu'il a reçu.
 */
export interface SessionEnregistree extends SessionOuverteReponse {
  titre: string;
  promotionId: number;
  cloturee: boolean;
}

const CLE = "kfokam48:sessions-ouvertes";

/** Au-delà, la liste cesse d'être lisible ; seules les plus récentes sont gardées. */
const MAX_SESSIONS = 10;

/**
 * Le contenu du stockage local est modifiable par l'utilisateur, et une entrée
 * écrite par une version antérieure de l'application peut ne pas porter tous les
 * champs : une entrée inexploitable est écartée, jamais propagée jusqu'au rendu
 * où elle ferait planter l'écran.
 */
function normaliser(valeur: unknown): SessionEnregistree | null {
  if (typeof valeur !== "object" || valeur === null) {
    return null;
  }
  const session = valeur as Record<string, unknown>;
  const complete =
    typeof session.id === "number" &&
    typeof session.code === "string" &&
    typeof session.ouvertureAt === "string" &&
    typeof session.expirationAt === "string" &&
    typeof session.titre === "string" &&
    typeof session.promotionId === "number";

  if (!complete) {
    return null;
  }

  return {
    id: session.id as number,
    code: session.code as string,
    ouvertureAt: session.ouvertureAt as string,
    expirationAt: session.expirationAt as string,
    titre: session.titre as string,
    promotionId: session.promotionId as number,
    cloturee: session.cloturee === true,
  };
}

function ecrire(sessions: SessionEnregistree[]): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.setItem(CLE, JSON.stringify(sessions));
  } catch {
    // Écriture refusée : la session reste affichée, simplement non conservée.
  }
}

/** Sessions connues de ce navigateur, la plus récente d'abord. */
export function lireSessionsOuvertes(): SessionEnregistree[] {
  if (typeof window === "undefined") {
    return [];
  }

  try {
    const brut = window.localStorage.getItem(CLE);
    if (brut === null) {
      return [];
    }
    const analyse: unknown = JSON.parse(brut);
    if (!Array.isArray(analyse)) {
      return [];
    }
    return analyse
      .map(normaliser)
      .filter((session): session is SessionEnregistree => session !== null);
  } catch {
    // Stockage indisponible (navigation privée) ou contenu illisible : on repart
    // d'une liste vide plutôt que de casser l'écran.
    return [];
  }
}

/**
 * Ajoute une session en tête et renvoie la liste à jour. Ré-enregistrer le même
 * identifiant le remplace au lieu de le dupliquer.
 */
export function enregistrerSessionOuverte(
  session: SessionOuverteReponse,
  titre: string,
  promotionId: number,
): SessionEnregistree[] {
  const sessions = [
    { ...session, titre, promotionId, cloturee: false },
    ...lireSessionsOuvertes().filter((connue) => connue.id !== session.id),
  ].slice(0, MAX_SESSIONS);

  ecrire(sessions);
  return sessions;
}

/** Retient qu'une session a été clôturée, pour que l'état survive au rechargement. */
export function marquerSessionCloturee(sessionId: number): SessionEnregistree[] {
  const sessions = lireSessionsOuvertes().map((session) =>
    session.id === sessionId ? { ...session, cloturee: true } : session,
  );

  ecrire(sessions);
  return sessions;
}
