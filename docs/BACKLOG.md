# Backlog — Issues GitHub

Issues dérivées des EF1–EF11 (section 4) et RG1–RG14 (section 6) du cahier des charges.

- **Milestone `v0.1`** : les 8 issues `Must`, exactement `{EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11}` (étape v0.1, section 10).
- **Labels** : `must`, `should`, plus `ef1`…`ef11` pour la traçabilité.
- Chaque bloc ci-dessous correspond au **champ description** d'une issue GitHub.

---

## 1 · Ouvrir une session et obtenir un code de présence

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF1` · `RG1`

**Critères d'acceptation**
- [ ] Quand le formateur crée une session avec un titre et une promotion, alors l'API répond `201` avec un identifiant, un code, une heure d'ouverture et une heure d'expiration.
- [ ] Quand la session vient d'être ouverte, alors l'heure d'expiration vaut l'heure d'ouverture **+ 15 min**.
- [ ] Quand le titre ou la promotion est absent/invalide, alors l'API répond `400` au format `{ code, message }`.

---

## 2 · Marquer ma présence avec le code de la session

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF2` · `RG1`, `RG2`, `RG3`

**Critères d'acceptation**
- [ ] Quand je saisis un code valide et non expiré, alors ma présence est enregistrée avec `source = ETUDIANT` et apparaît dans le tableau du formateur.
- [ ] Quand le code est inconnu, alors l'API répond `400 CODE_INCONNU`.
- [ ] Quand le code est expiré, alors l'API répond `410 CODE_EXPIRE`.
- [ ] Quand j'ai déjà marqué ma présence pour cette session, alors l'API répond `409 DEJA_PRESENT`.
- [ ] Quand j'atteins 5 échecs de saisie sur cette session, alors je suis bloqué 2 minutes (compteur par couple étudiant/session).

---

## 3 · Déposer le lien de mon exercice pour une session

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF3` · `RG10`

**Critères d'acceptation**
- [ ] Quand je soumets un lien valide pour une session où je n'ai pas encore déposé, alors l'exercice est créé au statut `DEPOSE` et l'API répond `201 { id, statut }`.
- [ ] Quand le lien est mal formé, alors l'API répond `400 LIEN_INVALIDE`.
- [ ] Quand j'ai déjà un exercice sur cette session, alors l'API répond `409 EXERCICE_DEJA_DEPOSE`.
- [ ] Quand la session est encore ouverte ou non clôturée, alors le dépôt reste possible même après expiration du code.

---

## 4 · Recevoir automatiquement un pair pour relire mon exercice

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF5` · `RG5`, `RG13`, `RG4`, `RG9`

**Critères d'acceptation**
- [ ] Quand un exercice est déposé, alors un relecteur est tiré au hasard parmi les étudiants **présents** à la session, **différent de l'auteur**.
- [ ] Quand le pool est évalué, alors il est recalculé à l'instant de l'assignation, présences manuelles antérieures incluses.
- [ ] Quand aucun étudiant éligible n'est disponible, alors l'exercice reste au statut `EN_ATTENTE_RELECTURE` sans relecteur.
- [ ] Quand je suis auteur de l'exercice, alors je ne peux jamais être désigné comme relecteur.

---

## 5 · Noter et commenter l'exercice qui m'est assigné

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF6` · `RG7`, `RG4`

**Critères d'acceptation**
- [ ] Quand j'envoie une note entière entre 0 et 20 et un commentaire pour une relecture qui m'est assignée, alors la relecture passe au statut `RELU` et l'API répond `200`.
- [ ] Quand la note est hors 0–20 ou non entière, alors l'API répond `400 NOTE_INVALIDE`.
- [ ] Quand la relecture visée concerne mon propre exercice, alors l'API répond `403 AUTO_RELECTURE`.

---

## 6 · Consulter ma note et le commentaire reçu, sans savoir qui m'a relu

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF8` · `RG6`, `RG9`

**Critères d'acceptation**
- [ ] Quand ma relecture est rendue, alors je vois la note et le commentaire.
- [ ] Quand j'accède à ma note, alors l'identité du relecteur n'apparaît dans aucune réponse de l'API.
- [ ] Quand aucune relecture n'est encore rendue, alors je vois mon exercice « en attente », sans note.

---

## 7 · Suivre l'avancement de ma promotion sur un tableau

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF9` · `RG9`, `RG5`

**Critères d'acceptation**
- [ ] Quand je consulte le tableau d'une promotion, alors j'obtiens par étudiant : présences, exercices déposés, moyenne des notes, relectures en attente.
- [ ] Quand aucune note n'a été reçue, alors la moyenne est `null` (et non `0`).
- [ ] Quand la moyenne est affichée, alors elle provient de l'API, jamais recalculée côté client.
- [ ] Quand la promotion est inconnue, alors l'API répond `404 PROMOTION_INCONNUE`.

---

## 8 · Clôturer une session pour figer les dépôts et les notes

**Priorité** : `Must` — v0.1
**Traçabilité** : `EF11` · `RG14`, `RG8`

**Critères d'acceptation**
- [ ] Quand je clôture une session, alors plus aucun dépôt ni remplacement de lien n'est accepté sur cette session.
- [ ] Quand je clôture une session, alors plus aucune note/commentaire ne peut être créé ou corrigé sur cette session.
- [ ] Quand des relectures restent en attente, alors la clôture réussit quand même et elles restent définitivement en attente.

---

## 9 · Remplacer le lien de mon exercice tant qu'il n'a pas été relu

**Priorité** : `Should`
**Traçabilité** : `EF4` · `RG11`, `RG10`

**Critères d'acceptation**
- [ ] Quand aucune relecture n'a commencé sur mon exercice, alors je peux soumettre un nouveau lien qui remplace l'ancien.
- [ ] Quand une relecture a été rendue, alors le remplacement est refusé.
- [ ] Quand la session est clôturée, alors le remplacement est refusé.
- [ ] À confirmer : « relecture commencée » = relecture **rendue**, ou simple **assignation** d'un relecteur ?

---

## 10 · Corriger ma note et mon commentaire avant la clôture

**Priorité** : `Should`
**Traçabilité** : `EF7` · `RG8`, `RG14`

**Critères d'acceptation**
- [ ] Quand la session n'est pas clôturée, alors je peux renvoyer une nouvelle note/commentaire pour une relecture que j'ai déjà rendue.
- [ ] Quand je corrige, alors la nouvelle valeur remplace l'ancienne et la relecture reste au statut `RELU`.
- [ ] Quand la session est clôturée, alors toute correction est refusée (`409 RELECTURE_DEJA_RENDUE`).

---

## 11 · Ajouter manuellement une présence pour un étudiant

**Priorité** : `Should`
**Traçabilité** : `EF10` · `RG12`, `RG13`

**Critères d'acceptation**
- [ ] Quand j'ajoute une présence pour un étudiant, alors elle apparaît avec `source = FORMATEUR`.
- [ ] Quand l'étudiant est déjà marqué présent pour cette session, alors l'ajout est refusé (`409 DEJA_PRESENT`).
- [ ] Quand une présence manuelle précède l'assignation d'un relecteur, alors l'étudiant entre dans le pool des éligibles.

---

## Récapitulatif

| # | EF | Priorité | Milestone |
|---|---|---|---|
| 1 | EF1 | Must | v0.1 |
| 2 | EF2 | Must | v0.1 |
| 3 | EF3 | Must | v0.1 |
| 4 | EF5 | Must | v0.1 |
| 5 | EF6 | Must | v0.1 |
| 6 | EF8 | Must | v0.1 |
| 7 | EF9 | Must | v0.1 |
| 8 | EF11 | Must | v0.1 |
| 9 | EF4 | Should | — |
| 10 | EF7 | Should | — |
| 11 | EF10 | Should | — |

Les 8 `Must` couvrent exactement `{EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11}`, conforme à l'étape v0.1 (section 10).
