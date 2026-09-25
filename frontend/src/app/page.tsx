import Link from "next/link";
import {
  ArrowRightIcon,
  ClipboardListIcon,
  GraduationCapIcon,
  UserRoundCheckIcon,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";

/**
 * Point d'entrée : la racine n'affiche aucun écran métier, elle indique
 * simplement quels écrans existent et à quelle EFx ils répondent (F2).
 *
 * Composant serveur, purement statique : aucun appel API, aucune règle métier.
 */
const ECRANS = [
  {
    href: "/formateur",
    titre: "Espace formateur",
    icone: GraduationCapIcon,
    fonctionnalites: ["EF1 · Ouvrir une session", "EF9 · Tableau de bord", "EF10 · Présence manuelle"],
    description:
      "Ouvrez une session, obtenez le code à dicter aux étudiants (et retrouvez-le après rechargement), puis suivez la promotion par étudiant.",
  },
  {
    href: "/etudiant",
    titre: "Espace étudiant",
    icone: UserRoundCheckIcon,
    fonctionnalites: ["EF2 · Marquer sa présence", "EF3 · Déposer un exercice", "EF8 · Notes reçues"],
    description:
      "Marquez votre présence avec le code dicté, déposez le lien de votre exercice, puis consultez les notes reçues.",
  },
  {
    href: "/relecteur",
    titre: "Espace relecteur",
    icone: ClipboardListIcon,
    fonctionnalites: ["EF6 · Rendre une relecture", "EF7 · Corriger sa note"],
    description:
      "Retrouvez l'exercice qui vous est confié, notez-le de 0 à 20 et commentez-le, sans jamais connaître son auteur.",
  },
];

export default function Accueil() {
  return (
    <section className="space-y-8">
      <header className="space-y-2">
        <Badge variant="secondary">Épreuve KFOKAM48</Badge>
        <h1 className="font-heading text-3xl font-semibold tracking-tight">
          Présences, exercices et relectures par les pairs
        </h1>
        <p className="max-w-2xl text-sm text-muted-foreground">
          Choisissez un espace pour commencer. Chaque écran fait ce qu&apos;il
          annonce, et rien de plus : les moyennes, les compteurs et les statuts
          affichés viennent tous de l&apos;API.
        </p>
      </header>

      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {ECRANS.map((ecran) => {
          const Icone = ecran.icone;
          return (
            <li key={ecran.href}>
              <Link
                href={ecran.href}
                className="group flex h-full flex-col gap-3 rounded-xl bg-card p-5 text-card-foreground ring-1 ring-foreground/10 transition hover:ring-2 hover:ring-primary/60 focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
              >
                <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Icone className="size-4" aria-hidden="true" />
                </span>

                <span className="flex items-center gap-2 font-heading font-medium">
                  {ecran.titre}
                  <ArrowRightIcon
                    className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                    aria-hidden="true"
                  />
                </span>

                <span className="text-sm text-muted-foreground">{ecran.description}</span>

                <span className="mt-auto flex flex-wrap gap-1.5 pt-2">
                  {ecran.fonctionnalites.map((fonctionnalite) => (
                    <Badge key={fonctionnalite} variant="outline" className="font-normal text-muted-foreground">
                      {fonctionnalite}
                    </Badge>
                  ))}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
