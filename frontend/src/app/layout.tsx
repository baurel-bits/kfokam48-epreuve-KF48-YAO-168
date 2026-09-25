import type { Metadata } from "next";
import "./globals.css";
import { EnteteApplication } from "@/components/entete-application";
import { ThemeProvider } from "@/components/theme-provider";

export const metadata: Metadata = {
  title: "KFOKAM48 — Présences, exercices et relectures",
  description:
    "Prise de présence, dépôt d'exercices et relecture par les pairs (épreuve KFOKAM48).",
};

/**
 * Coquille de l'application : thème (clair / sombre / système), en-tête commun
 * et conteneur de page. Les trois écrans imposés se contentent de leur contenu.
 *
 * `suppressHydrationWarning` est nécessaire sur `<html>` : next-themes y pose la
 * classe du thème avant l'hydratation, ce qui évite le flash de mauvais thème
 * (FOUC). Le reste de l'arbre n'est pas concerné.
 */
export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fr" suppressHydrationWarning>
      <body className="antialiased">
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <div className="flex min-h-dvh flex-col bg-background text-foreground">
            <EnteteApplication />
            <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-6 sm:px-6 sm:py-10">
              {children}
            </main>
            <footer className="border-t border-border/70 px-4 py-6 sm:px-6">
              <p className="mx-auto w-full max-w-5xl text-xs text-muted-foreground">
                Épreuve KFOKAM48 — prise de présence, dépôt d&apos;exercices et
                relecture par les pairs. Moyennes et compteurs affichés
                proviennent du serveur.
              </p>
            </footer>
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
