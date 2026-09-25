"use client";

import { useState } from "react";
import {
  KeyRoundIcon,
  ListChecksIcon,
  LoaderCircleIcon,
  PencilLineIcon,
  SaveIcon,
  SendIcon,
  UndoIcon,
  UserRoundCheckIcon,
} from "lucide-react";
import { ApiError } from "@/core/api/types";
import type {
  EtudiantResume,
  ExerciceDeposeReponse,
  NoteRecue,
  PresenceReponse,
} from "@/core/api/types";
import { CLASSE_CARTE, EnteteEtape } from "@/components/etape";
import { AlerteErreur, BlocSucces, Chargement, EtatVide } from "@/components/retours";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { deposerExercice } from "@/features/exercice/api/deposerExercice";
import { remplacerLien } from "@/features/exercice/api/remplacerLien";
import { marquerPresence } from "@/features/presence/api/marquerPresence";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";
import { listerNotesRecues } from "@/features/relecture/api/listerNotesRecues";
import { cn } from "@/lib/utils";

/** Étape de l'écran à laquelle rattacher une erreur. */
type Etape = "etudiants" | "presence" | "depot" | "remplacement" | "notes";

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

/**
 * Écran étudiant — EF2 (marquer sa présence), EF3 (déposer son exercice),
 * EF4 (remplacer le lien de cet exercice) et EF8 (consulter ses notes reçues).
 *
 * Vue mobile en priorité (ENF1) : une colonne, aucune largeur fixe, aucun
 * défilement horizontal. Aucune règle métier n'est recalculée ici (F3) : format
 * du lien, expiration du code, blocage RG3 et statut de l'exercice viennent tous
 * du serveur. Les trois formulaires restent frères et dans l'ordre — leur style
 * de carte est appliqué directement sur le `<form>`, sans boîte intermédiaire.
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

  // EF4 — le lien déposé n'est conservé que le temps de l'écran : le contrat
  // n'offre aucune opération qui relirait les exercices d'un étudiant.
  const [lienDepose, setLienDepose] = useState("");
  const [remplacementOuvert, setRemplacementOuvert] = useState(false);
  const [nouveauLien, setNouveauLien] = useState("");
  const [exerciceRemplace, setExerciceRemplace] = useState<ExerciceDeposeReponse | null>(null);
  const [chargementRemplacement, setChargementRemplacement] = useState(false);

  const [notes, setNotes] = useState<NoteRecue[] | null>(null);
  const [chargementNotes, setChargementNotes] = useState(false);

  const [chargementListe, setChargementListe] = useState(false);
  const [erreur, setErreur] = useState<ErreurAffichee | null>(null);

  async function chargerLesEtudiants(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setChargementListe(true);
    setErreur(null);
    setPresence(null);
    setEtudiants([]);
    setEtudiantId(null);
    setNotes(null);

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
    // Un nouveau dépôt remplace le contexte de l'exercice précédent.
    fermerLeRemplacement();

    const lienEnvoye = lien.trim();

    try {
      setExercice(
        await deposerExercice({
          sessionId: Number(sessionId),
          etudiantId,
          lien: lienEnvoye,
        }),
      );
      setLienDepose(lienEnvoye);
      setLien("");
    } catch (echec) {
      setErreur(versErreur(echec, "depot", "Le dépôt n'a pas pu être enregistré."));
    } finally {
      setChargementDepot(false);
    }
  }

  /**
   * Ouvre le remplacement en repartant du lien déposé : l'étudiant corrige ce
   * qu'il a envoyé, il ne le ressaisit pas de mémoire.
   */
  function ouvrirLeRemplacement() {
    if (exercice === null) {
      return;
    }
    setNouveauLien(lienDepose);
    setExerciceRemplace(null);
    setErreur(null);
    setRemplacementOuvert(true);
  }

  function fermerLeRemplacement() {
    setRemplacementOuvert(false);
    setNouveauLien("");
    setExerciceRemplace(null);
  }

  async function soumettreLeRemplacement(evenement: React.FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (exercice === null || chargementRemplacement) {
      return;
    }

    setChargementRemplacement(true);
    setErreur(null);
    setExerciceRemplace(null);

    const lienEnvoye = nouveauLien.trim();

    try {
      const reponse = await remplacerLien(exercice.id, { lien: lienEnvoye });
      setExerciceRemplace(reponse);
      // Le lien de référence devient celui que le serveur vient d'accepter.
      setLienDepose(lienEnvoye);
    } catch (echec) {
      setErreur(
        versErreur(echec, "remplacement", "Le remplacement du lien n'a pas pu être enregistré."),
      );
    } finally {
      setChargementRemplacement(false);
    }
  }

  async function chargerLesNotes() {
    if (etudiantId === null || chargementNotes) {
      return;
    }

    setChargementNotes(true);
    setErreur(null);

    try {
      setNotes(await listerNotesRecues(etudiantId));
    } catch (echec) {
      setErreur(versErreur(echec, "notes", "Impossible de charger vos notes reçues."));
    } finally {
      setChargementNotes(false);
    }
  }

  return (
    <section className="mx-auto flex w-full max-w-2xl flex-col gap-6">
      <header className="space-y-3">
        <div className="flex flex-wrap gap-1.5">
          <Badge variant="secondary">Étudiant</Badge>
          <Badge variant="outline" className="font-normal text-muted-foreground">
            EF2 · EF3 · EF4 · EF8
          </Badge>
        </div>
        <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-3xl">
          Espace étudiant
        </h1>
        <p className="text-sm text-muted-foreground">
          Choisissez votre nom, saisissez le code dicté par le formateur, puis
          déposez le lien de votre exercice.
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
              disabled={chargementListe}
              className="h-10 flex-1 sm:flex-none sm:px-4"
            >
              {chargementListe ? (
                <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
              ) : (
                <UserRoundCheckIcon aria-hidden="true" />
              )}
              {chargementListe ? "Chargement…" : "Afficher les étudiants"}
            </Button>
          </div>
        </div>

        {chargementListe && <Chargement libelle="Chargement de la liste…" />}

        {etudiants.length > 0 && (
          <ul className="flex flex-col gap-2">
            {etudiants.map((etudiant) => {
              const selectionne = etudiant.id === etudiantId;
              return (
                <li key={etudiant.id}>
                  <button
                    type="button"
                    aria-pressed={selectionne}
                    onClick={() => {
                      setEtudiantId(etudiant.id);
                      // Les notes affichées appartenaient à l'étudiant précédent.
                      setNotes(null);
                    }}
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

        {etudiants.length === 0 && !chargementListe && erreur?.etape !== "etudiants" && (
          <EtatVide>Aucun étudiant affiché pour l&apos;instant.</EtatVide>
        )}

        {erreur?.etape === "etudiants" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </form>

      <form onSubmit={soumettreLeCode} className={CLASSE_CARTE}>
        <EnteteEtape
          numero={2}
          titre="Code de présence"
          description="Le code est dicté par le formateur. Il est alphanumérique (lettres et chiffres), jamais uniquement des chiffres."
        />

        <div className="space-y-2">
          <Label htmlFor="code">Code dicté par le formateur</Label>
          <Input
            id="code"
            name="code"
            required
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            value={code}
            onChange={(evenement) => setCode(evenement.target.value)}
            placeholder="ABC234"
            aria-describedby="code-aide"
            className="h-16 text-center font-mono text-2xl font-semibold tracking-[0.3em]"
          />
          <p id="code-aide" className="text-xs text-muted-foreground">
            Saisissez-le tel qu&apos;il a été dicté, sans espace.
          </p>
        </div>

        <Button
          type="submit"
          disabled={chargementPresence || etudiantId === null}
          className="h-11 w-full text-base"
        >
          {chargementPresence ? (
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
          ) : (
            <KeyRoundIcon aria-hidden="true" />
          )}
          {chargementPresence ? "Enregistrement…" : "Marquer ma présence"}
        </Button>

        {etudiantId === null && (
          <p className="text-xs text-muted-foreground">
            Choisissez d&apos;abord votre nom à l&apos;étape 1.
          </p>
        )}

        {presence && (
          <BlocSucces titre="Présence enregistrée.">
            <p>
              Session n°{presence.sessionId} — source : {presence.source}
            </p>
          </BlocSucces>
        )}

        {erreur?.etape === "presence" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </form>

      <form onSubmit={deposerLeLien} className={CLASSE_CARTE}>
        <EnteteEtape
          numero={3}
          titre="Déposer mon exercice"
          description="Le lien du dépôt, tel qu'il sera relu par un autre étudiant."
        />

        <div className="space-y-2">
          <Label htmlFor="sessionId">Session</Label>
          <Input
            id="sessionId"
            type="number"
            min="1"
            required
            value={sessionId}
            onChange={(evenement) => setSessionId(evenement.target.value)}
            className="h-10"
          />
          <p className="text-xs text-muted-foreground">
            Renseigné automatiquement après avoir marqué votre présence.
          </p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="lien">Lien de l&apos;exercice</Label>
          <Input
            id="lien"
            name="lien"
            type="url"
            required
            autoComplete="off"
            spellCheck={false}
            value={lien}
            onChange={(evenement) => setLien(evenement.target.value)}
            placeholder="https://…"
            className="h-10"
          />
        </div>

        <Button
          type="submit"
          disabled={chargementDepot || etudiantId === null}
          className="h-11 w-full text-base"
        >
          {chargementDepot ? (
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
          ) : (
            <SendIcon aria-hidden="true" />
          )}
          {chargementDepot ? "Dépôt…" : "Déposer mon exercice"}
        </Button>

        <p className="text-xs text-muted-foreground">
          Le dépôt reste possible après l&apos;expiration du code, tant que le
          formateur n&apos;a pas clôturé la session.
        </p>

        {exercice && (
          <BlocSucces titre="Exercice déposé.">
            <p>
              Exercice n°{exercice.id} — statut : {exercice.statut}
            </p>
          </BlocSucces>
        )}

        {erreur?.etape === "depot" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </form>

      <section className={CLASSE_CARTE}>
        <EnteteEtape
          numero={4}
          titre="Remplacer le lien de mon exercice"
          description="Tant qu'aucune relecture n'a été commencée sur cet exercice et que la session n'est pas clôturée — c'est le serveur qui en décide."
        />

        {exercice === null ? (
          <EtatVide>
            Déposez d&apos;abord un exercice à l&apos;étape 3 : cette section
            s&apos;appuie sur l&apos;exercice que vous venez de déposer.
          </EtatVide>
        ) : (
          <div className="rounded-lg border border-border bg-muted/40 px-3 py-2 text-sm">
            <p className="flex flex-wrap items-center gap-2">
              <Badge variant="outline" className="font-mono">
                Exercice n°{exercice.id}
              </Badge>
              <Badge variant="secondary" className="font-mono">
                {exercice.statut}
              </Badge>
            </p>
            {lienDepose !== "" && (
              <p className="mt-2 break-all text-xs text-muted-foreground">
                Lien déposé : {lienDepose}
              </p>
            )}
          </div>
        )}

        {exercice !== null && !remplacementOuvert && (
          <Button
            type="button"
            variant="outline"
            onClick={ouvrirLeRemplacement}
            className="h-11 w-full text-base"
          >
            <PencilLineIcon aria-hidden="true" />
            Remplacer le lien
          </Button>
        )}

        {exercice !== null && remplacementOuvert && (
          <form onSubmit={soumettreLeRemplacement} className="flex flex-col gap-4">
            <div className="space-y-2">
              <Label htmlFor="nouveauLien">Nouveau lien</Label>
              <Input
                id="nouveauLien"
                type="url"
                required
                autoComplete="off"
                spellCheck={false}
                value={nouveauLien}
                onChange={(evenement) => setNouveauLien(evenement.target.value)}
                placeholder="https://…"
                className="h-10"
              />
            </div>

            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                type="submit"
                disabled={chargementRemplacement}
                className="h-11 flex-1 text-base"
              >
                {chargementRemplacement ? (
                  <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
                ) : (
                  <SaveIcon aria-hidden="true" />
                )}
                {chargementRemplacement ? "Remplacement…" : "Enregistrer le nouveau lien"}
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={chargementRemplacement}
                onClick={fermerLeRemplacement}
                className="h-11"
              >
                <UndoIcon aria-hidden="true" />
                Annuler
              </Button>
            </div>

            {exerciceRemplace && (
              <BlocSucces titre="Lien remplacé.">
                <p>
                  Exercice n°{exerciceRemplace.id} — statut : {exerciceRemplace.statut}
                </p>
              </BlocSucces>
            )}

            {erreur?.etape === "remplacement" && (
              <AlerteErreur code={erreur.code} message={erreur.message} />
            )}
          </form>
        )}
      </section>

      <section className={CLASSE_CARTE}>
        <EnteteEtape
          numero={5}
          titre="Mes notes reçues"
          description="La note et le commentaire reçus pour vos exercices. Le nom de votre relecteur ne vous est jamais communiqué."
        />

        <Button
          type="button"
          variant="outline"
          disabled={etudiantId === null || chargementNotes}
          onClick={() => void chargerLesNotes()}
          className="h-11 w-full text-base"
        >
          {chargementNotes ? (
            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
          ) : (
            <ListChecksIcon aria-hidden="true" />
          )}
          {chargementNotes ? "Chargement…" : "Afficher mes notes reçues"}
        </Button>

        {etudiantId === null && (
          <p className="text-xs text-muted-foreground">
            Choisissez d&apos;abord votre nom à l&apos;étape 1.
          </p>
        )}

        {notes !== null && notes.length === 0 && (
          <EtatVide>Aucune relecture reçue pour l&apos;instant.</EtatVide>
        )}

        {notes !== null && notes.length > 0 && (
          <ul aria-label="Notes reçues" className="flex flex-col gap-3">
            {notes.map((note) => (
              <li
                key={note.exerciceId}
                className="flex flex-col gap-2 rounded-lg border border-border px-3 py-3 text-sm"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium">Exercice n°{note.exerciceId}</span>
                  {note.note === null ? (
                    <Badge variant="outline" className="text-muted-foreground">
                      {note.statut} — note à venir
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="border-success/30 bg-success/10 font-mono text-success">
                      {note.note}/20
                    </Badge>
                  )}
                </div>

                {note.note !== null && (
                  <p className="text-xs text-muted-foreground">Statut : {note.statut}</p>
                )}

                {note.commentaire !== null && (
                  <p className="border-l-2 border-border pl-3 text-muted-foreground">
                    {note.commentaire}
                  </p>
                )}
              </li>
            ))}
          </ul>
        )}

        {erreur?.etape === "notes" && (
          <AlerteErreur code={erreur.code} message={erreur.message} />
        )}
      </section>
    </section>
  );
}
