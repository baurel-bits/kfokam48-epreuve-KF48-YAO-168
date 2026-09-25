import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";

/**
 * Style de carte du composant `Card` de shadcn/ui, réutilisable tel quel sur un
 * élément qui doit rester un `<form>` (les formulaires des écrans étudiant et
 * relecteur doivent rester frères et dans l'ordre pour que leur sélecteur soit
 * stable). Même rendu, même hiérarchie, aucune boîte intermédiaire.
 */
export const CLASSE_CARTE =
  "flex flex-col gap-4 rounded-xl bg-card p-5 text-sm text-card-foreground ring-1 ring-foreground/10";

/**
 * En-tête d'une étape d'écran : numéro, titre et phrase d'explication.
 *
 * Les trois écrans partagent exactement le même gabarit et la même typographie,
 * ce qui donne à la succession des étapes la même lecture partout.
 */
export function EnteteEtape({
  numero,
  titre,
  description,
}: {
  numero: number;
  titre: string;
  description?: ReactNode;
}) {
  return (
    <div className="flex items-start gap-3">
      <Badge
        variant="secondary"
        className="mt-0.5 size-6 shrink-0 justify-center rounded-full p-0 text-xs font-semibold"
        aria-hidden="true"
      >
        {numero}
      </Badge>
      <div className="space-y-1">
        <p className="font-heading text-base leading-snug font-medium">{titre}</p>
        {description !== undefined && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
    </div>
  );
}
