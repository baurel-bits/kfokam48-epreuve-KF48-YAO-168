import Link from "next/link";

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
    description:
      "EF1 / EF9 — ouvrir une session, obtenir le code de présence à dicter aux étudiants, puis suivre la promotion par étudiant.",
  },
  {
    href: "/etudiant",
    titre: "Espace étudiant",
    description:
      "EF2 / EF3 / EF8 — marquer sa présence, déposer son exercice, puis consulter les notes reçues.",
  },
  {
    href: "/relecteur",
    titre: "Espace relecteur",
    description:
      "EF6 — retrouver l'exercice qui vous est confié, le noter et le commenter.",
  },
];

export default function Accueil() {
  return (
    <section className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">KFOKAM48</h1>
        <p className="mt-1 text-sm text-slate-600">
          Prise de présence, dépôt d&apos;exercices et relecture par les pairs —
          choisissez un espace pour commencer.
        </p>
      </header>

      <ul className="grid gap-4 sm:grid-cols-2">
        {ECRANS.map((ecran) => (
          <li key={ecran.href}>
            <Link
              href={ecran.href}
              className="block h-full rounded-lg border border-slate-200 bg-white p-5 transition hover:border-slate-400"
            >
              <h2 className="font-medium">{ecran.titre}</h2>
              <p className="mt-1 text-sm text-slate-600">{ecran.description}</p>
            </Link>
          </li>
        ))}
      </ul>

    </section>
  );
}
