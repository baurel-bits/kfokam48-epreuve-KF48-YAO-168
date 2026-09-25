import { API_BASE_URL } from "./endpoints";
import { ApiError, CODES_ERREUR, type ErreurApi, type RequeteOptions } from "./types";

const DELAI_MAX_MS = 10_000;

function construireUrl(chemin: string, params?: RequeteOptions["params"]): string {
  const url = `${API_BASE_URL}${chemin}`;
  const recherche = new URLSearchParams();
  Object.entries(params ?? {}).forEach(([cle, valeur]) => {
    if (valeur !== undefined && valeur !== null) {
      recherche.append(cle, String(valeur));
    }
  });
  const requete = recherche.toString();
  return requete ? `${url}?${requete}` : url;
}

/**
 * Convertit une réponse HTTP en échec normalisé : on relit le corps
 * `{ code, message }` du contrat ; à défaut (page d'erreur du serveur),
 * on retombe sur un message générique sans jamais exposer de détail technique.
 */
async function construireErreur(reponse: Response): Promise<ApiError> {
  try {
    const corps = (await reponse.json()) as Partial<ErreurApi>;
    if (typeof corps.code === "string" && typeof corps.message === "string") {
      return new ApiError(corps.message, corps.code, reponse.status);
    }
  } catch {
    // corps non JSON : ignoré volontairement
  }
  const serveur = reponse.status >= 500;
  return new ApiError(
    serveur
      ? "Une erreur interne est survenue côté serveur."
      : `La requête a été refusée par le serveur (HTTP ${reponse.status}).`,
    serveur ? CODES_ERREUR.ERREUR_INTERNE : CODES_ERREUR.DEMANDE_INVALIDE,
    reponse.status,
  );
}

/** Appel HTTP unique de l'application : chemin + options -> données typées ou ApiError. */
export async function appelApi<T>(chemin: string, options: RequeteOptions = {}): Promise<T> {
  const { method = "GET", params, body, entetes, delaiMs = DELAI_MAX_MS, signal } = options;
  const controleur = new AbortController();
  const minuteur = setTimeout(() => controleur.abort(), delaiMs);

  try {
    const reponse = await fetch(construireUrl(chemin, params), {
      method,
      signal: signal ?? controleur.signal,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...entetes,
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (!reponse.ok) {
      throw await construireErreur(reponse);
    }

    const texte = await reponse.text();
    return (texte === "" ? undefined : JSON.parse(texte)) as T;
  } catch (erreur) {
    if (erreur instanceof ApiError) {
      throw erreur;
    }
    if (erreur instanceof DOMException && erreur.name === "AbortError") {
      throw new ApiError(
        "Le serveur n'a pas répondu dans le délai imparti.",
        CODES_ERREUR.DELAI_DEPASSE,
        0,
      );
    }
    throw new ApiError(
      "Le serveur est injoignable. Vérifiez que le backend est démarré.",
      CODES_ERREUR.SERVEUR_INJOIGNABLE,
      0,
    );
  } finally {
    clearTimeout(minuteur);
  }
}

export const api = {
  get: <T>(chemin: string, options?: Omit<RequeteOptions, "body">) =>
    appelApi<T>(chemin, { ...options, method: "GET" }),
  post: <T>(chemin: string, body?: unknown, options?: Omit<RequeteOptions, "body">) =>
    appelApi<T>(chemin, { ...options, method: "POST", body }),
  put: <T>(chemin: string, body?: unknown, options?: Omit<RequeteOptions, "body">) =>
    appelApi<T>(chemin, { ...options, method: "PUT", body }),
};
