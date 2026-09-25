"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { GraduationCapIcon } from "lucide-react";
import { BasculeTheme } from "@/components/bascule-theme";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/core/config";

/** Espaces proposés dans l'en-tête, dans l'ordre du cahier des charges (F2). */
const ONGLETS = [
  { href: ROUTES.FORMATEUR, libelle: "Formateur" },
  { href: ROUTES.ETUDIANT, libelle: "Étudiant" },
  { href: ROUTES.RELECTEUR, libelle: "Relecteur" },
] as const;

/**
 * En-tête commun aux trois écrans : identité du projet, navigation entre les
 * espaces et bascule de thème. Purement présentationnel : il n'appelle aucune
 * API et ne connaît aucune règle métier.
 */
export function EnteteApplication() {
  const chemin = usePathname();

  return (
    <header className="sticky top-0 z-40 border-b border-border/70 bg-background/85 backdrop-blur supports-backdrop-filter:bg-background/70">
      <div className="mx-auto flex w-full max-w-5xl flex-wrap items-center justify-between gap-x-4 gap-y-2 px-4 py-3 sm:px-6">
        <Link
          href="/"
          className="flex items-center gap-2 text-sm font-semibold tracking-tight"
        >
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <GraduationCapIcon className="size-4" aria-hidden="true" />
          </span>
          <span>
            KFOKAM48
            <span className="ml-2 hidden text-xs font-normal text-muted-foreground sm:inline">
              Présences · Exercices · Relectures
            </span>
          </span>
        </Link>

        <div className="flex items-center gap-1">
          <nav aria-label="Espaces de travail" className="flex items-center gap-1">
            {ONGLETS.map((onglet) => {
              const actif = chemin === onglet.href;
              return (
                <Link
                  key={onglet.href}
                  href={onglet.href}
                  aria-current={actif ? "page" : undefined}
                  className={cn(
                    "rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
                    "focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none",
                    actif
                      ? "bg-secondary text-secondary-foreground"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  )}
                >
                  {onglet.libelle}
                </Link>
              );
            })}
          </nav>
          <BasculeTheme />
        </div>
      </div>
    </header>
  );
}
