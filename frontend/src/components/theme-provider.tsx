"use client";

import { ThemeProvider as FournisseurThemes } from "next-themes";
import type { ComponentProps } from "react";

/**
 * Fournisseur de thème (next-themes).
 *
 * `attribute="class"` pose la classe `dark` sur `<html>` : les jetons de
 * `globals.css` basculent alors tous seuls, sans qu'aucun écran n'ait à savoir
 * quel thème est actif. Le script injecté par next-themes applique la classe
 * avant la première peinture, ce qui évite le flash de mauvais thème (FOUC).
 */
export function ThemeProvider(props: ComponentProps<typeof FournisseurThemes>) {
  return <FournisseurThemes {...props} />;
}
