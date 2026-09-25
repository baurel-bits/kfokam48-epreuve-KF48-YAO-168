#!/usr/bin/env node
/**
 * Vérification statique du contrat DOM attendu par scripts/parcours-navigateur.mjs.
 *
 * Le parcours navigateur s'appuie sur des identifiants (`#titre`, `#code`,
 * `#note`…), des `aria-label` de listes et sur l'ordre des formulaires d'un
 * écran. Ce script relit le HTML pré-rendu par `next build` et contrôle que ce
 * contrat n'a pas bougé — sans lancer ni le backend ni le navigateur.
 *
 * Filet de sécurité rapide : il complète le parcours complet, il ne le remplace
 * pas (lui seul vérifie l'hydratation et les appels API réels).
 *
 * Usage : cd frontend && npm run build && cd .. && node scripts/verif-selecteurs-frontend.mjs
 */
import fs from "node:fs";
import path from "node:path";

const RACINE = path.dirname(new URL(import.meta.url).pathname);
const DOSSIER = path.join(RACINE, "..", "frontend", ".next", "server", "app");

const lire = (nom) => {
  const fichier = path.join(DOSSIER, `${nom}.html`);
  if (!fs.existsSync(fichier)) {
    throw new Error(`HTML introuvable : ${fichier} — lancez d'abord « npm run build » dans frontend/.`);
  }
  return fs.readFileSync(fichier, "utf8");
};

/** Retire les balises <script> : le HTML contient la charge RSC de Next, qui
 *  reprend le texte de tous les composants, y compris ceux non rendus. Les
 *  contrôles d'absence doivent donc se faire sur le balisage seul. */
const balisage = (html) => html.replace(/<script[\s\S]*?<\/script>/g, "");

/**
 * Concatène les bundles clients livrés par le build.
 *
 * Les écrans étant montés côté navigateur, les libellés des listes affichées
 * après chargement (`aria-label` des listes, en-têtes du tableau de bord) ne
 * figurent pas dans le HTML servi : on vérifie au moins qu'ils sont bien
 * présents dans le code livré.
 */
function bundlesClients() {
  const dossier = path.join(RACINE, "..", "frontend", ".next", "static", "chunks");
  const fichiers = fs.readdirSync(dossier, { withFileTypes: true, recursive: true }).filter((entree) => entree.isFile());
  return fichiers
    .map((entree) => fs.readFileSync(path.join(entree.parentPath ?? dossier, entree.name), "utf8"))
    .join("\n");
}

const bundles = bundlesClients();

const resultats = [];
const verifier = (description, ok, detail = "") => {
  resultats.push({ description, ok, detail });
  console.log(`  ${ok ? "OK   " : "ECHEC"} ${description}${detail ? ` — ${detail}` : ""}`);
};

const accueil = lire("index");
const formateur = balisage(lire("formateur"));
const etudiant = balisage(lire("etudiant"));
const relecteur = balisage(lire("relecteur"));

console.log("Accueil");
for (const chemin of ["/formateur", "/etudiant", "/relecteur"]) {
  verifier(`Lien vers ${chemin}`, accueil.includes(`href="${chemin}"`));
}
verifier(
  "Les trois espaces sont nommés",
  ["Espace formateur", "Espace étudiant", "Espace relecteur"].every((titre) => accueil.includes(titre)),
);

console.log("\nFormateur");
verifier("Champ #titre", formateur.includes('id="titre"'));
verifier("Champ #promotionId", formateur.includes('id="promotionId"'));
verifier(
  "Un seul <form> : le premier bouton submit reste celui de l'ouverture",
  (formateur.match(/<form/g) ?? []).length === 1,
  `${(formateur.match(/<form/g) ?? []).length} formulaire(s)`,
);
verifier('Sélecteur natif #sessionPresence (donc #sessionPresence.options exploitable)', /<select[^>]*id="sessionPresence"/.test(formateur));
verifier('Option vide du sélecteur de session', /<option value=""[^>]*>/.test(formateur));
verifier('Bouton « Afficher le tableau »', formateur.includes("Afficher le tableau"));
verifier(
  'Aucun role="alert" au premier rendu (une réussite ne doit pas ressembler à une erreur)',
  !formateur.includes('role="alert"'),
);
verifier("Aucun <table> avant chargement du tableau de bord", !formateur.includes("<table"));
verifier(
  'Le tableau de bord porte aria-label="Tableau de bord" (rendu après chargement)',
  bundles.includes("Tableau de bord"),
);
verifier(
  "Libellés des listes du domaine présents dans le code livré",
  ["Sessions ouvertes", "Étudiants à marquer présents", "Notes reçues", "Exercices à relire"].every(
    (libelle) => bundles.includes(libelle),
  ),
);

console.log("\nÉtudiant");
const formsEtudiant = (etudiant.match(/<form/g) ?? []).length;
verifier("Trois formulaires de premier niveau, dans l'ordre attendu", formsEtudiant === 3, `${formsEtudiant} formulaire(s)`);
for (const id of ["promotionId", "code", "sessionId", "lien"]) {
  verifier(`Champ #${id}`, etudiant.includes(`id="${id}"`));
}
const position = (id) => etudiant.indexOf(`id="${id}"`);
verifier(
  "Ordre des champs : #promotionId < #code < #sessionId < #lien",
  position("promotionId") < position("code") &&
    position("code") < position("sessionId") &&
    position("sessionId") < position("lien"),
);
verifier('Bouton « Afficher mes notes reçues »', etudiant.includes("Afficher mes notes reçues"));
verifier(
  'Aucun role="alert" au premier rendu',
  !etudiant.includes('role="alert"'),
);
verifier(
  "La liste d'étudiants est le premier <ul> de l'écran (donc `ul li button` = les étudiants)",
  etudiant.indexOf("<ul") < etudiant.indexOf("Afficher mes notes reçues"),
);

console.log("\nRelecteur");
const formsRelecteur = (relecteur.match(/<form/g) ?? []).length;
verifier("Deux formulaires au premier rendu, dans l'ordre attendu", formsRelecteur === 2, `${formsRelecteur} formulaire(s)`);
for (const id of ["promotionId", "note", "commentaire"]) {
  verifier(`Champ #${id}`, relecteur.includes(`id="${id}"`));
}
const baliseNote = (relecteur.match(/<input[^>]*id="note"[^>]*>/) ?? [""])[0];
verifier(
  "Note bornée : input numérique 0-20, pas de 1 (donc entier)",
  /type="number"/.test(baliseNote) &&
    /min="0"/.test(baliseNote) &&
    /max="20"/.test(baliseNote) &&
    /step="1"/.test(baliseNote),
  baliseNote.slice(0, 40) || "balise #note introuvable",
);
verifier(
  "Curseur de note borné 0-20 dans le même formulaire",
  /<input[^>]*type="range"[^>]*min="0"[^>]*max="20"/.test(relecteur) ||
    /<input[^>]*type="range"[^>]*max="20"[^>]*min="0"/.test(relecteur),
);
verifier("Commentaire en <textarea>", /<textarea[^>]*id="commentaire"/.test(relecteur));
verifier(
  "Le formulaire de correction est imbriqué dans sa section (aucun #noteCorrigee au premier rendu)",
  !relecteur.includes('id="noteCorrigee"'),
);
verifier(
  "Les valeurs de correction ne sont pas rendues avant d'être nécessaires",
  !relecteur.includes('id="commentaireCorrige"'),
);
verifier('Aucun role="alert" au premier rendu', !relecteur.includes('role="alert"'));

console.log("\nThème");
const htmlFormateur = lire("formateur");
verifier(
  "Script anti-FOUC de next-themes présent avant la peinture",
  /classList/.test(htmlFormateur) && !!htmlFormateur.match(/<script[^>]*>[^<]*classList/),
);
verifier(
  "Aucune classe de thème figée dans le HTML du serveur",
  !/<html[^>]*class="[^"]*\bdark\b/.test(htmlFormateur),
);
verifier("Bascule de thème nommée pour les lecteurs d'écran", htmlFormateur.includes("Changer de thème"));

const echecs = resultats.filter((r) => !r.ok);
console.log(`\n=== ${resultats.length - echecs.length}/${resultats.length} vérifications passées ===`);
if (echecs.length > 0) {
  console.log("Échecs :");
  echecs.forEach((echec) => console.log(`  - ${echec.description}${echec.detail ? ` (${echec.detail})` : ""}`));
}
process.exit(echecs.length > 0 ? 1 : 0);
