"use client";

import { useEffect, useState } from "react";
import { useTheme } from "next-themes";
import { MonitorIcon, MoonIcon, SunIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

/** Les trois choix de thème proposés, dans l'ordre d'affichage. */
const CHOIX = [
  { valeur: "light", libelle: "Clair", icone: SunIcon },
  { valeur: "dark", libelle: "Sombre", icone: MoonIcon },
  { valeur: "system", libelle: "Système", icone: MonitorIcon },
] as const;

/**
 * Bascule clair / sombre / système, présente sur les trois écrans (via l'en-tête).
 *
 * Le choix est persisté par next-themes. Tant que le thème résolu n'est pas connu
 * côté client, le rendu reste neutre (aucune icône d'état) : c'est ce qui évite
 * une divergence entre le HTML du serveur et celui du navigateur.
 */
export function BasculeTheme() {
  const { theme, setTheme } = useTheme();
  const [monte, setMonte] = useState(false);

  // Le thème n'est connu qu'après montage : le lire avant ferait diverger le
  // rendu serveur (thème inconnu) du rendu navigateur. C'est le motif recommandé
  // par next-themes ; l'effet ne s'exécute qu'une fois et n'entraîne aucun
  // rendu en cascade.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMonte(true);
  }, []);

  const actif = monte ? theme : undefined;
  const IconeCourante = CHOIX.find((choix) => choix.valeur === actif)?.icone ?? MonitorIcon;

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label="Changer de thème (clair, sombre ou système)"
          className="size-9"
        >
          <IconeCourante aria-hidden="true" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="min-w-44">
        <DropdownMenuLabel>Apparence</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {/* Groupe de boutons radio : l'état sélectionné est annoncé par
            `aria-checked`, et la coche est fournie par le composant. */}
        <DropdownMenuRadioGroup value={actif ?? ""} onValueChange={setTheme}>
          {CHOIX.map(({ valeur, libelle, icone: Icone }) => (
            <DropdownMenuRadioItem key={valeur} value={valeur} data-theme-choice={valeur}>
              <Icone aria-hidden="true" />
              <span>{libelle}</span>
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
