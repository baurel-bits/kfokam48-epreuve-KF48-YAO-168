"use client";

import { useEffect, useState } from "react";
import { CheckIcon, ClockIcon, CopyIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Code de session, présenté en grand et copiable.
 *
 * Le code est la donnée la plus fragile de l'écran formateur : il est dicté à
 * voix haute, donc affiché en gros caractères monospace, et copiable en un clic
 * pour être collé dans un chat. La copie est un simple confort de saisie ; si
 * le presse-papiers est refusé par le navigateur, le code reste lisible et le
 * bouton l'annonce sans casser l'écran.
 */
export function CodeSession({
  code,
  taille = "grand",
  className,
}: {
  code: string;
  /** `grand` dans l'encart d'ouverture, `petit` pour la liste des sessions. */
  taille?: "grand" | "petit";
  className?: string;
}) {
  const [copie, setCopie] = useState(false);

  // Le message « Copié » s'efface tout seul : l'état ne doit pas mentir si
  // l'utilisateur relit l'écran une minute plus tard.
  useEffect(() => {
    if (!copie) {
      return;
    }
    const minuteur = setTimeout(() => setCopie(false), 2000);
    return () => clearTimeout(minuteur);
  }, [copie]);

  async function copier() {
    try {
      await navigator.clipboard.writeText(code);
      setCopie(true);
    } catch {
      // Presse-papiers indisponible (contexte non sécurisé) : le code reste
      // affiché à l'écran, rien d'autre à faire.
      setCopie(false);
    }
  }

  return (
    <div className={cn("flex flex-wrap items-center gap-x-3 gap-y-2", className)}>
      <p
        className={cn(
          "font-mono font-semibold tracking-[0.25em] text-foreground select-all",
          taille === "grand" ? "text-3xl sm:text-4xl" : "text-lg",
        )}
      >
        {code}
      </p>
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => void copier()}
        aria-label={`Copier le code de session ${code}`}
      >
        {copie ? <CheckIcon aria-hidden="true" /> : <CopyIcon aria-hidden="true" />}
        {copie ? "Copié" : "Copier"}
      </Button>
    </div>
  );
}

/**
 * Indication d'expiration d'un code (« expire dans X min »).
 *
 * Le décompte est purement indicatif : il est calculé à partir de l'instant
 * d'expiration renvoyé par le serveur, et c'est **le serveur** qui reste seul
 * juge de la validité d'un code (un étudiant qui saisit un code périmé reçoit
 * `CODE_EXPIRE`). Aucune décision n'est prise ici.
 *
 * Avant le montage, le décompte n'existe pas : le rendu serveur se contente de
 * la date d'expiration, ce qui évite toute divergence entre les deux rendus.
 */
export function ExpirationSession({
  expirationAt,
  dateFormatee,
}: {
  expirationAt: string;
  /** Date lisible préparée par l'écran appelant (même format partout). */
  dateFormatee: string;
}) {
  const [restant, setRestant] = useState<number | null>(null);

  useEffect(() => {
    const cible = Date.parse(expirationAt);
    if (Number.isNaN(cible)) {
      return;
    }
    const tic = () => setRestant(cible - Date.now());
    tic();
    const minuteur = setInterval(tic, 1000);
    return () => clearInterval(minuteur);
  }, [expirationAt]);

  if (restant === null) {
    return (
      <Badge variant="outline" className="gap-1.5 text-muted-foreground">
        <ClockIcon aria-hidden="true" />
        Expire le {dateFormatee}
      </Badge>
    );
  }

  if (restant <= 0) {
    return (
      <Badge variant="outline" className="gap-1.5 border-warning/40 bg-warning/10 text-warning">
        <ClockIcon aria-hidden="true" />
        Expiration dépassée — le serveur décide seul
      </Badge>
    );
  }

  const minutes = Math.floor(restant / 60_000);
  const secondes = Math.floor((restant % 60_000) / 1000);
  const texte =
    minutes === 0
      ? "Expire dans moins d'une minute"
      : `Expire dans ${minutes} min ${String(secondes).padStart(2, "0")} s`;

  return (
    <Badge
      variant="outline"
      className={cn(
        "gap-1.5",
        minutes < 2
          ? "border-warning/40 bg-warning/10 text-warning"
          : "border-success/40 bg-success/10 text-success",
      )}
    >
      <ClockIcon aria-hidden="true" />
      {texte}
    </Badge>
  );
}
