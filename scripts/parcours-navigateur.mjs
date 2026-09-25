#!/usr/bin/env node
/**
 * Parcours navigateur de bout en bout : accueil → formateur → étudiant.
 *
 * Complète les tests de `backend/` (MockMvc, H2) sur ce qu'ils ne peuvent pas
 * voir : hydratation React, clics réels sur les formulaires contrôlés, appels
 * API depuis l'origine du navigateur (CORS), affichage des erreurs du contrat
 * et mise en page mobile (ENF1).
 *
 * Le script lance lui-même Chrome en mode headless et pilote l'onglet par le
 * DevTools Protocol. Il n'installe rien : `WebSocket` et `fetch` sont fournis
 * par Node (>= 22), aucune dépendance n'est ajoutée au projet.
 *
 * Prérequis (deux terminaux) :
 *   cd backend  && ./mvnw spring-boot:run     # http://localhost:8080
 *   cd frontend && npm run dev                # http://localhost:3000
 *
 * Usage :
 *   node scripts/parcours-navigateur.mjs
 *
 * Variables d'environnement (toutes optionnelles) :
 *   BASE_URL     URL du frontend         (défaut http://localhost:3000)
 *   API_URL      URL du backend          (défaut http://localhost:8080)
 *   CHROME_PATH  binaire Chrome/Chromium (détecté automatiquement sinon)
 *
 * Code de sortie : 0 si toutes les vérifications passent, 1 sinon.
 */
import { spawn } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const BASE_URL = (process.env.BASE_URL ?? "http://localhost:3000").replace(/\/+$/, "");
const API_URL = (process.env.API_URL ?? "http://localhost:8080").replace(/\/+$/, "");
const PACOURS_TIMEOUT_MS = 100_000;
const DELAI_CHROME_MS = 30_000;

if (process.argv.includes("--help")) {
  console.log(fs.readFileSync(new URL(import.meta.url), "utf8").split("*/")[0]);
  process.exit(0);
}

const resultats = [];
const erreursConsole = [];
const exceptions = [];
const requetesEnEchec = [];

const pause = (ms) => new Promise((r) => setTimeout(r, ms));

function verifier(description, ok, detail = "") {
  resultats.push({ description, ok, detail });
  console.log(`  ${ok ? "OK   " : "ECHEC"} ${description}${detail ? ` — ${detail}` : ""}`);
}

// ---------------------------------------------------------------------------
// Cycle de vie de Chrome
// ---------------------------------------------------------------------------

/** Binaire Chrome/Chromium, selon la plateforme et CHROME_PATH. */
function trouverChrome() {
  if (process.env.CHROME_PATH) {
    if (!fs.existsSync(process.env.CHROME_PATH)) {
      throw new Error(`CHROME_PATH introuvable : ${process.env.CHROME_PATH}`);
    }
    return process.env.CHROME_PATH;
  }

  const candidats =
    process.platform === "win32"
      ? [
          path.join(process.env["PROGRAMFILES"] ?? "C:\\Program Files", "Google/Chrome/Application/chrome.exe"),
          path.join(process.env["PROGRAMFILES(X86)"] ?? "C:\\Program Files (x86)", "Google/Chrome/Application/chrome.exe"),
          path.join(process.env.LOCALAPPDATA ?? "", "Google/Chrome/Application/chrome.exe"),
        ]
      : process.platform === "darwin"
        ? [
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
          ]
        : [
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/snap/bin/chromium",
          ];

  const trouve = candidats.find((candidat) => candidat && fs.existsSync(candidat));
  if (!trouve) {
    throw new Error("Chrome introuvable. Installez Chrome, ou indiquez CHROME_PATH=/chemin/vers/chrome.");
  }
  return trouve;
}

/**
 * Lance Chrome sur un port de débogage choisi par le navigateur lui-même
 * (`--remote-debugging-port=0`) : le port est lu dans le fichier
 * `DevToolsActivePort` du profil temporaire, ce qui évite tout conflit avec une
 * instance déjà ouverte.
 */
async function lancerChrome() {
  const binaire = trouverChrome();
  const profil = fs.mkdtempSync(path.join(os.tmpdir(), "kf48-parcours-"));

  const processus = spawn(
    binaire,
    [
      "--headless=new",
      "--remote-debugging-port=0",
      "--remote-allow-origins=*",
      `--user-data-dir=${profil}`,
      "--no-first-run",
      "--no-default-browser-check",
      "--disable-gpu",
      "--window-size=900,1200",
      BASE_URL,
    ],
    { stdio: "ignore", windowsHide: true },
  );

  const fichierPort = path.join(profil, "DevToolsActivePort");
  const fin = Date.now() + DELAI_CHROME_MS;
  while (Date.now() < fin) {
    if (fs.existsSync(fichierPort)) {
      const [port] = fs.readFileSync(fichierPort, "utf8").split("\n");
      if (port && port.trim() !== "") {
        return { processus, profil, port: port.trim() };
      }
    }
    if (processus.exitCode !== null) {
      throw new Error(`Chrome s'est arrêté aussitôt (code ${processus.exitCode}).`);
    }
    await pause(250);
  }
  throw new Error("Chrome n'a pas exposé son port de débogage en 30 s.");
}

/** Ferme Chrome par le protocole (et non par un kill global), puis efface le profil. */
async function arreterChrome({ processus, profil, port }) {
  try {
    const version = await (await fetch(`http://127.0.0.1:${port}/json/version`)).json();
    const ws = new WebSocket(version.webSocketDebuggerUrl);
    await new Promise((r) => ws.addEventListener("open", r));
    ws.send(JSON.stringify({ id: 1, method: "Browser.close", params: {} }));
    await pause(1200);
  } catch {
    // Chrome déjà arrêté : rien à faire.
  }
  try {
    processus.kill();
  } catch {
    // ignoré
  }
  await pause(300);
  try {
    fs.rmSync(profil, { recursive: true, force: true });
  } catch {
    // Le profil reste dans le dossier temporaire du système : sans conséquence.
  }
}

// ---------------------------------------------------------------------------
// Client CDP minimal
// ---------------------------------------------------------------------------

class Cdp {
  constructor(ws) {
    this.ws = ws;
    this.enAttente = new Map();
    this.prochainId = 1;
    ws.addEventListener("message", (evenement) => {
      const message = JSON.parse(evenement.data);
      if (message.id && this.enAttente.has(message.id)) {
        const { resoudre, rejeter } = this.enAttente.get(message.id);
        this.enAttente.delete(message.id);
        if (message.error) rejeter(new Error(JSON.stringify(message.error)));
        else resoudre(message.result);
        return;
      }
      this.traiterEvenement(message);
    });
  }

  traiterEvenement(message) {
    if (message.method === "Runtime.consoleAPICalled" && ["error", "warning"].includes(message.params.type)) {
      erreursConsole.push(`[${message.params.type}] ${message.params.args.map((a) => a.value ?? a.description ?? a.type).join(" ")}`);
    }
    if (message.method === "Runtime.exceptionThrown") {
      exceptions.push(message.params.exceptionDetails.exception?.description ?? message.params.exceptionDetails.text);
    }
    if (message.method === "Network.loadingFailed") {
      requetesEnEchec.push(`${message.params.errorText} (${message.params.type})`);
    }
    if (message.method === "Network.responseReceived" && message.params.response.status >= 400) {
      requetesEnEchec.push(`HTTP ${message.params.response.status} ${message.params.response.url}`);
    }
  }

  envoyer(method, params = {}) {
    const id = this.prochainId;
    this.prochainId += 1;
    return new Promise((resoudre, rejeter) => {
      this.enAttente.set(id, { resoudre, rejeter });
      this.ws.send(JSON.stringify({ id, method, params }));
      setTimeout(() => {
        if (this.enAttente.has(id)) {
          this.enAttente.delete(id);
          rejeter(new Error(`${method} : aucune réponse du navigateur (15 s)`));
        }
      }, 15000);
    });
  }

  /** Évalue une expression dans la page et renvoie sa valeur sérialisable. */
  async evaluer(expression) {
    const resultat = await this.envoyer("Runtime.evaluate", {
      expression,
      returnByValue: true,
      awaitPromise: true,
    });
    if (resultat.exceptionDetails) {
      throw new Error(resultat.exceptionDetails.exception?.description ?? "évaluation en échec");
    }
    return resultat.result.value;
  }

  /** Attend qu'une expression devienne vraie. */
  async attendre(expression, description, delai = 10000) {
    const fin = Date.now() + delai;
    while (Date.now() < fin) {
      // eslint-disable-next-line no-await-in-loop
      if (await this.evaluer(`Boolean(${expression})`)) return true;
      // eslint-disable-next-line no-await-in-loop
      await pause(200);
    }
    throw new Error(`délai dépassé en attendant : ${description}`);
  }
}

/** Renseigne un champ contrôlé par React (setter natif + événement input). */
const REMPLIR = `
function remplir(selecteur, valeur) {
  const champ = document.querySelector(selecteur);
  if (!champ) return false;
  const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
  setter.call(champ, valeur);
  champ.dispatchEvent(new Event("input", { bubbles: true }));
  return true;
}
`;

const cliquerSur = (selecteur) => `document.querySelector(${JSON.stringify(selecteur)}).click()`;

// ---------------------------------------------------------------------------
// Parcours
// ---------------------------------------------------------------------------

/** Échoue tôt et lisiblement si les deux serveurs ne tournent pas. */
async function verifierPrerequis() {
  const joignable = async (url) => {
    try {
      await fetch(url, { signal: AbortSignal.timeout(5000) });
      return true;
    } catch {
      return false;
    }
  };

  if (!(await joignable(BASE_URL))) {
    throw new Error(`frontend injoignable sur ${BASE_URL} — démarrez-le : cd frontend && npm run dev`);
  }
  if (!(await joignable(`${API_URL}/api/promotions/1/etudiants`))) {
    throw new Error(`backend injoignable sur ${API_URL} — démarrez-le : cd backend && ./mvnw spring-boot:run`);
  }
}

/**
 * Après une navigation (chargement complet ou navigation cliente de Next), React
 * doit avoir attaché ses gestionnaires : cliquer avant l'hydratation envoie le
 * clic dans le vide et ferait échouer le parcours à tort.
 */
async function attendreHydratation(cdp) {
  await cdp.attendre('document.readyState === "complete"', "document chargé");
  await pause(2000);
}

async function deroulerLeParcours(port) {
  const cibles = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
  const cible = cibles.find((c) => c.type === "page" && c.webSocketDebuggerUrl);
  if (!cible) throw new Error("Chrome n'a exposé aucune cible de type page.");

  const ws = new WebSocket(cible.webSocketDebuggerUrl);
  await new Promise((resoudre, rejeter) => {
    ws.addEventListener("open", resoudre);
    ws.addEventListener("error", rejeter);
  });

  const cdp = new Cdp(ws);
  await cdp.envoyer("Page.enable");
  await cdp.envoyer("Runtime.enable");
  await cdp.envoyer("Network.enable");
  await cdp.envoyer("Log.enable");

  const url = (chemin) => `${BASE_URL}${chemin}`;

  // ---------- 1. Accueil ----------
  console.log("\n1. Accueil (/)");
  await cdp.envoyer("Page.navigate", { url: url("/") });
  await cdp.attendre(`document.body.innerText.includes("Espace formateur")`, "contenu de l'accueil");
  await attendreHydratation(cdp);

  const accueil = await cdp.evaluer("document.body.innerText");
  verifier("La racine sert une page (pas de 404)",
    accueil.includes("Espace formateur") && accueil.includes("Espace étudiant"));
  verifier("L'espace relecteur est annoncé sans mener à une page inexistante",
    accueil.includes("Espace relecteur")
      && (await cdp.evaluer(`document.querySelectorAll('a[href="/relecteur"]').length === 0`)));
  verifier("Un lien mène à l'espace formateur",
    await cdp.evaluer(`document.querySelector('a[href="/formateur"]') !== null`));

  // ---------- 2. Formateur : ouvrir une session ----------
  console.log("\n2. Formateur (/formateur)");
  await cdp.evaluer(cliquerSur('a[href="/formateur"]'));
  await cdp.attendre(`location.pathname === "/formateur"`, "navigation vers /formateur");
  await cdp.attendre(`document.querySelector("#titre") !== null`, "formulaire formateur rendu");
  await attendreHydratation(cdp);

  const titre = `Parcours navigateur ${new Date().toISOString().slice(11, 19)}`;
  await cdp.evaluer(`${REMPLIR}; remplir("#titre", ${JSON.stringify(titre)})`);
  await cdp.evaluer(`${REMPLIR}; remplir("#promotionId", "1")`);
  verifier("Les champs contrôlés acceptent la saisie",
    await cdp.evaluer(`document.querySelector("#titre").value === ${JSON.stringify(titre)}`));

  await cdp.evaluer(cliquerSur('form button[type="submit"]'));
  await cdp.attendre(`document.body.innerText.includes("Session ouverte")`, "affichage du code de session");
  const texteFormateur = await cdp.evaluer("document.body.innerText");
  const code = (texteFormateur.match(/\b[A-Z0-9]{6}\b/) ?? [])[0];
  verifier("La session s'ouvre depuis le navigateur et le code s'affiche", Boolean(code), `code=${code}`);

  // ---------- 3. Retour accueil, puis étudiant ----------
  console.log("\n3. Étudiant (/etudiant)");
  await cdp.envoyer("Page.navigate", { url: url("/") });
  await cdp.attendre(`document.querySelector('a[href="/etudiant"]') !== null`, "accueil de nouveau affiché");
  await cdp.evaluer(cliquerSur('a[href="/etudiant"]'));
  await cdp.attendre(`location.pathname === "/etudiant"`, "navigation vers /etudiant");
  await cdp.attendre(`document.querySelector("#promotionId") !== null`, "formulaire étudiant rendu");
  await attendreHydratation(cdp);
  verifier("Le clic depuis l'accueil mène à l'espace étudiant", (await cdp.evaluer("location.pathname")) === "/etudiant");

  // 3a. Liste des étudiants — appel API réel depuis l'origine du navigateur (CORS).
  await cdp.evaluer(`${REMPLIR}; remplir("#promotionId", "1")`);
  await cdp.evaluer(cliquerSur('form:nth-of-type(1) button[type="submit"]'));
  let listeChargee = true;
  try {
    await cdp.attendre(`document.querySelectorAll("ul li button").length > 0`, "liste des étudiants", 8000);
  } catch {
    listeChargee = false;
  }
  const nbEtudiants = await cdp.evaluer(`document.querySelectorAll("ul li button").length`);
  verifier("La liste des étudiants se charge depuis le navigateur", listeChargee && nbEtudiants > 0,
    listeChargee ? `${nbEtudiants} étudiants` : await cdp.evaluer("document.body.innerText.slice(0, 200)"));

  // 3b. Marquage de présence avec le code obtenu à l'étape 2 (EF2).
  await cdp.evaluer(`document.querySelectorAll("ul li button")[0].click()`);
  await cdp.evaluer(`${REMPLIR}; remplir("#code", ${JSON.stringify(code)})`);
  await cdp.evaluer(cliquerSur('form:nth-of-type(2) button[type="submit"]'));
  let presenceOk = true;
  try {
    await cdp.attendre(`document.body.innerText.includes("Présence enregistrée")`, "confirmation de présence", 8000);
  } catch {
    presenceOk = false;
  }
  verifier("La présence est enregistrée depuis le navigateur", presenceOk,
    presenceOk
      ? await cdp.evaluer(`document.body.innerText.match(/Session n°\\d+ — source : \\w+/)?.[0] ?? ""`)
      : await cdp.evaluer("document.body.innerText.slice(0, 200)"));

  // 3c. Dépôt de l'exercice (EF3) : la session vient du serveur, sans ressaisie.
  verifier("L'identifiant de session est repris du serveur après la présence",
    (await cdp.evaluer(`document.querySelector("#sessionId").value`)) !== "");
  await cdp.evaluer(`${REMPLIR}; remplir("#lien", "https://exemple.org/parcours-navigateur.pdf")`);
  await cdp.evaluer(cliquerSur('form:nth-of-type(3) button[type="submit"]'));

  let depotOk = true;
  try {
    await cdp.attendre(`document.body.innerText.includes("Exercice déposé")`, "confirmation de dépôt", 8000);
  } catch {
    depotOk = false;
  }
  const detailDepot = await cdp.evaluer(
    `document.body.innerText.match(/Exercice n°\\d+ — statut : \\w+/)?.[0] ?? document.body.innerText.slice(0, 200)`,
  );
  verifier("Le dépôt de l'exercice aboutit depuis le navigateur", depotOk, detailDepot);
  verifier("Le statut renvoyé appartient au cycle de vie du contrat (D4)",
    /statut : (DEPOSE|EN_ATTENTE_RELECTURE|RELU)/.test(detailDepot), detailDepot);

  // ---------- 4. Chemin d'erreur et affichage mobile ----------
  console.log("\n4. Chemin d'erreur et affichage mobile");

  // Un code inconnu doit afficher l'erreur du serveur, pas un écran cassé.
  await cdp.evaluer(cliquerSur('form:nth-of-type(1) button[type="submit"]'));
  await cdp.attendre(`document.querySelectorAll("ul li button").length > 0`, "liste des étudiants rechargée", 8000);
  await cdp.evaluer(`document.querySelectorAll("ul li button")[0].click()`);
  await cdp.evaluer(`${REMPLIR}; remplir("#code", "ZZZZZZ")`);
  await cdp.evaluer(cliquerSur('form:nth-of-type(2) button[type="submit"]'));

  let erreurAffichee = true;
  try {
    await cdp.attendre(`document.querySelector('[role="alert"]') !== null`, "bloc d'erreur affiché", 8000);
  } catch {
    erreurAffichee = false;
  }
  const texteErreur = await cdp.evaluer(
    `(document.querySelector('[role="alert"]')?.innerText ?? "(aucun)").split(String.fromCharCode(10)).join(" ")`,
  );
  verifier("Un code inconnu affiche l'erreur du contrat (CODE_INCONNU)",
    erreurAffichee && texteErreur.includes("CODE_INCONNU"), texteErreur);

  // ENF1 : l'écran étudiant doit rester utilisable sur un écran de téléphone.
  await cdp.envoyer("Emulation.setDeviceMetricsOverride", {
    width: 390, height: 844, deviceScaleFactor: 2, mobile: true,
  });
  await cdp.envoyer("Page.navigate", { url: url("/etudiant") });
  await attendreHydratation(cdp);
  const debordement = await cdp.evaluer(`document.documentElement.scrollWidth - window.innerWidth`);
  verifier("Aucun débordement horizontal sur 390 px de large (ENF1)", debordement <= 0, `débordement=${debordement}px`);
  await cdp.envoyer("Emulation.clearDeviceMetricsOverride");

  // ---------- 5. Hygiène du navigateur ----------
  console.log("\n5. Console et réseau");
  verifier("Aucune exception JavaScript", exceptions.length === 0, exceptions.slice(0, 3).join(" | "));
  verifier("Aucune erreur console", erreursConsole.length === 0, erreursConsole.slice(0, 3).join(" | "));

  // Deux entrées attendues sont exclues du contrôle : le 400 provoqué par le code
  // inconnu (étape 4), et le chargement de page que ce script interrompt lui-même
  // en naviguant (Chrome démarre sur BASE_URL, puis le pilote prend la main).
  const inattendues = requetesEnEchec.filter(
    (e) => !e.includes(`HTTP 400 ${API_URL}/api/presences`) && !e.includes("net::ERR_ABORTED (Document)"),
  );
  verifier("Aucune requête réseau en échec hors refus provoqué", inattendues.length === 0,
    inattendues.slice(0, 4).join(" | ")
      || `${requetesEnEchec.length - inattendues.length} entrée(s) attendue(s) écartée(s)`);

  ws.close();
}

// ---------------------------------------------------------------------------

console.log(`Parcours navigateur — frontend ${BASE_URL}, backend ${API_URL}`);

let erreurFatale = null;
try {
  await verifierPrerequis();
  const chrome = await lancerChrome();
  try {
    await deroulerLeParcours(chrome.port);
  } finally {
    await arreterChrome(chrome);
  }
} catch (erreur) {
  erreurFatale = erreur;
  console.log(`\nPARCOURS INTERROMPU : ${erreur.message}`);
  if (erreursConsole.length > 0) console.log(`console   : ${erreursConsole.join(" | ")}`);
  if (exceptions.length > 0) console.log(`exceptions: ${exceptions.join(" | ")}`);
  if (requetesEnEchec.length > 0) console.log(`réseau    : ${requetesEnEchec.join(" | ")}`);
}

const echecs = resultats.filter((r) => !r.ok);
console.log(`\n=== ${resultats.length - echecs.length}/${resultats.length} vérifications passées ===`);
if (echecs.length > 0) {
  console.log("Échecs :");
  echecs.forEach((e) => console.log(`  - ${e.description}${e.detail ? ` (${e.detail})` : ""}`));
}
process.exit(erreurFatale || echecs.length > 0 ? 1 : 0);
