import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "KFOKAM48 — Présences, exercices et relectures",
  description:
    "Prise de présence, dépôt d'exercices et relecture par les pairs (épreuve KFOKAM48).",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fr">
      <body className="antialiased">
        <main className="mx-auto w-full max-w-4xl px-4 py-8">{children}</main>
      </body>
    </html>
  );
}
