"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/core/api/types";
import type { LigneTableau, SessionOuverteReponse } from "@/core/api/types";
import { ouvrirSession } from "@/features/session/api/ouvrirSession";
import {
  enregistrerSessionOuverte,
  lireSessionsOuvertes,
  type SessionEnregistree,
} from "@/features/session/sessionsOuvertes";
import { consulterTableau } from "@/features/tableau/api/consulterTableau";

/** Section de l'écran à laquelle rattacher une erreur. */
type Etape = "session" | "tableau";

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

/** Formate une date ISO de l'API pour l'affichage (aucun calcul métier). */
function formaterDate(dateIso: string): string {
  return new Date(dateIso).toLocaleString("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

/**
 * Moyenne telle que renvoyée par l'API. Aucune note rendue arrive en `null` :
 * on affiche un tiret, jamais « 0 », qui laisserait croire à une évaluation
 * ratée. Le serveur a déjà arrondi (F3).
 */
function formaterMoyenne(moyenne: number | null): string {
  return moyenne === null
    ? "—"
    : `${moyenne.toLocaleString("fr-FR", { maximumFractionDigits: 2 })}/20`;
}

/**
 * Écran formateur — EF1 (ouvrir une session et lire son code de présence) et
 * EF9 (tableau de bord de la promotion).
 *
 * Aucune règle métier n'est recalculée ici (F3) : la moyenne, les compteurs et
 * l'état « en attente » viennent du serveur, qui a déjà agrégé la promotion.
 */
export default function EcranFormateur() {
  const [titre, setTitre] = useState("");
  const [promotionId, setPromotionId] = useState("1");
  const [session, setSession] = useState<SessionOuverteReponse | null>(null);
  const [chargement, setChargement] = useState(false);

  const [tableau, setTableau] = useState<LigneTableau[] | null>(null);
  const [chargementTableau, setChargementTableau] = useState(false);

  const [sessions, setSessions] = useState<SessionEnregistree[]>([]);

  const [erreur, setErreur] = useState<ErreurAffichee | null>(null);

  // Le stockage local n'existe pas au rendu serveur : la liste est lue après
  // montage, sinon le HTML initial divergerait de celui du navigateur.
  useEffect(() => {
    setSessions(lireSessionsOuvertes());
  }, []);

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
      // Le code n'existe que dans cette réponse : le retenir est ce qui permet de
      // le retrouver après un rechargement de la page.
      setSessions(enregistrerSessionOuverte(ouverte, titre.trim(), Number(promotionId)));
      // La promotion vient de changer : le tableau affiché ne la concerne plus.
      setTableau(null);
    } catch (echec) {
      setErreur(
        versErreur(echec, "session", "Une erreur est survenue à l'ouverture de la session."),
      );
    } finally {
      setChargement(false);
    }
  }

  async function afficherLeTableau() {
    if (chargementTableau) {
      return;
    }

    setChargementTableau(true);
    setErreur(null);
    setTableau(null);

    try {
      setTableau(await consulterTableau(Number(promotionId)));
    } catch (echec) {
      setErreur(versErreur(echec, "tableau", "Impossible de charger le tableau de bord."));
    } finally {
      setChargementTableau(false);
    }
  }

  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Espace formateur</h1>
        <p className="mt-1 text-sm text-slate-600">
          Ouvrez une session pour obtenir le code de présence à dicter aux
          étudiants, puis suivez la promotion d&apos;un coup d&apos;œil.
        </p>
      </header>

      <form
        onSubmit={soumettre}
        className="space-y-4 rounded-lg border border-slate-200 bg-white p-5"
      >
        <h2 className="text-sm font-medium">1. Ouvrir une session</h2>

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
            onChange={(evenement) => {
              setPromotionId(evenement.target.value);
              // Le tableau appartenait à la promotion précédente.
              setTableau(null);
            }}
            placeholder="1"
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm"
          />
          <p className="mt-1 text-xs text-slate-500">
            La promotion de démonstration a l&apos;identifiant 1. Elle sert aussi
            au tableau de bord ci-dessous.
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

      {erreur?.etape === "session" && <BlocErreur erreur={erreur} />}

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
              <dd className="inline">{formaterDate(session.ouvertureAt)}</dd>
            </div>
            <div>
              <dt className="inline font-medium">Expire le : </dt>
              <dd className="inline">{formaterDate(session.expirationAt)}</dd>
            </div>
          </dl>
        </div>
      )}

      <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium">2. Sessions ouvertes depuis ce navigateur</h2>

        <p className="text-xs text-slate-500">
          Le code s&apos;affiche ci-dessus à l&apos;ouverture ; cette liste le
          retrouve après un rechargement de la page. Elle ne contient que les
          sessions ouvertes <strong>ici</strong> : le contrat ne prévoit aucune
          opération qui relirait les sessions du serveur.
        </p>
        <p className="text-xs text-slate-500">
          La validité d&apos;un code est décidée par le serveur — un étudiant qui
          saisit un code périmé reçoit <code>CODE_EXPIRE</code>. L&apos;heure
          d&apos;expiration est donc affichée telle quelle, sans être
          réinterprétée ici (F3).
        </p>

        {sessions.length === 0 ? (
          <p className="text-sm text-slate-500">
            Aucune session ouverte pour l&apos;instant.
          </p>
        ) : (
          <ul aria-label="Sessions ouvertes" className="flex flex-col gap-2">
            {sessions.map((enregistree) => (
              <li
                key={enregistree.id}
                className={`rounded border px-3 py-3 text-sm ${
                  session?.id === enregistree.id
                    ? "border-emerald-400 bg-emerald-50"
                    : "border-slate-300"
                }`}
              >
                <p className="font-medium">{enregistree.titre}</p>
                <p className="mt-1 font-mono text-lg tracking-widest">
                  {enregistree.code}
                </p>
                <p className="mt-1 text-xs text-slate-600">
                  Session n°{enregistree.id} · promotion {enregistree.promotionId} ·
                  ouverte le {formaterDate(enregistree.ouvertureAt)} · expire le{" "}
                  {formaterDate(enregistree.expirationAt)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="space-y-3 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="text-sm font-medium">3. Tableau de bord de la promotion</h2>

        <p className="text-xs text-slate-500">
          Une ligne par étudiant, y compris ceux qui n&apos;ont encore rien fait.
          Une moyenne « — » signifie qu&apos;aucune note n&apos;a encore été reçue.
        </p>

        <button
          type="button"
          disabled={chargementTableau || promotionId.trim() === ""}
          onClick={afficherLeTableau}
          className="rounded border border-slate-300 bg-white px-4 py-2 text-sm font-medium disabled:opacity-50"
        >
          {chargementTableau ? "Chargement…" : "Afficher le tableau"}
        </button>

        {chargementTableau && (
          <p role="status" className="text-sm text-slate-600">
            Chargement du tableau de bord…
          </p>
        )}

        {tableau !== null && tableau.length === 0 && (
          <p className="text-sm text-slate-500">
            Cette promotion ne contient aucun étudiant.
          </p>
        )}

        {tableau !== null && tableau.length > 0 && (
          <div className="overflow-x-auto">
            <table aria-label="Tableau de bord" className="w-full border-collapse text-sm">
              <thead>
                <tr className="text-left text-xs uppercase text-slate-500">
                  <th scope="col" className="border-b border-slate-200 py-2 pr-3">
                    Étudiant
                  </th>
                  <th scope="col" className="border-b border-slate-200 py-2 pr-3">
                    Présences
                  </th>
                  <th scope="col" className="border-b border-slate-200 py-2 pr-3">
                    Dépôts
                  </th>
                  <th scope="col" className="border-b border-slate-200 py-2 pr-3">
                    Moyenne
                  </th>
                  <th scope="col" className="border-b border-slate-200 py-2">
                    Relectures en attente
                  </th>
                </tr>
              </thead>
              <tbody>
                {tableau.map((ligne) => (
                  <tr key={ligne.etudiantId} className="border-b border-slate-100">
                    <th scope="row" className="py-2 pr-3 text-left font-medium">
                      {ligne.nom}
                    </th>
                    <td className="py-2 pr-3">{ligne.presences}</td>
                    <td className="py-2 pr-3">{ligne.exercicesDeposes}</td>
                    <td className="py-2 pr-3">{formaterMoyenne(ligne.moyenne)}</td>
                    <td className="py-2">{ligne.relecturesEnAttente}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {erreur?.etape === "tableau" && <BlocErreur erreur={erreur} />}
      </section>
    </section>
  );
}
