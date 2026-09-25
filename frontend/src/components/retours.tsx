import type { ReactNode } from "react";
import { CheckCircle2Icon, LoaderCircleIcon, TriangleAlertIcon } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

/**
 * Composants de retour partagés par les trois écrans.
 *
 * Un seul vocabulaire visuel pour toute l'application : alerte d'erreur, bloc de
 * succès, chargement et état vide se ressemblent d'un écran à l'autre. Tous
 * n'affichent que ce qu'ils reçoivent — aucune règle métier, aucun appel API.
 *
 * Point d'accessibilité : seule l'**erreur** porte `role="alert"` (annonce
 * immédiate aux lecteurs d'écran et repère unique pour les tests), les retours
 * de succès et les chargements restent en `role="status"` (annonce polie).
 */

/** Intention d'affichage déduite du code d'erreur du contrat (présentation seule). */
export interface TonErreur {
  /** Libellé en français, sans jargon, affiché à côté du code brut. */
  libelle: string;
  /** Variante de couleur du badge : erreur, avertissement ou information. */
  variante: "erreur" | "avertissement" | "information";
}

/**
 * Traduit un code d'erreur renvoyé par l'API en intention d'affichage.
 *
 * Aucune règle de gestion n'est reproduite ici : le code reçu n'est jamais
 * réinterprété pour décider quoi que ce soit, il est seulement coloré et
 * expliqué. Un code inconnu reste une erreur, avec son message du serveur.
 */
export function tonDuCode(code: string): TonErreur {
  switch (code) {
    case "CODE_EXPIRE":
    case "SESSION_CLOTUREE":
    case "TROP_DE_TENTATIVES":
    case "RELECTURE_DEJA_RENDUE":
      return { libelle: "Action refusée", variante: "avertissement" };
    case "DEJA_PRESENT":
    case "EXERCICE_DEJA_DEPOSE":
    case "RELECTURE_COMMENCEE":
      return { libelle: "Déjà enregistré", variante: "information" };
    default:
      return { libelle: "Erreur", variante: "erreur" };
  }
}

const CLASSES_VARIANTE: Record<TonErreur["variante"], string> = {
  erreur: "border-destructive/30 bg-destructive/10 text-destructive",
  avertissement: "border-warning/35 bg-warning/10 text-warning",
  information: "border-info/30 bg-info/10 text-info",
};

/**
 * Alerte d'erreur : le code brut du contrat (en monospace, identifiable d'un
 * coup d'œil) puis son message tel que le serveur l'a renvoyé. `role="alert"`
 * vient du composant `Alert`.
 */
export function AlerteErreur({ code, message }: { code: string; message: string }) {
  const ton = tonDuCode(code);
  return (
    <Alert variant="destructive" className={cn("px-4 py-3", CLASSES_VARIANTE[ton.variante])}>
      <TriangleAlertIcon aria-hidden="true" />
      <AlertTitle className="flex flex-wrap items-center gap-2">
        <Badge variant="outline" className="border-current/40 font-mono text-[0.7rem] tracking-wide">
          {code}
        </Badge>
        <span>{ton.libelle}</span>
      </AlertTitle>
      <AlertDescription className="mt-1 text-current/90">{message}</AlertDescription>
    </Alert>
  );
}

/**
 * Retour de succès — volontairement **sans** `role="alert"` : une réussite
 * n'interrompt pas la lecture. Le titre et le détail sont libres, pour que
 * chaque écran garde les phrases exactes qu'il affichait.
 */
export function BlocSucces({
  titre,
  children,
  className,
}: {
  titre: string;
  children?: ReactNode;
  className?: string;
}) {
  return (
    <div
      role="status"
      className={cn(
        "flex items-start gap-3 rounded-xl border border-success/30 bg-success/10 px-4 py-3",
        className,
      )}
    >
      <CheckCircle2Icon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden="true" />
      <div className="min-w-0 flex-1 text-sm">
        <p className="font-medium text-success">{titre}</p>
        {children !== undefined && <div className="mt-1 text-foreground">{children}</div>}
      </div>
    </div>
  );
}

/**
 * Indicateur de chargement : même gabarit partout (icône qui tourne + libellé
 * explicite), annoncé poliment. Les squelettes sont préférés là où la forme de
 * la donnée attendue est connue (tableau de bord).
 */
export function Chargement({ libelle, className }: { libelle: string; className?: string }) {
  return (
    <p
      role="status"
      className={cn("flex items-center gap-2 text-sm text-muted-foreground", className)}
    >
      <LoaderCircleIcon className="size-4 animate-spin" aria-hidden="true" />
      {libelle}
    </p>
  );
}

/** État vide : cadre discret, pour dire qu'il n'y a rien à afficher — jamais un blanc. */
export function EtatVide({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <p
      className={cn(
        "rounded-lg border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground",
        className,
      )}
    >
      {children}
    </p>
  );
}
