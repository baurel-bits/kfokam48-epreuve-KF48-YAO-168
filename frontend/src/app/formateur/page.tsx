"use client";

import { useState } from "react";
import { ApiError } from "@/core/api/types";
import type { SessionOuverteReponse } from "@/core/api/types";
import { ouvrirSession } from "@/features/session/api/ouvrirSession";

/** Formate une date ISO de l'API pour l'affichage (aucun calcul métier). */
function formater(dateIso: string): string {
  return new Date(dateIso).toLocaleString("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

/** Écran formateur — portion de l'EF1 : ouvrir une session et obtenir son code. */
export default function EcranFormateur() {
  const [titre, setTitre] = useState("");
  const [promotionId, setPromotionId] = useState("");
  const [session, setSession] = useState<SessionOuverteReponse | null>(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  async function soumettre(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setChargement(true);
    setErreur(null);
    setSession(null);

    try {
      const ouverte = await ouvrirSession({
        titre,
        promotionId: Number(promotionId),
      });
      setSession(ouverte);
    } catch (echec) {
      setErreur(
        echec instanceof ApiError
          ? echec.message
          : "Une erreur est survenue à l'ouverture de la session.",
      );
    } finally {
      setChargement(false);
    }
  }

  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Espace formateur</h1>
        <p className="mt-1 text-sm text-slate-600">
          Ouvrez une session pour obtenir le code de présence à dicter aux
          étudiants.
        </p>
      </header>

      <form
        onSubmit={soumettre}
        className="space-y-4 rounded-lg border border-slate-200 bg-white p-5"
      >
        <div>
          <label htmlFor="titre" className="block text-sm font-medium">
            Titre de la session
          </label>
          <input
            id="titre"
            name="titre"
            required
            value={titre}
            onChange={(evenement) => setTitre(evenement.target.value)}
            placeholder="Cours du 25 septembre"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <div>
          <label htmlFor="promotionId" className="block text-sm font-medium">
            Identifiant de la promotion
          </label>
          <input
            id="promotionId"
            name="promotionId"
            type="number"
            min="1"
            required
            value={promotionId}
            onChange={(evenement) => setPromotionId(evenement.target.value)}
            placeholder="1"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
          <p className="mt-1 text-xs text-slate-500">
            La promotion de démonstration a l&apos;identifiant 1.
          </p>
        </div>

        <button
          type="submit"
          disabled={chargement}
          className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {chargement ? "Ouverture…" : "Ouvrir la session"}
        </button>
      </form>

      {chargement && (
        <p role="status" className="text-sm text-slate-600">
          Ouverture de la session en cours…
        </p>
      )}

      {erreur && (
        <p
          role="alert"
          className="rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          {erreur}
        </p>
      )}

      {session && (
        <div className="rounded-lg border border-emerald-300 bg-emerald-50 p-5">
          <h2 className="text-sm font-medium text-emerald-900">
            Session ouverte — code à dicter
          </h2>
          <p className="mt-2 font-mono text-3xl tracking-widest text-emerald-900">
            {session.code}
          </p>
          <dl className="mt-3 space-y-1 text-sm text-emerald-900">
            <div>
              <dt className="inline font-medium">Identifiant : </dt>
              <dd className="inline">{session.id}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Ouverte le : </dt>
              <dd className="inline">{formater(session.ouvertureAt)}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Expire le : </dt>
              <dd className="inline">{formater(session.expirationAt)}</dd>
            </div>
          </dl>
        </div>
      )}
    </section>
  );
}
