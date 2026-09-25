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
      "EF1 — ouvrir une session et obtenir le code de présence à dicter aux étudiants.",
  },
  {
    href: "/etudiant",
    titre: "Espace étudiant",
    description:
      "EF2 / EF3 — marquer sa présence avec le code de la session, puis déposer le lien de son exercice.",
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

      {/* Écran attendu par F2 mais rattaché à des issues non encore traitées :
          signalé ici sans lien, pour ne pas mener à une page inexistante. */}
      <div className="rounded-lg border border-dashed border-slate-300 p-5">
        <h2 className="font-medium text-slate-500">Espace relecteur</h2>
        <p className="mt-1 text-sm text-slate-500">
          EF6 / EF8 — consulter l&apos;exercice confié, le noter et le commenter.
          Écran prévu par les issues #12 et #13, pas encore livré.
        </p>
      </div>
    </section>
  );
}
