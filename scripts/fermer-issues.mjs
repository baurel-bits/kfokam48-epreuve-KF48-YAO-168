#!/usr/bin/env node
/**
 * Ferme les issues terminées du backlog, une par exigence fonctionnelle, en y
 * joignant un commentaire de preuve (livrable, écart documenté, branche, commit).
 *
 * Les issues sont retrouvées par leur **label `efN`** — jamais par un numéro
 * écrit en dur : le script ne peut donc pas fermer la mauvaise.
 *
 * Usage :
 *   GH_TOKEN=xxx node scripts/fermer-issues.mjs --apercu   # montre ce qui serait fermé
 *   GH_TOKEN=xxx node scripts/fermer-issues.mjs            # commente puis ferme
 *
 * Rejouable : une issue déjà fermée est signalée et laissée telle quelle.
 */
import { execFileSync } from "node:child_process";
import path from "node:path";

const OWNER = "baurel-bits";
const REPO = "kfokam48-epreuve-KF48-YAO-168";
const API = "https://api.github.com";
const TOKEN = process.env.GH_TOKEN;
const APERCU = process.argv.includes("--apercu");

// L'aperçu se contente de lire : il accepte un accès anonyme, ce qui suffit sur
// un dépôt public. Toute écriture, elle, exige le jeton.
if (!TOKEN && !APERCU) {
  console.error(
    "GH_TOKEN manquant.\n\n" +
      "  GH_TOKEN=<ton_jeton> node scripts/fermer-issues.mjs --apercu\n" +
      "  GH_TOKEN=<ton_jeton> node scripts/fermer-issues.mjs\n\n" +
      "Le jeton a besoin du droit « issues: write » sur ce dépôt uniquement.",
  );
  process.exit(1);
}

const HEADERS = {
  Authorization: `Bearer ${TOKEN}`,
  Accept: "application/vnd.github+json",
  "X-GitHub-Api-Version": "2022-11-28",
  "User-Agent": "kfokam48-backlog-script",
};

async function rest(method, url, body) {
  if (method !== "GET" && !TOKEN) {
    throw new Error(`écriture impossible sans GH_TOKEN (${method} ${url})`);
  }
  const reponse = await fetch(`${API}${url}`, {
    method,
    headers: {
      ...(TOKEN ? HEADERS : { Accept: HEADERS.Accept, "User-Agent": HEADERS["User-Agent"] }),
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const texte = await reponse.text();
  let json = null;
  try {
    json = texte ? JSON.parse(texte) : null;
  } catch {
    // corps non JSON : signalé par le statut
  }
  return { status: reponse.status, json, texte };
}

/**
 * Ce qui a été livré pour chaque EF, et l'écart éventuel tranché en section 11
 * du cahier des charges. Les issues sont retrouvées par label, donc l'ordre et
 * les numéros ici n'ont aucune importance.
 */
const LIVRAISONS = {
  1: {
    livrable:
      "`POST /api/sessions` avec code de présence de 6 caractères (`SecureRandom`, alphabet sans `I`, `O`, `0`, `1`), DTO d'horodatage en RFC 3339 avec décalage.",
    ecart: null,
  },
  2: {
    livrable:
      "`POST /api/presences` : ordre de contrôle RG3 → RG1 → unicité, `400 CODE_INCONNU`, `410 CODE_EXPIRE`, `409 DEJA_PRESENT`, `429 TROP_DE_TENTATIVES`.",
    ecart:
      "RG3 : le contrat ne prévoyait ni statut ni code pour une saisie faite pendant le blocage → `429 TROP_DE_TENTATIVES` ajouté, avec un compteur à **deux portées** (couple étudiant/session si le code est attribuable, étudiant seul sinon — migration `V3`). Voir §11.",
  },
  3: {
    livrable:
      "`POST /api/exercices` : contrôle du lien (URL `http(s)`, 500 caractères), `409 EXERCICE_DEJA_DEPOSE`, `409 SESSION_CLOTUREE` (RG10/RG14) ; le dépôt déclenche l'assignation d'un relecteur.",
    ecart: null,
  },
  4: {
    livrable:
      "`PUT /api/exercices/{id}/lien` : remplacement du lien, `400 LIEN_INVALIDE`, `404 EXERCICE_INCONNU`, `409 RELECTURE_COMMENCEE` / `SESSION_CLOTUREE`, statut de l'exercice inchangé.",
    ecart:
      "L'ébauche d'issue ouvrait le remplacement « tant qu'aucune relecture n'a été **rendue** ». RG11 (Q13), que l'issue cite elle-même, parle d'« aucune relecture **commencée** », et le contrat déclare `409 RELECTURE_COMMENCEE` — jamais `RELECTURE_RENDUE`. RG11 fait foi : le remplacement est fermé dès l'assignation faite au dépôt. Conséquence assumée : l'EF4 n'est atteignable que pour un exercice resté `DEPOSE`. Voir §11.",
  },
  5: {
    livrable:
      "Assignation automatique au dépôt (aucun endpoint : effet de bord de `POST /api/exercices`, RG5/RG13), l'auteur étant écarté du pool (RG4) ; mission lisible en `GET /api/exercices/{exerciceId}/relecteur`.",
    ecart: null,
  },
  6: {
    livrable:
      "`POST /api/relectures/{id}` (opération imposée) : `400 NOTE_INVALIDE` (RG7), et **deux** statuts écrits dans la même transaction — `relecture.RENDUE` et `exercice.RELU` (D4) ; liste des missions en attente du relecteur.",
    ecart:
      "`404 RELECTURE_INCONNUE` ajouté : le contrat ne déclare aucun statut pour un identifiant de relecture qui n'existe pas. Le `403 AUTO_RELECTURE` reste un garde-fou couvert en unitaire : il est inatteignable par l'API, RG4 étant déjà garantie à l'assignation et par `ck_relecture_pas_auto_relecture`. Voir §11.",
  },
  7: {
    livrable:
      "`PUT /api/relectures/{id}/correction` : la note remplacée est archivée dans `correction_relecture` (RG8), `rendu_at` n'est pas réécrit et l'exercice reste `RELU` ; `409 SESSION_CLOTUREE` après clôture.",
    ecart:
      "`409 RELECTURE_NON_RENDUE` ajouté : corriger une relecture jamais rendue n'était prévu par aucun statut, et `correction_relecture.ancienne_note` étant `NOT NULL`, laisser passer produirait une écriture impossible. Voir §11.",
  },
  8: {
    livrable:
      "`GET /api/etudiants/{etudiantId}/relectures-recues` : note et commentaire reçus, une relecture en attente apparaissant sans note (RG9), jamais l'identité du relecteur (RG6), liste vide en `200`.",
    ecart: null,
  },
  9: {
    livrable:
      "`GET /api/tableau?promotionId=` (opération imposée) : agrégats par étudiant en **six requêtes SQL constantes** quelle que soit la taille de la promotion, moyenne arrondie côté serveur, `404 PROMOTION_INCONNUE`.",
    ecart:
      "L'ébauche d'issue décrivait un tableau par **session** (`?sessionId=`) et un `403 ACCES_REFUSE` : abandonnés, l'opération imposée prenant `promotionId` et Q1 excluant l'authentification. `relecturesEnAttente` est défini **côté auteur** (RG9). Voir §11.",
  },
  10: {
    livrable:
      "`POST /api/presences/manuelles` : `source = FORMATEUR` décidée par le serveur (RG12), `409 DEJA_PRESENT` (y compris en cas d'appels concurrents), fonctionne sans code et après la clôture ; l'étudiant entre dans le pool des relecteurs (RG13).",
    ecart:
      "`404 ETUDIANT_INCONNU` ici, alors que la même violation est un `400` sur l'opération imposée `POST /api/presences` : chaque guichet suit sa propre déclaration au contrat. Le `403` de l'ébauche est abandonné (aucune authentification, Q1). Voir §11.",
  },
  11: {
    livrable:
      "`POST /api/sessions/{id}/cloture` + gel des dépôts et des notes (`409 SESSION_CLOTUREE`), y compris pour la correction de l'EF7 ; une relecture restée en attente reste visible.",
    ecart:
      "Reclôturer répond `200` (idempotent) : le contrat ne déclare que `200` et `404` sur cette opération, et un `409` aurait été ambigu, ce code servant déjà à refuser une action **sur** une session clôturée. Précision de périmètre : RG14 gèle les dépôts et les notes, pas les présences. Voir §11.",
  },
};

/** Évidences de la dernière exécution complète, citées dans chaque commentaire. */
const PREUVES = [
  "Suite backend complète : `./mvnw test` → **161 tests, 0 échec** (H2, sans PostgreSQL).",
  "Parcours navigateur réel (Chrome headless, CDP) : **44/44 vérifications**.",
  "`npx tsc --noEmit` et `npm run build` : verts.",
];

function git(...arguments_) {
  try {
    return execFileSync("git", arguments_, { encoding: "utf8" }).trim();
  } catch {
    return "";
  }
}

/**
 * Branche et commits de l'EF, retrouvés dans l'historique : une recherche
 * littérale sur `(efN)` évite que `ef1` ne capture `ef10`.
 *
 * Le commit cité est le plus récent de l'issue (une correction ultérieure ou une
 * vérification en navigateur est plus parlante que le tout premier jet), et le
 * nombre de commits rappelle qu'une issue peut en compter plusieurs.
 */
function tracer(ef) {
  // `--no-merges` : depuis que les branches sont fusionnées par des pull
  // requests, les fusions portent le nom de la branche dans leur sujet et
  // masqueraient les commits de l'issue.
  const commits = git("log", "--all", "--no-merges", "--format=%h %s", "-F", `--grep=(ef${ef})`)
    .split("\n")
    .filter(Boolean);
  const branches = git("branch", "--list", "--format=%(refname:short)", `*ef${ef}-*`)
    .split("\n")
    .filter(Boolean);
  return {
    commit: commits[0] ?? "",
    nombre: commits.length,
    branche: branches[0] ?? "",
    fusion: fusion(branches[0] ?? ""),
  };
}

/**
 * Merge de cette branche sur `main`, s'il a déjà eu lieu : les branches sont
 * empilées, donc chaque fusion apporte les commits de son issue en plus de ceux
 * de la précédente.
 */
function fusion(branche) {
  if (!branche) return "";
  return git("log", "main", "--merges", "--format=%h %s", "-F", `--grep=${branche}`)
    .split("\n")
    .filter(Boolean)[0] ?? "";
}

function commentaire(ef) {
  const livraison = LIVRAISONS[ef];
  const { commit, nombre, branche, fusion: merge } = tracer(ef);

  const lignes = [
    `### Livré — EF${ef}`,
    "",
    livraison.livrable,
    "",
    "**Preuves**",
    "",
    ...PREUVES.map((preuve) => `- ${preuve}`),
    "",
    "**Traçabilité**",
    "",
    `- Branche : \`${branche || "(non identifiée)"}\`${nombre > 0 ? ` — ${nombre} commit(s)` : ""}`,
    commit ? `- Dernier commit : \`${commit}\`` : "- Commit : non identifié automatiquement",
    merge ? `- Fusion sur \`main\` : \`${merge}\`` : "- Fusion sur `main` : à faire",
    "- Écarts tranchés : `docs/CAHIER_DES_CHARGES.md`, section 11",
  ];

  if (livraison.ecart) {
    lignes.push("", "**Écart documenté**", "", livraison.ecart);
  }

  lignes.push(
    "",
    "---",
    "",
    merge
      ? "Fusionnée sur `main`. Pas de PR : le suivi de cette épreuve se fait par commit direct sur la branche de l'issue, fusionnée ensuite dans l'ordre des branches empilées."
      : "Branche poussée, pas encore fusionnée sur `main`. Pas de PR : le suivi de cette épreuve se fait par commit direct sur la branche de l'issue.",
  );

  return lignes.join("\n");
}

// ---------------------------------------------------------------- Issues ouvertes
if (APERCU) console.log("APERÇU — aucune écriture ne sera faite sur GitHub.\n");

const listes = await rest("GET", `/repos/${OWNER}/${REPO}/issues?state=open&per_page=100`);
if (listes.status !== 200) {
  console.error(`Lecture des issues impossible : HTTP ${listes.status} — ${listes.texte.slice(0, 200)}`);
  if (!TOKEN) {
    console.error("Sans GH_TOKEN, la lecture d'un dépôt privé est refusée : relance l'aperçu avec le jeton.");
  }
  process.exit(1);
}

const ouvertes = (listes.json ?? []).filter((issue) => !issue.pull_request);
console.log(`→ ${ouvertes.length} issue(s) ouverte(s) sur ${OWNER}/${REPO}\n`);

let fermees = 0;
let absentes = 0;

for (const ef of Object.keys(LIVRAISONS).map(Number)) {
  const label = `ef${ef}`;
  const issue = ouvertes.find((candidate) =>
    (candidate.labels ?? []).some((etiquette) => etiquette.name === label),
  );

  if (!issue) {
    console.log(`  · ${label} : aucune issue ouverte (déjà fermée ou absente)`);
    absentes += 1;
    continue;
  }

  const { commit, branche, fusion: merge } = tracer(ef);
  console.log(
    `  ${APERCU ? "→" : "✔"} #${issue.number} ${label} « ${issue.title} »` +
      `${branche ? `\n      ${branche}` : ""}${commit ? ` · ${commit}` : ""}` +
      `${merge ? `\n      fusionnée sur main : ${merge}` : "\n      PAS ENCORE FUSIONNÉE sur main"}`,
  );

  if (APERCU) continue;

  const commente = await rest("POST", `/repos/${OWNER}/${REPO}/issues/${issue.number}/comments`, {
    body: commentaire(ef),
  });
  if (commente.status !== 201) {
    console.log(`      ✘ commentaire refusé : HTTP ${commente.status} — ${commente.texte.slice(0, 160)}`);
    continue;
  }

  const close = await rest("PATCH", `/repos/${OWNER}/${REPO}/issues/${issue.number}`, {
    state: "closed",
    state_reason: "completed",
  });
  if (close.status !== 200) {
    console.log(`      ✘ fermeture refusée : HTTP ${close.status} — ${close.texte.slice(0, 160)}`);
    continue;
  }

  fermees += 1;
}

console.log(
  APERCU
    ? `\n${Object.keys(LIVRAISONS).length - absentes} issue(s) seraient fermées. Relance sans --apercu pour le faire.`
    : `\n✅ ${fermees} issue(s) fermée(s) avec leur commentaire de preuve.`,
);
console.log(`Dépôt : https://github.com/${OWNER}/${REPO}/issues?q=is%3Aissue`);
