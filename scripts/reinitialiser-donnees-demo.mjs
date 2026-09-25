#!/usr/bin/env node
/**
 * Remise à zéro des données de démonstration de `exam_db`.
 *
 * Les données de référence (promotion et étudiants, créés par la migration
 * `V2__seed_donnees_demo.sql`) sont toujours conservées : seules les données
 * d'usage sont supprimées — sessions, présences, exercices, relectures,
 * historique de correction et compteurs de tentatives.
 *
 * Par défaut, le script est **prudent** : il ne cible que les sessions créées
 * par les vérifications automatisées (parcours navigateur, tests HTTP manuels),
 * jamais celles ouvertes à la main dans l'interface. Pour vider l'ensemble des
 * données d'usage, utiliser `--tout`.
 *
 * Une sauvegarde est faite avant toute suppression (sauf `--sans-sauvegarde`),
 * dans `backup/`, dossier ignoré par Git.
 *
 * Prérequis : PostgreSQL accessible, soit par le conteneur Docker
 * `exam-postgres`, soit par un client `psql` local.
 *
 * Usage :
 *   node scripts/reinitialiser-donnees-demo.mjs              # runs automatisés
 *   node scripts/reinitialiser-donnees-demo.mjs --tout       # toutes les données d'usage
 *   node scripts/reinitialiser-donnees-demo.mjs --apercu     # ne supprime rien
 *   node scripts/reinitialiser-donnees-demo.mjs --aide
 *
 * Variables d'environnement :
 *   DB_CONTENEUR  conteneur Docker (défaut exam-postgres)
 *   DB_NOM        base de données  (défaut exam_db)
 *   DB_UTILISATEUR / PGPASSWORD    identifiants PostgreSQL
 */
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const CONTENEUR = process.env.DB_CONTENEUR ?? "exam-postgres";
const BASE = process.env.DB_NOM ?? "exam_db";
const UTILISATEUR = process.env.DB_UTILISATEUR ?? "postgres";
const MOT_DE_PASSE = process.env.PGPASSWORD ?? "postgres";

/** Titres des sessions produites par les vérifications automatisées. */
const MOTIFS_PAR_DEFAUT = ["Verif E2E", "Parcours navigateur %", "Cours EF5 %"];

/** Tables d'usage, dans l'ordre où elles peuvent être vidées (dépendances d'abord). */
const TABLES = [
  "correction_relecture",
  "relecture",
  "exercice",
  "presence",
  "tentative_saisie",
  "session",
];

const options = {
  tout: process.argv.includes("--tout"),
  apercu: process.argv.includes("--apercu") || process.argv.includes("--dry-run"),
  sansSauvegarde: process.argv.includes("--sans-sauvegarde"),
  aide: process.argv.includes("--aide") || process.argv.includes("--help"),
  motifs: MOTIFS_PAR_DEFAUT,
};

if (options.aide) {
  console.log(fs.readFileSync(new URL(import.meta.url), "utf8").split("*/")[0]);
  process.exit(0);
}

const motifArgument = process.argv.find((a) => a.startsWith("--motifs="));
if (motifArgument) {
  options.motifs = motifArgument.slice("--motifs=".length).split(",").map((m) => m.trim());
}

// ---------------------------------------------------------------------------
// Accès à PostgreSQL : conteneur Docker si disponible, sinon client local.
// ---------------------------------------------------------------------------

function executer(binaire, arguments_) {
  return spawnSync(binaire, arguments_, {
    encoding: "utf8",
    env: { ...process.env, PGPASSWORD: MOT_DE_PASSE },
  });
}

let mode = null;

function detecterMode() {
  const docker = executer("docker", ["exec", CONTENEUR, "psql", "-U", UTILISATEUR, "-d", BASE, "-tAc", "SELECT 1"]);
  if (docker.status === 0 && docker.stdout.trim() === "1") {
    return "docker";
  }
  const local = executer("psql", ["-h", "localhost", "-U", UTILISATEUR, "-d", BASE, "-tAc", "SELECT 1"]);
  if (local.status === 0 && local.stdout.trim() === "1") {
    return "psql";
  }
  return null;
}

/** Exécute du SQL et renvoie la sortie brute (non triée), ou lève une erreur. */
function sql(requete, { tuplesSeulement = true, psql = false } = {}) {
  const args =
    mode === "docker"
      ? ["exec", "-e", `PGPASSWORD=${MOT_DE_PASSE}`, CONTENEUR, "psql", "-v", "ON_ERROR_STOP=1", "-U", UTILISATEUR, "-d", BASE]
      : ["-h", "localhost", "-U", UTILISATEUR, "-d", BASE];
  const binaire = mode === "docker" ? "docker" : "psql";

  if (tuplesSeulement) args.push("-tA");
  if (psql) args.push("--no-psqlrc");
  args.push("-c", requete);

  const resultat = executer(binaire, args);
  if (resultat.status !== 0) {
    throw new Error(`Échec SQL (${binaire}) : ${(resultat.stderr || resultat.stdout || "").trim()}`);
  }
  return resultat.stdout.trim();
}

/** Lignes d'une requête mono-colonne, séparées par des sauts de ligne. */
function colonne(requete) {
  return sql(requete)
    .split("\n")
    .map((ligne) => ligne.trim())
    .filter((ligne) => ligne !== "");
}

/** Sauvegarde les tables d'usage dans backup/ (aucune donnée n'est encore supprimée). */
function sauvegarder() {
  fs.mkdirSync("backup", { recursive: true });
  const horodatage = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
  const fichier = path.join("backup", `exam_db-${horodatage}.sql`);

  const args =
    mode === "docker"
      ? ["exec", "-e", `PGPASSWORD=${MOT_DE_PASSE}`, CONTENEUR, "pg_dump", "-U", UTILISATEUR, "-d", BASE]
      : ["-h", "localhost", "-U", UTILISATEUR, "-d", BASE];
  TABLES.forEach((table) => args.push("-t", table));

  const binaire = mode === "docker" ? "docker" : "pg_dump";
  const resultat = spawnSync(binaire, args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });

  if (resultat.status !== 0) {
    throw new Error(`Échec de la sauvegarde : ${(resultat.stderr || "").trim()}`);
  }
  fs.writeFileSync(fichier, resultat.stdout);
  return fichier;
}

// ---------------------------------------------------------------------------
// Sélection des données à supprimer
// ---------------------------------------------------------------------------

const PREDICAT_SESSIONS = options.tout
  ? "TRUE"
  : options.motifs.map((motif) => (motif.includes("%") ? `titre LIKE '${motif}'` : `titre = '${motif}'`)).join(" OR ");

const DEVISES = `
CREATE TEMP TABLE a_supprimer AS SELECT id FROM session WHERE ${PREDICAT_SESSIONS};`;

const SUPPRESSION = `
${DEVISES}
DELETE FROM correction_relecture WHERE relecture_id IN
    (SELECT r.id FROM relecture r JOIN exercice e ON e.id = r.exercice_id
     WHERE e.session_id IN (SELECT id FROM a_supprimer));
DELETE FROM relecture WHERE exercice_id IN
    (SELECT id FROM exercice WHERE session_id IN (SELECT id FROM a_supprimer));
DELETE FROM exercice WHERE session_id IN (SELECT id FROM a_supprimer);
DELETE FROM presence WHERE session_id IN (SELECT id FROM a_supprimer);
DELETE FROM tentative_saisie WHERE session_id IS NOT NULL AND session_id IN (SELECT id FROM a_supprimer);
${options.tout ? "DELETE FROM tentative_saisie;" : "DELETE FROM tentative_saisie WHERE session_id IS NULL;"}
DELETE FROM session WHERE id IN (SELECT id FROM a_supprimer);
${options.tout
  ? TABLES.map((table) => `ALTER TABLE ${table} ALTER COLUMN id RESTART WITH 1;`).join("\n")
  : ""}`;

function etatDesTables() {
  return colonne(
    TABLES.map((table) => `SELECT '${table} = ' || count(*) FROM ${table}`).join(" UNION ALL "),
  );
}

function sessionsCiblees() {
  return colonne(`SELECT id || ' | ' || titre || ' | ' || code FROM session WHERE ${PREDICAT_SESSIONS} ORDER BY id`);
}

// ---------------------------------------------------------------------------

console.log("Remise à zéro des données de démonstration");
console.log(`  portée : ${options.tout ? "TOUTES les données d'usage" : `runs automatisés (${options.motifs.join(", ")})`}`);

mode = detecterMode();
if (mode === null) {
  console.error(
    `\nAucun accès à PostgreSQL : le conteneur « ${CONTENEUR} » n'est pas joignable et le client « psql » local a échoué.` +
      "\nDémarrez le conteneur, ou renseignez DB_CONTENEUR / DB_NOM / DB_UTILISATEUR / PGPASSWORD.",
  );
  process.exit(1);
}
console.log(`  accès  : ${mode === "docker" ? `docker exec ${CONTENEUR}` : "psql local"} · base ${BASE}`);

const avant = etatDesTables();
const cibles = sessionsCiblees();

if (cibles.length === 0) {
  console.log("\nAucune session à cibler : rien à supprimer, aucune sauvegarde n'est nécessaire.");
  console.log(avant.map((ligne) => `  ${ligne}`).join("\n"));
  process.exit(0);
}

console.log(`\n${cibles.length} session(s) ciblée(s) :`);
cibles.forEach((ligne) => console.log(`  ${ligne}`));

if (options.apercu) {
  console.log("\nAperçu : aucune suppression effectuée (retirez --apercu pour appliquer).");
  process.exit(0);
}

if (options.sansSauvegarde) {
  console.log("\nSauvegarde désactivée (--sans-sauvegarde).");
} else {
  const fichier = sauvegarder();
  console.log(`\nSauvegarde : ${fichier} (${(fs.statSync(fichier).size / 1024).toFixed(0)} Ko)`);
}

sql(`BEGIN; ${SUPPRESSION} COMMIT;`);
console.log("Suppression effectuée.");

console.log("\nAvant :");
avant.forEach((ligne) => console.log(`  ${ligne}`));
console.log("Après :");
etatDesTables().forEach((ligne) => console.log(`  ${ligne}`));

if (!options.tout) {
  console.log(
    "\nLes identifiants ne sont pas remis à zéro : d'autres sessions subsistent, et une séquence" +
      "\nramenée à 1 produirait une collision de clé primaire. Utilisez --tout pour repartir de 1.",
  );
}
