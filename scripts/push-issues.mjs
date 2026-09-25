#!/usr/bin/env node
/**
 * Sauvegarde les issues existantes, les supprime (GraphQL deleteIssue),
 * puis crée les 11 issues du backlog lues depuis docs/BACKLOG.md.
 *
 * Usage : GH_TOKEN=xxx node scripts/push-issues.mjs
 */
import fs from "node:fs";
import path from "node:path";

const OWNER = "baurel-bits";
const REPO = "kfokam48-epreuve-KF48-YAO-168";
const API = "https://api.github.com";
const TOKEN = process.env.GH_TOKEN;

if (!TOKEN) {
  console.error("GH_TOKEN manquant.");
  process.exit(1);
}

const REST_HEADERS = {
  Authorization: `Bearer ${TOKEN}`,
  Accept: "application/vnd.github+json",
  "X-GitHub-Api-Version": "2022-11-28",
  "User-Agent": "kfokam48-backlog-script",
};

async function rest(method, url, body) {
  const res = await fetch(`${API}${url}`, {
    method,
    headers: { ...REST_HEADERS, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { /* ignore */ }
  return { status: res.status, json, text };
}

async function graphql(query, variables) {
  const res = await fetch(`${API}/graphql`, {
    method: "POST",
    headers: { ...REST_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  return { status: res.status, json: await res.json() };
}

// ---------------------------------------------------------------- 1. Lire le backlog
const md = fs.readFileSync(path.join(process.cwd(), "docs", "BACKLOG.md"), "utf8");
const issues = [];

for (const section of md.split(/^## /m).slice(1)) {
  const nl = section.indexOf("\n");
  const heading = section.slice(0, nl).trim();
  const m = /^Issue (\d+)\b/.exec(heading);
  if (!m) continue; // ignore "Couverture ..." et "Récapitulatif"

  const content = section.slice(nl + 1);

  const titleMatch = /^\*\*Titre :\*\* (.+)$/m.exec(content);
  if (!titleMatch) throw new Error(`Titre introuvable pour l'issue ${m[1]}`);
  const title = titleMatch[1].trim();

  const body = content
    .split(/\n---\n/)[0]
    .replace(/^- \[ \] \*\*Backlog\*\*\s*$/m, "")
    .replace(/^\*\*Titre :\*\* .+$/m, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();

  const priority = /Priorité\*\*\s+(Must|Should|Could)/.exec(content)?.[1] ?? "Should";
  const ef = /EF(\d+)/.exec(content)?.[1];

  issues.push({ num: Number(m[1]), title, body, priority, ef });
}

console.log(`→ ${issues.length} issues lues depuis docs/BACKLOG.md`);

// ---------------------------------------------------------------- 2. Sauvegarder + supprimer
const existing = await rest("GET", `/repos/${OWNER}/${REPO}/issues?state=all&per_page=100`);
const toDelete = (existing.json ?? []).filter((i) => !i.pull_request);

const backupDir = path.join(process.cwd(), "backup");
fs.mkdirSync(backupDir, { recursive: true });
const backupFile = path.join(backupDir, "issues-avant-suppression.json");
fs.writeFileSync(backupFile, JSON.stringify(toDelete, null, 2), "utf8");
console.log(`→ Sauvegarde de ${toDelete.length} issue(s) dans ${path.relative(process.cwd(), backupFile)}`);

for (const issue of toDelete) {
  const r = await graphql(
    "mutation($id: ID!) { deleteIssue(input: { issueId: $id }) { clientMutationId } }",
    { id: issue.node_id },
  );
  const ok = !r.json?.errors;
  console.log(`  ${ok ? "✔" : "✘"} suppression #${issue.number} « ${issue.title} »${ok ? "" : " → " + JSON.stringify(r.json?.errors?.[0]?.message)}`);
}

// ---------------------------------------------------------------- 3. Labels + milestone
const LABELS = [
  { name: "must", color: "B60205", description: "Périmètre v0.1" },
  { name: "should", color: "FBCA04", description: "Hors v0.1" },
];
for (let i = 1; i <= 11; i++) {
  LABELS.push({ name: `ef${i}`, color: "0E8A16", description: `Exigence fonctionnelle EF${i}` });
}

for (const label of LABELS) {
  const r = await rest("POST", `/repos/${OWNER}/${REPO}/labels`, label);
  if (r.status === 201) console.log(`  ✔ label ${label.name}`);
  else if (r.status === 422) console.log(`  = label ${label.name} déjà présent`);
  else console.log(`  ✘ label ${label.name} → ${r.status} ${r.text.slice(0, 120)}`);
}

let milestoneNumber = null;
{
  const r = await rest("POST", `/repos/${OWNER}/${REPO}/milestones`, {
    title: "v0.1",
    description: "Périmètre Must : EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11",
  });
  if (r.status === 201) {
    milestoneNumber = r.json.number;
    console.log(`  ✔ milestone v0.1 (#${milestoneNumber})`);
  } else if (r.status === 422) {
    const list = await rest("GET", `/repos/${OWNER}/${REPO}/milestones?state=all&per_page=100`);
    milestoneNumber = list.json?.find((ms) => ms.title === "v0.1")?.number ?? null;
    console.log(`  = milestone v0.1 déjà présent (#${milestoneNumber})`);
  } else {
    console.log(`  ✘ milestone → ${r.status} ${r.text.slice(0, 120)}`);
  }
}

// ---------------------------------------------------------------- 4. Créer les issues
console.log("→ Création des issues…");
const created = [];
for (const issue of issues) {
  const labels = [issue.priority.toLowerCase(), `ef${issue.ef}`];
  const payload = { title: issue.title, body: issue.body, labels };
  if (issue.priority === "Must" && milestoneNumber) payload.milestone = milestoneNumber;

  const r = await rest("POST", `/repos/${OWNER}/${REPO}/issues`, payload);
  if (r.status === 201) {
    created.push(r.json);
    console.log(`  ✔ #${r.json.number} ${issue.title}`);
  } else {
    console.log(`  ✘ ${issue.title} → ${r.status} ${r.text.slice(0, 160)}`);
  }
}

console.log(`\n✅ ${created.length}/${issues.length} issues créées.`);
console.log(`Sauvegarde des anciennes issues : ${path.relative(process.cwd(), backupFile)}`);
