#!/usr/bin/env node
/**
 * Ajoute à chaque issue existante (repérée par son label `efN`) le détail
 * complémentaire défini dans docs/BACKLOG_DETAIL.md.
 *
 * Usage : GH_TOKEN=xxx node scripts/enrich-issues.mjs
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

const HEADERS = {
  Authorization: `Bearer ${TOKEN}`,
  Accept: "application/vnd.github+json",
  "X-GitHub-Api-Version": "2022-11-28",
  "User-Agent": "kfokam48-backlog-script",
};

async function rest(method, url, body) {
  const res = await fetch(`${API}${url}`, {
    method,
    headers: { ...HEADERS, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { /* ignore */ }
  return { status: res.status, json, text };
}

const md = fs.readFileSync(path.join(process.cwd(), "docs", "BACKLOG_DETAIL.md"), "utf8");
const sections = {};
for (const sec of md.split(/^## EF/m).slice(1)) {
  const nl = sec.indexOf("\n");
  const ef = sec.slice(0, nl).trim();
  if (!/^\d+$/.test(ef)) continue;
  sections[ef] = sec.slice(nl + 1).replace(/\n---\s*$/, "").trim();
}
console.log(`→ ${Object.keys(sections).length} sections de détail lues (EF${Object.keys(sections).join(", EF")})`);

const list = await rest("GET", `/repos/${OWNER}/${REPO}/issues?state=open&per_page=100`);
const openIssues = (list.json ?? []).filter((i) => !i.pull_request);

for (const [ef, extra] of Object.entries(sections)) {
  const issue = openIssues.find((i) => (i.labels ?? []).some((l) => l.name === `ef${ef}`));
  if (!issue) {
    console.log(`  ✘ aucune issue ouverte avec le label ef${ef}`);
    continue;
  }
  if ((issue.body ?? "").includes("Détails complémentaires")) {
    console.log(`  = #${issue.number} déjà enrichie`);
    continue;
  }
  const body = `${issue.body ?? ""}\n\n---\n\n## Détails complémentaires\n\n${extra}\n`;
  const r = await rest("PATCH", `/repos/${OWNER}/${REPO}/issues/${issue.number}`, { body });
  console.log(`  ${r.status === 200 ? "✔" : "✘"} #${issue.number} (ef${ef}) → ${r.status}`);
}
