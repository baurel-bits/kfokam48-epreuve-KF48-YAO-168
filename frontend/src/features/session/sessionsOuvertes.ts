import type { SessionOuverteReponse } from "@/core/api/types";

/**
 * Session ouverte par le formateur, telle que conservée par son navigateur.
 *
 * Le contrat ne prévoit **aucune** opération qui relirait une session : le code
 * de présence n'existe que dans la réponse de `POST /api/sessions`. Sans cette
 * mémorisation, un rechargement de la page — ou une fermeture d'onglet — le
 * perdrait définitivement, alors qu'il reste valable quinze minutes (RG1).
 */
export interface SessionEnregistree extends SessionOuverteReponse {
  titre: string;
  promotionId: number;
}

const CLE = "kfokam48:sessions-ouvertes";

/** Au-delà, la liste cesse d'être lisible ; seules les plus récentes sont gardées. */
const MAX_SESSIONS = 10;

/**
 * Le contenu du stockage local est modifiable par l'utilisateur : une entrée
 * abîmée doit être ignorée, jamais propagée jusqu'au rendu où elle ferait
 * planter l'écran.
 */
function estSessionEnregistree(valeur: unknown): valeur is SessionEnregistree {
  if (typeof valeur !== "object" || valeur === null) {
    return false;
  }
  const session = valeur as Record<string, unknown>;
  return (
    typeof session.id === "number" &&
    typeof session.code === "string" &&
    typeof session.ouvertureAt === "string" &&
    typeof session.expirationAt === "string" &&
    typeof session.titre === "string" &&
    typeof session.promotionId === "number"
  );
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
    return Array.isArray(analyse) ? analyse.filter(estSessionEnregistree) : [];
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
    { ...session, titre, promotionId },
    ...lireSessionsOuvertes().filter((connue) => connue.id !== session.id),
  ].slice(0, MAX_SESSIONS);

  if (typeof window !== "undefined") {
    try {
      window.localStorage.setItem(CLE, JSON.stringify(sessions));
    } catch {
      // Écriture refusée : la session reste affichée, simplement non conservée.
    }
  }

  return sessions;
}
