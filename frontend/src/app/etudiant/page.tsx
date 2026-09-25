"use client";

import { useState } from "react";
import { ApiError } from "@/core/api/types";
import type { EtudiantResume, ExerciceDeposeReponse, PresenceReponse } from "@/core/api/types";
import { deposerExercice } from "@/features/exercice/api/deposerExercice";
import { marquerPresence } from "@/features/presence/api/marquerPresence";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";

/** Étape de l'écran à laquelle rattacher une erreur. */
type Etape = "etudiants" | "presence" | "depot";

/** Forme affichable d'une erreur : le `code` imposé par le contrat + son message. */
interface ErreurAffichee {
  etape: Etape;
  code: string;
  message: string;
}

function versErreur(echec: unknown, etape: Etape, messageParDefaut: string): ErreurAffichee {
  if (echec instanceof ApiError) {
    return { etape, code: echec.code, message: echec.message };
  }
  return { etape, code: "ERREUR_INCONNUE", message: messageParDefaut };
}

function BlocErreur({ erreur }: { erreur: ErreurAffichee }) {
  return (
    <div
      role="alert"
      className="rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800"
    >
      <p className="font-mono text-xs uppercase">{erreur.code}</p>
      <p className="mt-1">{erreur.message}</p>
    </div>
  );
}

/**
 * Écran étudiant — EF2 (marquer sa présence) et EF3 (déposer son exercice).
 * Vue mobile en priorité (ENF1) : une colonne, aucune largeur fixe, aucun
 * défilement horizontal. Aucune règle métier n'est recalculée ici (F3) :
 * format du lien, expiration du code, blocage RG3 et statut de l'exercice
 * viennent tous du serveur.
 */
export default function EcranEtudiant() {
  const [promotionId, setPromotionId] = useState("1");
  const [etudiants, setEtudiants] = useState<EtudiantResume[]>([]);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  const [code, setCode] = useState("");
  const [presence, setPresence] = useState<PresenceReponse | null>(null);
  const [chargementPresence, setChargementPresence] = useState(false);

  const [sessionId, setSessionId] = useState("");
  const [lien, setLien] = useState("");
  const [exercice, setExercice] = useState<ExerciceDeposeReponse | null>(null);
  const [chargementDepot, setChargementDepot] = useState(false);

  const [chargementListe, setChargementListe] = useState(false);
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
      setErreur(versErreur(echec, "etudiants", "Impossible de charger la liste des étudiants."));
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
    setExercice(null);

    try {
      const enregistree = await marquerPresence({ code: code.trim(), etudiantId });
      setPresence(enregistree);
      // L'identifiant de session vient du serveur : on le réutilise pour le
      // dépôt au lieu de le faire ressaisir à l'étudiant.
      setSessionId(String(enregistree.sessionId));
      setCode("");
    } catch (echec) {
      setErreur(versErreur(echec, "presence", "La présence n'a pas pu être enregistrée."));
    } finally {
      setChargementPresence(false);
    }
  }

  async function deposerLeLien(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (etudiantId === null || chargementDepot) {
      return;
    }

    setChargementDepot(true);
    setErreur(null);
    setExercice(null);

    try {
      setExercice(
        await deposerExercice({
          sessionId: Number(sessionId),
          etudiantId,
          lien: lien.trim(),
        }),
      );
      setLien("");
    } catch (echec) {
      setErreur(versErreur(echec, "depot", "Le dépôt n'a pas pu être enregistré."));
    } finally {
      setChargementDepot(false);
    }
  }

  return (
    <section className="mx-auto flex w-full max-w-sm flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold">Espace étudiant</h1>
        <p className="mt-1 text-sm text-slate-600">
          Choisissez votre nom, saisissez le code dicté par le formateur, puis
          déposez le lien de votre exercice.
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

        {etudiants.length === 0 && !chargementListe && erreur?.etape !== "etudiants" && (
          <p className="text-sm text-slate-500">Aucun étudiant affiché pour l&apos;instant.</p>
        )}

        {erreur?.etape === "etudiants" && <BlocErreur erreur={erreur} />}
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

        {presence && (
          <div className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
            <p className="font-medium">Présence enregistrée.</p>
            <p className="mt-1">
              Session n°{presence.sessionId} — source : {presence.source}
            </p>
          </div>
        )}

        {erreur?.etape === "presence" && <BlocErreur erreur={erreur} />}
      </form>

      <form
        onSubmit={deposerLeLien}
        className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-medium">3. Déposer mon exercice</h2>

        <div>
          <label htmlFor="sessionId" className="block text-sm text-slate-600">
            Session
          </label>
          <input
            id="sessionId"
            type="number"
            min="1"
            required
            value={sessionId}
            onChange={(evenement) => setSessionId(evenement.target.value)}
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
          <p className="mt-1 text-xs text-slate-500">
            Renseigné automatiquement après avoir marqué votre présence.
          </p>
        </div>

        <div>
          <label htmlFor="lien" className="block text-sm text-slate-600">
            Lien de l&apos;exercice
          </label>
          <input
            id="lien"
            name="lien"
            type="url"
            required
            autoComplete="off"
            spellCheck={false}
            value={lien}
            onChange={(evenement) => setLien(evenement.target.value)}
            placeholder="https://…"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={chargementDepot || etudiantId === null}
          className="w-full rounded bg-slate-900 px-4 py-3 text-sm font-medium text-white disabled:opacity-50"
        >
          {chargementDepot ? "Dépôt…" : "Déposer mon exercice"}
        </button>

        <p className="text-xs text-slate-500">
          Le dépôt reste possible après l&apos;expiration du code, tant que le
          formateur n&apos;a pas clôturé la session.
        </p>

        {exercice && (
          <div className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
            <p className="font-medium">Exercice déposé.</p>
            <p className="mt-1">
              Exercice n°{exercice.id} — statut : {exercice.statut}
            </p>
          </div>
        )}

        {erreur?.etape === "depot" && <BlocErreur erreur={erreur} />}
      </form>
    </section>
  );
}
