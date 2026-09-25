"use client";

import { useState } from "react";
import { ApiError } from "@/core/api/types";
import type {
  EtudiantResume,
  MissionRelecteur,
  RelectureRendueReponse,
} from "@/core/api/types";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";
import { corrigerRelecture } from "@/features/relecture/api/corrigerRelecture";
import { listerMissionsEnAttente } from "@/features/relecture/api/listerMissionsEnAttente";
import { rendreRelecture } from "@/features/relecture/api/rendreRelecture";

/** Étape de l'écran à laquelle rattacher une erreur. */
type Etape = "etudiants" | "missions" | "note" | "correction";

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
 * Écran relecteur — EF6 : choisir son nom, retrouver l'exercice qui lui est
 * confié, rendre sa note et son commentaire ; EF7 : corriger cette note tant que
 * la session n'est pas clôturée.
 *
 * Vue mobile en priorité (ENF1), et aucune règle métier recalculée ici (F3) : la
 * validité de la note (RG7), le passage de la relecture à `RENDUE`, celui de
 * l'exercice à `RELU` et le gel de la correction (RG8) sont décidés par le
 * serveur.
 */
export default function EcranRelecteur() {
  const [promotionId, setPromotionId] = useState("1");
  const [etudiants, setEtudiants] = useState<EtudiantResume[]>([]);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [missions, setMissions] = useState<MissionRelecteur[]>([]);
  const [missionId, setMissionId] = useState<number | null>(null);
  const [note, setNote] = useState("");
  const [commentaire, setCommentaire] = useState("");
  const [rendue, setRendue] = useState<RelectureRendueReponse | null>(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<ErreurAffichee | null>(null);

  // EF7 — correction de la relecture qui vient d'être rendue. Le serveur seul
  // décide si c'est encore permis (RG8) : cet état ne fait qu'afficher le
  // formulaire et son résultat.
  const [correctionOuverte, setCorrectionOuverte] = useState(false);
  const [noteCorrigee, setNoteCorrigee] = useState("");
  const [commentaireCorrige, setCommentaireCorrige] = useState("");
  const [corrigee, setCorrigee] = useState<RelectureRendueReponse | null>(null);
  const [chargementCorrection, setChargementCorrection] = useState(false);

  async function chargerLesEtudiants(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setChargement(true);
    setErreur(null);
    setEtudiants([]);
    setEtudiantId(null);
    setMissions([]);
    setMissionId(null);
    setRendue(null);

    try {
      setEtudiants(await listerEtudiants(Number(promotionId)));
    } catch (echec) {
      setErreur(versErreur(echec, "etudiants", "Impossible de charger la liste des étudiants."));
    } finally {
      setChargement(false);
    }
  }

  async function choisirEtudiant(id: number) {
    setEtudiantId(id);
    setMissions([]);
    setMissionId(null);
    setRendue(null);
    setErreur(null);

    try {
      setMissions(await listerMissionsEnAttente(id));
    } catch (echec) {
      setErreur(versErreur(echec, "missions", "Impossible de charger les relectures assignées."));
    }
  }

  async function rafraichirLesMissions(id: number) {
    try {
      setMissions(await listerMissionsEnAttente(id));
    } catch (echec) {
      setErreur(versErreur(echec, "missions", "Impossible de recharger les relectures assignées."));
    }
  }

  async function soumettreLaNote(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (missionId === null || etudiantId === null || chargement) {
      return;
    }

    setChargement(true);
    setErreur(null);
    setRendue(null);

    try {
      const reponse = await rendreRelecture(missionId, {
        note: Number(note),
        commentaire,
      });
      setRendue(reponse);
      setNote("");
      setCommentaire("");
      setMissionId(null);
      // Une nouvelle relecture rendue : une éventuelle correction précédente ne
      // concerne plus ce formulaire.
      fermerLaCorrection();
      await rafraichirLesMissions(etudiantId);
    } catch (echec) {
      setErreur(versErreur(echec, "note", "La note n'a pas pu être enregistrée."));
    } finally {
      setChargement(false);
    }
  }

  /**
   * Ouvre la correction en repartant de la note rendue : le relecteur corrige ce
   * qu'il a écrit, il ne le ressaisit pas. Les valeurs viennent de la réponse du
   * serveur, jamais d'un calcul local.
   */
  function ouvrirLaCorrection() {
    if (rendue === null) {
      return;
    }
    setNoteCorrigee(String(rendue.note));
    setCommentaireCorrige(rendue.commentaire);
    setCorrigee(null);
    setErreur(null);
    setCorrectionOuverte(true);
  }

  function fermerLaCorrection() {
    setCorrectionOuverte(false);
    setNoteCorrigee("");
    setCommentaireCorrige("");
    setCorrigee(null);
  }

  async function soumettreLaCorrection(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (rendue === null || chargementCorrection) {
      return;
    }

    setChargementCorrection(true);
    setErreur(null);
    setCorrigee(null);

    try {
      const reponse = await corrigerRelecture(rendue.relectureId, {
        note: Number(noteCorrigee),
        commentaire: commentaireCorrige,
      });
      // La note affichée à l'étape 3 devient celle que le serveur vient
      // d'enregistrer : l'écran ne garde pas de version périmée.
      setRendue(reponse);
      setCorrigee(reponse);
    } catch (echec) {
      setErreur(versErreur(echec, "correction", "La correction n'a pas pu être enregistrée."));
    } finally {
      setChargementCorrection(false);
    }
  }

  const missionChoisie = missions.find((mission) => mission.relectureId === missionId) ?? null;

  return (
    <section className="mx-auto flex w-full max-w-sm flex-col gap-6">
      <header>
        <h1 className="text-2xl font-semibold">Espace relecteur</h1>
        <p className="mt-1 text-sm text-slate-600">
          Choisissez votre nom, ouvrez l&apos;exercice qui vous est confié, puis
          rendez votre note et votre commentaire.
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
              disabled={chargement}
              className="flex-1 rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              {chargement ? "Chargement…" : "Afficher les étudiants"}
            </button>
          </div>
        </div>

        {etudiants.length > 0 && (
          <ul aria-label="Étudiants de la promotion" className="flex flex-col gap-2">
            {etudiants.map((etudiant) => {
              const selectionne = etudiant.id === etudiantId;
              return (
                <li key={etudiant.id}>
                  <button
                    type="button"
                    aria-pressed={selectionne}
                    onClick={() => choisirEtudiant(etudiant.id)}
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

        {etudiants.length === 0 && !chargement && erreur?.etape !== "etudiants" && (
          <p className="text-sm text-slate-500">Aucun étudiant affiché pour l&apos;instant.</p>
        )}

        {erreur?.etape === "etudiants" && <BlocErreur erreur={erreur} />}
      </form>

      <section className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="text-sm font-medium">2. Exercice à relire</h2>

        {etudiantId === null && (
          <p className="text-sm text-slate-500">
            Choisissez d&apos;abord votre nom à l&apos;étape 1.
          </p>
        )}

        {etudiantId !== null && missions.length === 0 && erreur?.etape !== "missions" && (
          <p className="text-sm text-slate-500">
            Aucun exercice ne vous est confié pour le moment.
          </p>
        )}

        {missions.length > 0 && (
          <ul aria-label="Exercices à relire" className="flex flex-col gap-2">
            {missions.map((mission) => {
              const selectionnee = mission.relectureId === missionId;
              return (
                <li key={mission.relectureId} className="flex flex-col gap-1">
                  <button
                    type="button"
                    aria-pressed={selectionnee}
                    onClick={() => setMissionId(mission.relectureId)}
                    className={`w-full rounded border px-3 py-3 text-left text-sm ${
                      selectionnee
                        ? "border-slate-900 bg-slate-900 text-white"
                        : "border-slate-300 bg-white"
                    }`}
                  >
                    Exercice n°{mission.exerciceId} — session n°{mission.sessionId}
                    <span className="mt-1 block text-xs opacity-80">
                      {mission.statut}
                    </span>
                  </button>
                  <a
                    href={mission.lien}
                    target="_blank"
                    rel="noreferrer"
                    className="px-1 text-xs text-slate-600 underline"
                  >
                    Ouvrir l&apos;exercice déposé
                  </a>
                </li>
              );
            })}
          </ul>
        )}

        {erreur?.etape === "missions" && <BlocErreur erreur={erreur} />}
      </section>

      <form
        onSubmit={soumettreLaNote}
        className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-medium">3. Ma note et mon commentaire</h2>

        <div>
          <label htmlFor="note" className="block text-sm text-slate-600">
            Note sur 20 (entier)
          </label>
          <input
            id="note"
            name="note"
            type="number"
            step="1"
            required
            autoComplete="off"
            value={note}
            onChange={(evenement) => setNote(evenement.target.value)}
            placeholder="15"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-3 text-center text-xl"
          />
        </div>

        <div>
          <label htmlFor="commentaire" className="block text-sm text-slate-600">
            Commentaire
          </label>
          <textarea
            id="commentaire"
            name="commentaire"
            required
            rows={4}
            value={commentaire}
            onChange={(evenement) => setCommentaire(evenement.target.value)}
            placeholder="Points forts, axes d'amélioration…"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={chargement || missionId === null}
          className="w-full rounded bg-slate-900 px-4 py-3 text-sm font-medium text-white disabled:opacity-50"
        >
          {chargement ? "Enregistrement…" : "Rendre ma relecture"}
        </button>

        {missionId === null && !rendue && (
          <p className="text-xs text-slate-500">
            Choisissez d&apos;abord l&apos;exercice à relire à l&apos;étape 2.
          </p>
        )}

        {missionChoisie && (
          <p className="text-xs text-slate-500">
            Note portée sur l&apos;exercice n°{missionChoisie.exerciceId} de la session
            n°{missionChoisie.sessionId}.
          </p>
        )}

        {rendue && (
          <div className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
            <p className="font-medium">Relecture rendue.</p>
            <p className="mt-1">
              Exercice n°{rendue.exerciceId} — note {rendue.note}/20 — statut{" "}
              {rendue.statut}
            </p>
          </div>
        )}

        {erreur?.etape === "note" && <BlocErreur erreur={erreur} />}
      </form>

      <section className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="text-sm font-medium">4. Corriger ma note</h2>

        {rendue === null ? (
          <p className="text-sm text-slate-500">
            Rendez d&apos;abord une relecture à l&apos;étape 3 : on ne corrige
            qu&apos;une note déjà enregistrée. Après un rechargement de la page,
            cette section reste vide — la seule opération de liste ne renvoie que
            les relectures encore à rendre.
          </p>
        ) : (
          <p className="text-sm text-slate-600">
            Dernière note rendue : exercice n°{rendue.exerciceId} — {rendue.note}/20.
          </p>
        )}

        {rendue !== null && !correctionOuverte && (
          <button
            type="button"
            onClick={ouvrirLaCorrection}
            className="w-full rounded border border-slate-300 px-4 py-3 text-sm font-medium"
          >
            Corriger ma note
          </button>
        )}

        {rendue !== null && correctionOuverte && (
          <form onSubmit={soumettreLaCorrection} className="flex flex-col gap-3">
            <p className="text-xs text-slate-500">
              La note remplacée reste conservée dans l&apos;historique des
              corrections ; c&apos;est le serveur qui décide si la session permet
              encore de corriger (RG8).
            </p>

            <div>
              <label htmlFor="noteCorrigee" className="block text-sm text-slate-600">
                Nouvelle note sur 20 (entier)
              </label>
              <input
                id="noteCorrigee"
                type="number"
                step="1"
                required
                autoComplete="off"
                value={noteCorrigee}
                onChange={(evenement) => setNoteCorrigee(evenement.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-3 text-center text-xl"
              />
            </div>

            <div>
              <label htmlFor="commentaireCorrige" className="block text-sm text-slate-600">
                Nouveau commentaire
              </label>
              <textarea
                id="commentaireCorrige"
                required
                rows={4}
                value={commentaireCorrige}
                onChange={(evenement) => setCommentaireCorrige(evenement.target.value)}
                className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              />
            </div>

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={chargementCorrection}
                className="flex-1 rounded bg-slate-900 px-4 py-3 text-sm font-medium text-white disabled:opacity-50"
              >
                {chargementCorrection ? "Enregistrement…" : "Enregistrer la correction"}
              </button>
              <button
                type="button"
                disabled={chargementCorrection}
                onClick={fermerLaCorrection}
                className="rounded border border-slate-300 px-4 py-3 text-sm font-medium disabled:opacity-50"
              >
                Annuler
              </button>
            </div>

            {corrigee && (
              <div className="rounded-lg border border-emerald-300 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
                <p className="font-medium">Correction enregistrée</p>
                <p className="mt-1">
                  Exercice n°{corrigee.exerciceId} — note {corrigee.note}/20 —
                  statut {corrigee.statut}
                </p>
                <p className="mt-1">{corrigee.commentaire}</p>
              </div>
            )}

            {erreur?.etape === "correction" && <BlocErreur erreur={erreur} />}
          </form>
        )}
      </section>
    </section>
  );
}
