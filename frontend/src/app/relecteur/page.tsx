"use client";

import { useState } from "react";
import {
  ClipboardListIcon,
  ExternalLinkIcon,
  LoaderCircleIcon,
  PenLineIcon,
  SaveIcon,
  SquarePenIcon,
  UndoIcon,
  UserRoundCheckIcon,
} from "lucide-react";
import { ApiError } from "@/core/api/types";
import type {
  EtudiantResume,
  MissionRelecteur,
  RelectureRendueReponse,
} from "@/core/api/types";
import { CLASSE_CARTE, EnteteEtape } from "@/components/etape";
import { AlerteErreur, BlocSucces, Chargement, EtatVide } from "@/components/retours";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";
import { corrigerRelecture } from "@/features/relecture/api/corrigerRelecture";
import { listerMissionsEnAttente } from "@/features/relecture/api/listerMissionsEnAttente";
import { rendreRelecture } from "@/features/relecture/api/rendreRelecture";
import { cn } from "@/lib/utils";

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

/** Bornes affichées de la note — le serveur reste seul juge de sa validité (RG7). */
const NOTE_MIN = 0;
const NOTE_MAX = 20;

/**
 * Une note saisie est « prête à envoyer » si c'est un entier dans [0, 20].
 *
 * C'est une aide de saisie, pas une règle métier : la validité réelle de la note
 * est décidée par le serveur (`NOTE_INVALIDE`, RG7). Le formulaire ne fait
 * qu'éviter d'envoyer une valeur manifestement incomplète ou hors bornes.
 */
function notePrete(noteSaisie: string): boolean {
  const valeur = Number(noteSaisie.trim());
  return (
    noteSaisie.trim() !== "" &&
    Number.isInteger(valeur) &&
    valeur >= NOTE_MIN &&
    valeur <= NOTE_MAX
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
 * serveur. Les deux formulaires de premier niveau restent frères et dans
 * l'ordre ; le formulaire de correction reste imbriqué dans sa section.
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

  const notePreteAEnvoyer = notePrete(note);
  const noteMalSaisie = note.trim() !== "" && !notePreteAEnvoyer;
  const correctionPrete = notePrete(noteCorrigee);
  const correctionMalSaisie = noteCorrigee.trim() !== "" && !correctionPrete;

  return (
    <section className="mx-auto flex w-full max-w-2xl flex-col gap-6">
      <header className="space-y-3">
        <div className="flex flex-wrap gap-1.5">
          <Badge variant="secondary">Relecteur</Badge>
          <Badge variant="outline" className="font-normal text-muted-foreground">
            EF6 · EF7
          </Badge>
        </div>
        <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-3xl">
          Espace relecteur
        </h1>
        <p className="text-sm text-muted-foreground">
          Choisissez votre nom, ouvrez l&apos;exercice qui vous est confié, puis
          rendez votre note et votre commentaire.
        </p>
      </header>

      <form onSubmit={chargerLesEtudiants} className={CLASSE_CARTE}>
        <EnteteEtape
          numero={1}
          titre="Qui êtes-vous ?"
          description="Il n'y a pas d'authentification : votre nom est choisi dans la promotion."
        />

        <div className="space-y-2">
          <Label htmlFor="promotionId">Promotion</Label>
          <div className="flex flex-wrap gap-2">
            <Input
              id="promotionId"
              type="number"
              min="1"
              required
              value={promotionId}
              onChange={(evenement) => setPromotionId(evenement.target.value)}
              className="h-10 w-28"
            />
            <Button
              type="submit"
              disabled={chargement}
              className="h-10 flex-1 sm:flex-none sm:px-4"
            >
              {chargement ? (
                <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
              ) : (
                <UserRoundCheckIcon aria-hidden="true" />
              )}
              {chargement ? "Chargement…" : "Afficher les étudiants"}
            </Button>
          </div>
        </div>

        {chargement && <Chargement libelle="Chargement de la liste…" />}

        {etudiants.length > 0 && (
          <ul aria-label="Étudiants de la promotion" className="flex flex-col gap-2">
            {etudiants.map((etudiant) => {
              const selectionne = etudiant.id === etudiantId;
              return (
                <li key={etudiant.id}>
                  <button
                    type="button"
                    aria-pressed={selectionne}
                    onClick={() => void choisirEtudiant(etudiant.id)}
                    className={cn(
                      "w-full rounded-lg border px-3 py-3 text-left text-sm font-medium transition-colors",
                      "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                      selectionne
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-card hover:bg-muted",
                    )}
                  >
                    {etudiant.prenom} {etudiant.nom}
                  </button>
                </li>
              );
            })}
          </ul>
        )}

        {etudiants.length === 0 && !chargement && erreur?.etape !== "etudiants" && (
          <EtatVide>Aucun étudiant affiché pour l&apos;instant.</EtatVide>
        )}

        {erreur?.etape === "etudiants" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </form>

      <section className={CLASSE_CARTE}>
        <EnteteEtape
          numero={2}
          titre="Exercice à relire"
          description="L'exercice qui vous est confié ne vous dit jamais qui l'a écrit."
        />

        {etudiantId === null && (
          <EtatVide>Choisissez d&apos;abord votre nom à l&apos;étape 1.</EtatVide>
        )}

        {etudiantId !== null && missions.length === 0 && erreur?.etape !== "missions" && (
          <EtatVide>Aucun exercice ne vous est confié pour le moment.</EtatVide>
        )}

        {missions.length > 0 && (
          <ul aria-label="Exercices à relire" className="flex flex-col gap-3">
            {missions.map((mission) => {
              const selectionnee = mission.relectureId === missionId;
              return (
                <li key={mission.relectureId} className="flex flex-col gap-2">
                  <button
                    type="button"
                    aria-pressed={selectionnee}
                    onClick={() => setMissionId(mission.relectureId)}
                    className={cn(
                      "w-full rounded-lg border px-3 py-3 text-left text-sm transition-colors",
                      "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                      selectionnee
                        ? "border-primary bg-primary/10 ring-1 ring-primary/40"
                        : "border-border bg-card hover:bg-muted",
                    )}
                  >
                    <span className="flex flex-wrap items-center justify-between gap-2">
                      <span className="font-medium">
                        Exercice n°{mission.exerciceId} — session n°{mission.sessionId}
                      </span>
                      <Badge
                        variant="outline"
                        className={cn(
                          "font-mono",
                          mission.statut === "EN_ATTENTE"
                            ? "border-warning/40 bg-warning/10 text-warning"
                            : "border-success/30 bg-success/10 text-success",
                        )}
                      >
                        {mission.statut}
                      </Badge>
                    </span>
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {mission.statut === "EN_ATTENTE"
                        ? "À relire : votre note n'est pas encore rendue."
                        : "Relecture déjà rendue."}
                    </span>
                  </button>
                  <a
                    href={mission.lien}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 self-start px-1 text-xs text-muted-foreground underline underline-offset-4 hover:text-foreground"
                  >
                    <ExternalLinkIcon className="size-3.5" aria-hidden="true" />
                    Ouvrir l&apos;exercice déposé
                  </a>
                </li>
              );
            })}
          </ul>
        )}

        {erreur?.etape === "missions" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </section>

      <form onSubmit={soumettreLaNote} className={CLASSE_CARTE}>
        <EnteteEtape
          numero={3}
          titre="Ma note et mon commentaire"
          description="Note entière de 0 à 20, puis un commentaire. C'est le serveur qui valide la note et fait évoluer les statuts."
        />

        <div className="space-y-2">
          <Label htmlFor="note">Note sur 20 (entier)</Label>
          <Input
            id="note"
            name="note"
            type="number"
            step="1"
            min={NOTE_MIN}
            max={NOTE_MAX}
            inputMode="numeric"
            required
            autoComplete="off"
            value={note}
            onChange={(evenement) => setNote(evenement.target.value)}
            placeholder="15"
            aria-invalid={noteMalSaisie}
            aria-describedby="note-aide"
            className="h-14 text-center font-mono text-2xl font-semibold"
          />
          <input
            type="range"
            min={NOTE_MIN}
            max={NOTE_MAX}
            step={1}
            value={notePreteAEnvoyer ? note : String(NOTE_MIN)}
            onChange={(evenement) => setNote(evenement.target.value)}
            aria-label="Curseur de note sur 20"
            className="h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
          />
          <p
            id="note-aide"
            className={cn("text-xs", noteMalSaisie ? "text-destructive" : "text-muted-foreground")}
          >
            {noteMalSaisie
              ? `Valeur attendue : un entier entre ${NOTE_MIN} et ${NOTE_MAX}.`
              : `Note entière entre ${NOTE_MIN} et ${NOTE_MAX} (curseur ou saisie directe).`}
          </p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="commentaire">Commentaire</Label>
          <Textarea
            id="commentaire"
            name="commentaire"
            required
            rows={4}
            value={commentaire}
            onChange={(evenement) => setCommentaire(evenement.target.value)}
            placeholder="Points forts, axes d'amélioration…"
          />
        </div>

        <Button
          type="submit"
          disabled={chargement || missionId === null || !notePreteAEnvoyer}
          className="h-11 w-full text-base"
        >
          {chargement ? (
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
          ) : (
            <PenLineIcon aria-hidden="true" />
          )}
          {chargement ? "Enregistrement…" : "Rendre ma relecture"}
        </Button>

        {missionId === null && !rendue && (
          <p className="text-xs text-muted-foreground">
            Choisissez d&apos;abord l&apos;exercice à relire à l&apos;étape 2.
          </p>
        )}

        {missionChoisie && (
          <p className="text-xs text-muted-foreground">
            Note portée sur l&apos;exercice n°{missionChoisie.exerciceId} de la session n°
            {missionChoisie.sessionId}.
          </p>
        )}

        {rendue && (
          <BlocSucces titre="Relecture rendue.">
            <p>
              Exercice n°{rendue.exerciceId} — note {rendue.note}/20 — statut {rendue.statut}
            </p>
            {rendue.commentaire !== "" && (
              <p className="mt-1 text-muted-foreground">{rendue.commentaire}</p>
            )}
          </BlocSucces>
        )}

        {erreur?.etape === "note" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </form>

      <section className={CLASSE_CARTE}>
        <EnteteEtape
          numero={4}
          titre="Corriger ma note"
          description="On ne corrige qu'une relecture déjà rendue. La note remplacée reste conservée dans l'historique ; c'est le serveur qui décide si la session permet encore de corriger (RG8)."
        />

        {rendue === null ? (
          <EtatVide>
            Rendez d&apos;abord une relecture à l&apos;étape 3. Après un
            rechargement de la page, cette section reste vide — la seule opération
            de liste ne renvoie que les relectures encore à rendre.
          </EtatVide>
        ) : (
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-success/30 bg-success/5 px-3 py-2 text-sm">
            <span className="flex items-center gap-2">
              <ClipboardListIcon className="size-4 text-success" aria-hidden="true" />
              Relecture rendue et modifiable
            </span>
            <Badge variant="outline" className="font-mono">
              Exercice n°{rendue.exerciceId} — {rendue.note}/20
            </Badge>
          </div>
        )}

        {rendue !== null && !correctionOuverte && (
          <Button
            type="button"
            variant="outline"
            onClick={ouvrirLaCorrection}
            className="h-11 w-full text-base"
          >
            <SquarePenIcon aria-hidden="true" />
            Corriger ma note
          </Button>
        )}

        {rendue !== null && correctionOuverte && (
          <form onSubmit={soumettreLaCorrection} className="flex flex-col gap-4">
            <div className="space-y-2">
              <Label htmlFor="noteCorrigee">Nouvelle note sur 20 (entier)</Label>
              <Input
                id="noteCorrigee"
                type="number"
                step="1"
                min={NOTE_MIN}
                max={NOTE_MAX}
                inputMode="numeric"
                required
                autoComplete="off"
                value={noteCorrigee}
                onChange={(evenement) => setNoteCorrigee(evenement.target.value)}
                aria-invalid={correctionMalSaisie}
                aria-describedby="note-corrigee-aide"
                className="h-14 text-center font-mono text-2xl font-semibold"
              />
              <input
                type="range"
                min={NOTE_MIN}
                max={NOTE_MAX}
                step={1}
                value={correctionPrete ? noteCorrigee : String(NOTE_MIN)}
                onChange={(evenement) => setNoteCorrigee(evenement.target.value)}
                aria-label="Curseur de la nouvelle note sur 20"
                className="h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-primary"
              />
              <p
                id="note-corrigee-aide"
                className={cn(
                  "text-xs",
                  correctionMalSaisie ? "text-destructive" : "text-muted-foreground",
                )}
              >
                {correctionMalSaisie
                  ? `Valeur attendue : un entier entre ${NOTE_MIN} et ${NOTE_MAX}.`
                  : `Note entière entre ${NOTE_MIN} et ${NOTE_MAX} (curseur ou saisie directe).`}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="commentaireCorrige">Nouveau commentaire</Label>
              <Textarea
                id="commentaireCorrige"
                required
                rows={4}
                value={commentaireCorrige}
                onChange={(evenement) => setCommentaireCorrige(evenement.target.value)}
              />
            </div>

            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                type="submit"
                disabled={chargementCorrection || !correctionPrete}
                className="h-11 flex-1 text-base"
              >
                {chargementCorrection ? (
                  <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
                ) : (
                  <SaveIcon aria-hidden="true" />
                )}
                {chargementCorrection ? "Enregistrement…" : "Enregistrer la correction"}
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={chargementCorrection}
                onClick={fermerLaCorrection}
                className="h-11"
              >
                <UndoIcon aria-hidden="true" />
                Annuler
              </Button>
            </div>

            {corrigee && (
              <BlocSucces titre="Correction enregistrée">
                <p>
                  Exercice n°{corrigee.exerciceId} — note {corrigee.note}/20 — statut{" "}
                  {corrigee.statut}
                </p>
                {corrigee.commentaire !== "" && (
                  <p className="mt-1 text-muted-foreground">{corrigee.commentaire}</p>
                )}
              </BlocSucces>
            )}

            {erreur?.etape === "correction" && (
              <AlerteErreur code={erreur.code} message={erreur.message} />
            )}
          </form>
        )}
      </section>
    </section>
  );
}
