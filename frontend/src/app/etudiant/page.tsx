"use client";

import { useState } from "react";
import { ApiError } from "@/core/api/types";
import type { EtudiantResume, PresenceReponse } from "@/core/api/types";
import { marquerPresence } from "@/features/presence/api/marquerPresence";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";

/** Forme affichable d'une erreur : le `code` imposé par le contrat + son message. */
interface ErreurAffichee {
  code: string;
  message: string;
}

function versErreur(echec: unknown, messageParDefaut: string): ErreurAffichee {
  if (echec instanceof ApiError) {
    return { code: echec.code, message: echec.message };
  }
  return { code: "ERREUR_INCONNUE", message: messageParDefaut };
}

/**
 * Écran étudiant — EF2 : marquer sa présence avec le code de la session.
 * Vue mobile en priorité (ENF1) : une colonne, aucune largeur fixe, aucun
 * défilement horizontal. Aucune règle métier n'est recalculée ici (F3) :
 * la validité du code, l'expiration et le blocage RG3 viennent du serveur.
 */
export default function EcranEtudiant() {
  const [promotionId, setPromotionId] = useState("1");
  const [etudiants, setEtudiants] = useState<EtudiantResume[]>([]);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [code, setCode] = useState("");
  const [presence, setPresence] = useState<PresenceReponse | null>(null);
  const [chargementListe, setChargementListe] = useState(false);
  const [chargementPresence, setChargementPresence] = useState(false);
  const [erreur, setErreur] = useState<ErreurAffichee | null>(null);

  async function chargerLesEtudiants(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setChargementListe(true);
    setErreur(null);
    setPresence(null);
    setEtudiants([]);
    setEtudiantId(null);

    try {
      setEtudiants(await listerEtudiants(Number(promotionId)));
    } catch (echec) {
      setErreur(versErreur(echec, "Impossible de charger la liste des étudiants."));
    } finally {
      setChargementListe(false);
    }
  }

  async function soumettreLeCode(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (etudiantId === null || chargementPresence) {
      return;
    }

    setChargementPresence(true);
    setErreur(null);
    setPresence(null);

    try {
      setPresence(await marquerPresence({ code: code.trim(), etudiantId }));
      setCode("");
    } catch (echec) {
      setErreur(versErreur(echec, "La présence n'a pas pu être enregistrée."));
    } finally {
      setChargementPresence(false);
    }
  }

  return (
    <section className="mx-auto flex w-full max-w-sm flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold">Espace étudiant</h1>
        <p className="mt-1 text-sm text-slate-600">
          Choisissez votre nom, puis saisissez le code dicté par le formateur.
        </p>
      </header>

      <form
        onSubmit={chargerLesEtudiants}
        className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-medium">1. Qui êtes-vous ?</h2>

        <div>
          <label htmlFor="promotionId" className="block text-sm text-slate-600">
            Promotion
          </label>
          <div className="mt-1 flex gap-2">
            <input
              id="promotionId"
              type="number"
              min="1"
              required
              value={promotionId}
              onChange={(evenement) => setPromotionId(evenement.target.value)}
              className="w-24 rounded border border-slate-300 px-3 py-2 text-sm"
            />
            <button
              type="submit"
              disabled={chargementListe}
              className="flex-1 rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              {chargementListe ? "Chargement…" : "Afficher les étudiants"}
            </button>
          </div>
        </div>

        {chargementListe && (
          <p role="status" className="text-sm text-slate-600">
            Chargement de la liste…
          </p>
        )}

        {etudiants.length > 0 && (
          <ul className="flex flex-col gap-2">
            {etudiants.map((etudiant) => {
              const selectionne = etudiant.id === etudiantId;
              return (
                <li key={etudiant.id}>
                  <button
                    type="button"
                    aria-pressed={selectionne}
                    onClick={() => setEtudiantId(etudiant.id)}
                    className={`w-full rounded border px-3 py-3 text-left text-sm ${
                      selectionne
                        ? "border-slate-900 bg-slate-900 text-white"
                        : "border-slate-300 bg-white"
                    }`}
                  >
                    {etudiant.prenom} {etudiant.nom}
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        {etudiants.length === 0 && !chargementListe && !erreur && (
          <p className="text-sm text-slate-500">
            Aucun étudiant affiché pour l&apos;instant.
          </p>
        )}
      </form>

      <form
        onSubmit={soumettreLeCode}
        className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-medium">2. Code de présence</h2>

        <div>
          <label htmlFor="code" className="block text-sm text-slate-600">
            Code dicté par le formateur
          </label>
          <input
            id="code"
            name="code"
            required
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            value={code}
            onChange={(evenement) => setCode(evenement.target.value)}
            placeholder="ABC234"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-3 font-mono text-center text-xl tracking-widest"
          />
        </div>

        <button
          type="submit"
          disabled={chargementPresence || etudiantId === null}
          className="w-full rounded bg-slate-900 px-4 py-3 text-sm font-medium text-white disabled:opacity-50"
        >
          {chargementPresence ? "Enregistrement…" : "Marquer ma présence"}
        </button>

        {etudiantId === null && (
          <p className="text-xs text-slate-500">
            Choisissez d&apos;abord votre nom à l&apos;étape 1.
          </p>
        )}
      </form>

      {erreur && (
        <div
          role="alert"
          className="rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          <p className="font-mono text-xs uppercase">{erreur.code}</p>
          <p className="mt-1">{erreur.message}</p>
        </div>
      )}

      {presence && (
        <div className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
          <p className="font-medium">Présence enregistrée.</p>
          <p className="mt-1">
            Session n°{presence.sessionId} — source : {presence.source}
          </p>
        </div>
      )}
    </section>
  );
}
