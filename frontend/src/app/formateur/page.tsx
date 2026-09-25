"use client";

import { useEffect, useState } from "react";
import {
  ArrowDownIcon,
  ArrowUpDownIcon,
  ArrowUpIcon,
  CirclePlayIcon,
  KeyRoundIcon,
  LayoutListIcon,
  LoaderCircleIcon,
  UsersIcon,
} from "lucide-react";
import { ApiError, CODES_ERREUR } from "@/core/api/types";
import type {
  EtudiantResume,
  LigneTableau,
  SessionOuverteReponse,
} from "@/core/api/types";
import { CodeSession, ExpirationSession } from "@/components/code-session";
import { EnteteEtape } from "@/components/etape";
import { AlerteErreur, Chargement, EtatVide } from "@/components/retours";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ajouterPresenceManuelle } from "@/features/presence/api/ajouterPresenceManuelle";
import { listerEtudiants } from "@/features/promotion/api/listerEtudiants";
import { cloturerSession } from "@/features/session/api/cloturerSession";
import { ouvrirSession } from "@/features/session/api/ouvrirSession";
import {
  enregistrerSessionOuverte,
  lireSessionsOuvertes,
  marquerSessionCloturee,
  type SessionEnregistree,
} from "@/features/session/sessionsOuvertes";
import { consulterTableau } from "@/features/tableau/api/consulterTableau";
import { cn } from "@/lib/utils";

/** Section de l'écran à laquelle rattacher une erreur. */
type Etape = "session" | "cloture" | "tableau" | "presence";

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
 * Colonnes du tableau de bord et clé de tri associée. Le tri ne réordonne que
 * l'affichage : les valeurs (moyennes, compteurs) restent celles du serveur.
 */
const COLONNES = [
  { cle: "nom", libelle: "Étudiant", numerique: false },
  { cle: "presences", libelle: "Présences", numerique: true },
  { cle: "exercicesDeposes", libelle: "Dépôts", numerique: true },
  { cle: "moyenne", libelle: "Moyenne", numerique: true },
  { cle: "relecturesEnAttente", libelle: "Relectures en attente", numerique: true },
] as const;

type CleTri = (typeof COLONNES)[number]["cle"];
type SensTri = "asc" | "desc";

/** Compare deux lignes du tableau pour l'affichage (aucun recalcul de valeur). */
function comparerLignes(
  a: LigneTableau,
  b: LigneTableau,
  cle: CleTri,
  sens: SensTri,
): number {
  if (cle === "nom") {
    return sens === "asc" ? a.nom.localeCompare(b.nom, "fr") : b.nom.localeCompare(a.nom, "fr");
  }

  const valeurA = cle === "moyenne" ? a.moyenne : a[cle];
  const valeurB = cle === "moyenne" ? b.moyenne : b[cle];

  // Sans note rendue, la valeur reste en fin de liste dans les deux sens :
  // l'ordre ne doit jamais laisser croire qu'un « — » est un zéro.
  if (valeurA === null) {
    return 1;
  }
  if (valeurB === null) {
    return -1;
  }
  return sens === "asc" ? valeurA - valeurB : valeurB - valeurA;
}

/**
 * Écran formateur — EF1 (ouvrir une session et lire son code de présence),
 * EF11 (clôturer une session), EF9 (tableau de bord de la promotion) et EF10
 * (ajouter une présence manuellement).
 *
 * Aucune règle métier n'est recalculée ici (F3) : la moyenne, les compteurs,
 * l'état « en attente » et l'état « clôturée » viennent du serveur. Seule la
 * présentation a changé : hiérarchie visuelle, composants shadcn/ui, thème
 * clair/sombre et retours d'état homogènes avec les deux autres écrans.
 */
export default function EcranFormateur() {
  const [titre, setTitre] = useState("");
  const [promotionId, setPromotionId] = useState("1");
  const [session, setSession] = useState<SessionOuverteReponse | null>(null);
  const [chargement, setChargement] = useState(false);

  const [tableau, setTableau] = useState<LigneTableau[] | null>(null);
  const [chargementTableau, setChargementTableau] = useState(false);
  const [tri, setTri] = useState<{ cle: CleTri; sens: SensTri } | null>(null);

  const [sessions, setSessions] = useState<SessionEnregistree[]>([]);
  const [cloture, setCloture] = useState<number | null>(null);

  // EF10 — session choisie pour l'ajout manuel, et étudiants de sa promotion.
  const [sessionPresence, setSessionPresence] = useState<number | null>(null);
  const [etudiants, setEtudiants] = useState<EtudiantResume[]>([]);
  const [chargementEtudiants, setChargementEtudiants] = useState(false);
  // Étudiants que le serveur a confirmés présents sur cette session, avec le
  // libellé à afficher. Rien n'est déduit ici : le 201 et le 409 font foi.
  const [presents, setPresents] = useState<Map<number, string>>(new Map());
  const [confirmation, setConfirmation] = useState<number | null>(null);
  const [ajout, setAjout] = useState<number | null>(null);

  const [erreur, setErreur] = useState<ErreurAffichee | null>(null);

  const sessionCouranteCloturee =
    session !== null && sessions.some((connue) => connue.id === session.id && connue.cloturee);

  // Le stockage local n'existe pas au rendu serveur : la liste est lue après
  // montage, sinon le HTML initial divergerait de celui du navigateur.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
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

  async function cloturerUneSession(sessionId: number) {
    if (cloture === sessionId) {
      return;
    }

    setCloture(sessionId);
    setErreur(null);

    try {
      const reponse = await cloturerSession(sessionId);
      // L'état affiché vient du serveur : il est simplement reporté dans la liste
      // locale, qui est la seule trace disponible faute d'opération de relecture.
      if (reponse.cloturee) {
        setSessions(marquerSessionCloturee(reponse.id));
      }
    } catch (echec) {
      setErreur(versErreur(echec, "cloture", "Impossible de clôturer cette session."));
    } finally {
      setCloture(null);
    }
  }

  async function choisirLaSession(valeur: string) {
    setConfirmation(null);
    setErreur(null);
    // « Présent » ne se dit que pour une session donnée : changer de session
    // remet la liste des étudiants et l'état affiché à zéro.
    setEtudiants([]);
    setPresents(new Map());

    if (valeur === "") {
      setSessionPresence(null);
      return;
    }

    const sessionId = Number(valeur);
    setSessionPresence(sessionId);

    const choisie = sessions.find((connue) => connue.id === sessionId);
    if (choisie === undefined) {
      return;
    }

    setChargementEtudiants(true);
    try {
      setEtudiants(await listerEtudiants(choisie.promotionId));
    } catch (echec) {
      setErreur(
        versErreur(echec, "presence", "Impossible de charger les étudiants de la promotion."),
      );
    } finally {
      setChargementEtudiants(false);
    }
  }

  function retenirPresent(etudiantId: number, libelle: string) {
    setPresents((precedents) => new Map(precedents).set(etudiantId, libelle));
  }

  async function ajouterLaPresence(etudiantId: number) {
    if (sessionPresence === null || ajout !== null) {
      return;
    }

    setAjout(etudiantId);
    setErreur(null);

    try {
      const ajoutee = await ajouterPresenceManuelle({
        sessionId: sessionPresence,
        etudiantId,
      });
      // La source affichée est celle que le serveur a enregistrée (RG12).
      retenirPresent(ajoutee.etudiantId, `Présent — source ${ajoutee.source}`);
      setConfirmation(null);
    } catch (echec) {
      if (echec instanceof ApiError && echec.code === CODES_ERREUR.DEJA_PRESENT) {
        // Refus attendu, pas un incident : le serveur dit que la présence existe
        // déjà. L'écran l'enregistre au lieu de le supposer — le contrat n'offre
        // aucune opération qui relirait les présences d'une session.
        retenirPresent(etudiantId, "Déjà présent");
        setConfirmation(null);
      } else {
        setErreur(
          versErreur(echec, "presence", "La présence n'a pas pu être enregistrée."),
        );
      }
    } finally {
      setAjout(null);
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

  function basculerTri(cle: CleTri) {
    setTri((precedent) =>
      precedent !== null && precedent.cle === cle
        ? { cle, sens: precedent.sens === "asc" ? "desc" : "asc" }
        : { cle, sens: "asc" },
    );
  }

  const lignesAffichees =
    tableau === null || tri === null
      ? tableau
      : [...tableau].sort((a, b) => comparerLignes(a, b, tri.cle, tri.sens));

  return (
    <div className="space-y-6">
      <header className="space-y-3">
        <div className="flex flex-wrap gap-1.5">
          <Badge variant="secondary">Formateur</Badge>
          <Badge variant="outline" className="font-normal text-muted-foreground">
            EF1 · EF9 · EF10 · EF11
          </Badge>
        </div>
        <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-3xl">
          Espace formateur
        </h1>
        <p className="max-w-3xl text-sm text-muted-foreground">
          Ouvrez une session pour obtenir le code de présence à dicter aux
          étudiants, puis suivez la promotion d&apos;un coup d&apos;œil.
        </p>
      </header>

      <Card>
        <CardHeader>
          <EnteteEtape
            numero={1}
            titre="Ouvrir une session"
            description="Le code n'existe que dans la réponse du serveur : il s'affiche ici dès l'ouverture, en grand et copiable."
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={soumettre} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="titre">Titre de la session</Label>
                <Input
                  id="titre"
                  name="titre"
                  required
                  value={titre}
                  onChange={(evenement) => setTitre(evenement.target.value)}
                  placeholder="Cours du 25 septembre"
                  className="h-10"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="promotionId">Identifiant de la promotion</Label>
                <Input
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
                  className="h-10"
                />
                <p className="text-xs text-muted-foreground">
                  La promotion de démonstration a l&apos;identifiant 1. Elle sert
                  aussi au tableau de bord ci-dessous.
                </p>
              </div>
            </div>

            <Button type="submit" disabled={chargement} className="h-10 px-4">
              {chargement ? (
                <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
              ) : (
                <CirclePlayIcon aria-hidden="true" />
              )}
              {chargement ? "Ouverture…" : "Ouvrir la session"}
            </Button>
          </form>

          {chargement && <Chargement libelle="Ouverture de la session en cours…" />}

          {erreur?.etape === "session" && (
            <AlerteErreur code={erreur.code} message={erreur.message} />
          )}
        </CardContent>
      </Card>

      {session && (
        <Card className="border-success/40 ring-success/25">
          <CardHeader>
            <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
              <div className="flex items-start gap-3">
                <KeyRoundIcon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden="true" />
                <div className="space-y-1">
                  <CardTitle className="text-success">Session ouverte — code à dicter</CardTitle>
                  <CardDescription>
                    Dictez ce code aux étudiants : il vaut pour cette session
                    uniquement.
                  </CardDescription>
                </div>
              </div>
              <ExpirationSession
                expirationAt={session.expirationAt}
                dateFormatee={formaterDate(session.expirationAt)}
              />
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <CodeSession code={session.code} />

            <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-xs text-muted-foreground">Identifiant</dt>
                <dd className="font-medium">{session.id}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Ouverte le</dt>
                <dd className="font-medium">{formaterDate(session.ouvertureAt)}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Expire le</dt>
                <dd className="font-medium">{formaterDate(session.expirationAt)}</dd>
              </div>
            </dl>

            {sessionCouranteCloturee && (
              <p className="text-sm font-medium text-muted-foreground">
                Session clôturée : les dépôts et les notes sont désormais refusés
                par le serveur (RG14).
              </p>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <EnteteEtape
            numero={2}
            titre="Sessions ouvertes depuis ce navigateur"
            description="Le code s'affiche à l'ouverture ; cette liste le retrouve après un rechargement de la page. Elle ne contient que les sessions ouvertes ici."
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="rounded-lg border border-info/30 bg-info/10 px-4 py-3 text-xs text-muted-foreground">
            Le contrat ne prévoit aucune opération qui relirait les sessions du
            serveur : cette liste est donc la seule trace disponible, et la
            validité d&apos;un code est décidée par le serveur — un étudiant qui
            saisit un code périmé reçoit <code className="font-mono">CODE_EXPIRE</code>.
            L&apos;heure d&apos;expiration est donc affichée telle quelle, sans
            être réinterprétée ici (F3).
          </p>

          {sessions.length === 0 ? (
            <EtatVide>Aucune session ouverte pour l&apos;instant.</EtatVide>
          ) : (
            <ul aria-label="Sessions ouvertes" className="flex flex-col gap-3">
              {sessions.map((enregistree) => (
                <li
                  key={enregistree.id}
                  className={cn(
                    "space-y-3 rounded-xl border p-4",
                    session?.id === enregistree.id
                      ? "border-success/40 bg-success/5"
                      : "border-border bg-card",
                  )}
                >
                  <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
                    <div className="space-y-1">
                      <p className="font-medium">{enregistree.titre}</p>
                      <p className="text-xs text-muted-foreground">
                        Session n°{enregistree.id} · promotion {enregistree.promotionId} ·
                        ouverte le {formaterDate(enregistree.ouvertureAt)} · expire le{" "}
                        {formaterDate(enregistree.expirationAt)}
                      </p>
                    </div>

                    {enregistree.cloturee ? (
                      // État renvoyé par le serveur à la clôture, jamais recalculé ici.
                      <Badge variant="outline" className="text-muted-foreground">
                        Clôturée — dépôts et notes gelés
                      </Badge>
                    ) : (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={cloture === enregistree.id}
                        onClick={() => void cloturerUneSession(enregistree.id)}
                      >
                        {cloture === enregistree.id && (
                          <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
                        )}
                        {cloture === enregistree.id ? "Clôture…" : "Clôturer"}
                      </Button>
                    )}
                  </div>

                  <CodeSession code={enregistree.code} taille="petit" />
                </li>
              ))}
            </ul>
          )}

          {erreur?.etape === "cloture" && (
            <AlerteErreur code={erreur.code} message={erreur.message} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <EnteteEtape
            numero={3}
            titre="Tableau de bord de la promotion"
            description="Une ligne par étudiant, y compris ceux qui n'ont encore rien fait. Une moyenne « — » signifie qu'aucune note n'a encore été reçue."
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button
              type="button"
              variant="outline"
              disabled={chargementTableau || promotionId.trim() === ""}
              onClick={() => void afficherLeTableau()}
              className="h-10 px-4"
            >
              {chargementTableau ? (
                <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
              ) : (
                <LayoutListIcon aria-hidden="true" />
              )}
              {chargementTableau ? "Chargement…" : "Afficher le tableau"}
            </Button>
            {tableau !== null && (
              <p className="text-xs text-muted-foreground">
                Promotion {promotionId} · {tableau.length} étudiant
                {tableau.length > 1 ? "s" : ""}
              </p>
            )}
          </div>

          {chargementTableau && (
            <div className="space-y-2">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          )}

          {tableau !== null && tableau.length === 0 && (
            <EtatVide>Cette promotion ne contient aucun étudiant.</EtatVide>
          )}

          {lignesAffichees !== null && lignesAffichees.length > 0 && (
            <Table aria-label="Tableau de bord">
              <TableHeader>
                <TableRow>
                  {COLONNES.map((colonne) => {
                    const actif = tri?.cle === colonne.cle;
                    return (
                      <TableHead
                        key={colonne.cle}
                        scope="col"
                        aria-sort={
                          actif ? (tri.sens === "asc" ? "ascending" : "descending") : "none"
                        }
                        className={cn(colonne.numerique && "text-right")}
                      >
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          aria-label={`Trier par ${colonne.libelle}`}
                          onClick={() => basculerTri(colonne.cle)}
                          className={cn(
                            "h-7 gap-1 px-2 text-xs font-medium",
                            colonne.numerique && "-mr-2 ml-auto",
                          )}
                        >
                          {colonne.libelle}
                          {actif ? (
                            tri.sens === "asc" ? (
                              <ArrowUpIcon aria-hidden="true" />
                            ) : (
                              <ArrowDownIcon aria-hidden="true" />
                            )
                          ) : (
                            <ArrowUpDownIcon className="opacity-40" aria-hidden="true" />
                          )}
                        </Button>
                      </TableHead>
                    );
                  })}
                </TableRow>
              </TableHeader>
              <TableBody>
                {lignesAffichees.map((ligne) => (
                  <TableRow key={ligne.etudiantId}>
                    <TableHead scope="row" className="font-medium">
                      {ligne.nom}
                    </TableHead>
                    <TableCell className="text-right tabular-nums">
                      {ligne.presences}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {ligne.exercicesDeposes}
                    </TableCell>
                    <TableCell className="text-right">
                      {ligne.moyenne === null ? (
                        <span className="text-muted-foreground">
                          {formaterMoyenne(ligne.moyenne)}
                        </span>
                      ) : (
                        <Badge variant="outline" className="font-mono">
                          {formaterMoyenne(ligne.moyenne)}
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {ligne.relecturesEnAttente > 0 ? (
                        <Badge variant="outline" className="border-warning/40 bg-warning/10 text-warning">
                          {ligne.relecturesEnAttente}
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">0</span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          {erreur?.etape === "tableau" && (
            <AlerteErreur code={erreur.code} message={erreur.message} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <EnteteEtape
            numero={4}
            titre="Ajouter une présence (EF10)"
            description="Pour un étudiant qui n'a pas pu saisir le code. Le serveur enregistre la présence avec la source « FORMATEUR », donc distinguable de celles marquées par les étudiants (RG12)."
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="sessionPresence">Session</Label>
            <select
              id="sessionPresence"
              value={sessionPresence === null ? "" : String(sessionPresence)}
              onChange={(evenement) => void choisirLaSession(evenement.target.value)}
              className="h-10 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm dark:bg-input/30"
            >
              <option value="">Choisissez une session…</option>
              {sessions.map((enregistree) => (
                <option key={enregistree.id} value={enregistree.id}>
                  {enregistree.titre} — session n°{enregistree.id}
                </option>
              ))}
            </select>
            <p className="text-xs text-muted-foreground">
              {sessions.length === 0
                ? "Ouvrez d'abord une session : cette liste ne connaît que celles ouvertes depuis ce navigateur."
                : "Seules les sessions ouvertes depuis ce navigateur sont proposées : c'est la seule lecture de session dont dispose cet écran. Aucun code n'est demandé, et la clôture ne bloque pas cet ajout : elle ne gèle que les dépôts et les notes."}
            </p>
          </div>

          {chargementEtudiants && <Chargement libelle="Chargement des étudiants…" />}

          {sessionPresence !== null && !chargementEtudiants && etudiants.length === 0 && (
            <EtatVide>Cette promotion ne contient aucun étudiant.</EtatVide>
          )}

          {etudiants.length > 0 && (
            <ul aria-label="Étudiants à marquer présents" className="flex flex-col gap-2">
              {etudiants.map((etudiant) => {
                const present = presents.get(etudiant.id);
                return (
                  <li
                    key={etudiant.id}
                    className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 rounded-lg border border-border px-3 py-2 text-sm"
                  >
                    <span className="flex items-center gap-2">
                      <UsersIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                      {etudiant.prenom} {etudiant.nom}
                    </span>

                    {present !== undefined ? (
                      // Confirmé par le serveur : 201 (créée) ou 409 (déjà présente).
                      <Badge
                        variant="outline"
                        className={cn(
                          present === "Déjà présent"
                            ? "border-info/30 bg-info/10 text-info"
                            : "border-success/30 bg-success/10 text-success",
                        )}
                      >
                        {present}
                      </Badge>
                    ) : confirmation === etudiant.id ? (
                      <span className="flex gap-2">
                        <Button
                          type="button"
                          size="sm"
                          disabled={ajout === etudiant.id}
                          onClick={() => void ajouterLaPresence(etudiant.id)}
                        >
                          {ajout === etudiant.id && (
                            <LoaderCircleIcon className="animate-spin" aria-hidden="true" />
                          )}
                          {ajout === etudiant.id ? "Enregistrement…" : "Confirmer"}
                        </Button>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={ajout === etudiant.id}
                          onClick={() => setConfirmation(null)}
                        >
                          Annuler
                        </Button>
                      </span>
                    ) : (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setConfirmation(etudiant.id)}
                      >
                        Marquer présent
                      </Button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}

          {erreur?.etape === "presence" && (
            <AlerteErreur code={erreur.code} message={erreur.message} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
