#!/usr/bin/env bash
# =============================================================================
# Création des 11 issues du backlog KFOKAM48 via GitHub CLI.
#
# Prérequis : gh installé ET authentifié (gh auth login) dans ce dépôt.
# Usage      : bash scripts/create-issues.sh
#
# - 8 issues Must   -> milestone "v0.1" (périmètre v0.1, section 10)
# - 3 issues Should -> sans milestone
# - labels : must/should + ef1..ef11
# - les corps référencent les 13 endpoints de api/contrat.yaml
# =============================================================================
set -euo pipefail

echo "→ Création des labels…"
for l in must should ef1 ef2 ef3 ef4 ef5 ef6 ef7 ef8 ef9 ef10 ef11; do
  gh label create "$l" --force >/dev/null 2>&1 || true
done

echo "→ Création du milestone v0.1…"
gh api repos/{owner}/{repo}/milestones -f title="v0.1" >/dev/null 2>&1 || true

echo "→ Création des issues…"

# ---------------------------------------------------------------- Issue 1 (Must) EF1
gh issue create \
  --title "Le formateur ouvre une session et obtient un code de présence" \
  --label "must,ef1" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF1 · Règles RG1 · **Priorité** Must · **Estimation** 2h

### Critères d'acceptation
- Quand le formateur crée une session avec un titre et une promotion, alors l'API répond `201` avec un identifiant, un code, une heure d'ouverture et une heure d'expiration.
- Quand la session vient d'être ouverte, alors l'heure d'expiration vaut l'heure d'ouverture + 15 minutes.
- Quand le titre ou la promotion est absent, alors l'API répond `400` au format `{ code, message }` sans exposer de stack trace.

**Endpoints :** `POST /api/sessions`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `POST /api/sessions` : `201` et `expirationAt = ouvertureAt + 15 min`
- [ ] Documentation impactée à jour (`api/contrat.yaml`, `docs/diagrammes/`)
EOF
)"

# ---------------------------------------------------------------- Issue 2 (Must) EF2
gh issue create \
  --title "L'étudiant marque sa présence avec un code" \
  --label "must,ef2" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF2 · Règles RG1, RG2, RG3 · **Priorité** Must · **Estimation** 3h

### Critères d'acceptation
- Quand l'étudiant saisit un code valide et non expiré, alors sa présence est enregistrée avec `source = ETUDIANT` et apparaît dans le tableau du formateur.
- Quand le code est inconnu, alors l'API répond `400 CODE_INCONNU`.
- Quand le code a plus de 15 minutes, alors l'API répond `410 CODE_EXPIRE`.
- Quand l'étudiant a déjà marqué sa présence pour cette session, alors l'API répond `409 DEJA_PRESENT`.
- Quand l'étudiant atteint 5 échecs de saisie sur cette session, alors il est bloqué 2 minutes et l'API répond `429 TROP_DE_TENTATIVES`.

**Endpoints :** `POST /api/presences` · prérequis `GET /api/promotions/{promotionId}/etudiants`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `POST /api/presences` : `201` / `400 CODE_INCONNU` / `409 DEJA_PRESENT` / `410 CODE_EXPIRE` / `429 TROP_DE_TENTATIVES`
- [ ] Test unitaire sur l'expiration du code (RG1) et le compteur d'échecs (RG3)
- [ ] `etudiantId` obtenu via `GET /api/promotions/{promotionId}/etudiants` (prérequis Q1)
- [ ] Documentation impactée à jour (`api/contrat.yaml`, `docs/diagrammes/D3_sequence_presence.md`)
EOF
)"

# ---------------------------------------------------------------- Issue 3 (Must) EF3
gh issue create \
  --title "L'étudiant dépose le lien de son exercice" \
  --label "must,ef3" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF3 · Règles RG10 · **Priorité** Must · **Estimation** 2h

### Critères d'acceptation
- Quand l'étudiant soumet un lien valide pour une session où il n'a rien déposé, alors l'exercice est créé au statut `DEPOSE` et l'API répond `201 { id, statut }`.
- Quand le lien est mal formé, alors l'API répond `400 LIEN_INVALIDE`.
- Quand l'étudiant a déjà un exercice sur cette session, alors l'API répond `409 EXERCICE_DEJA_DEPOSE`.
- Quand la session n'est pas clôturée, alors le dépôt reste possible même après l'expiration du code.

**Endpoints :** `POST /api/exercices` · prérequis `GET /api/promotions/{promotionId}/etudiants`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `POST /api/exercices` : `201` / `400 LIEN_INVALIDE` / `409 EXERCICE_DEJA_DEPOSE`
- [ ] `etudiantId` obtenu via `GET /api/promotions/{promotionId}/etudiants` (prérequis Q1)
- [ ] Documentation impactée à jour (`api/contrat.yaml`, `docs/diagrammes/D4_etat_exercice.md`)
EOF
)"

# ---------------------------------------------------------------- Issue 4 (Must) EF5
gh issue create \
  --title "Un relecteur est assigné automatiquement à chaque exercice déposé" \
  --label "must,ef5" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF5 · Règles RG5, RG13, RG4, RG9 · **Priorité** Must · **Estimation** 3h

### Critères d'acceptation
- Quand un exercice est déposé, alors un relecteur est tiré au hasard parmi les étudiants présents à la session et différent de l'auteur.
- Quand le pool est évalué, alors il est recalculé à l'instant de l'assignation, présences manuelles antérieures incluses.
- Quand aucun étudiant éligible n'est disponible, alors l'exercice reste au statut `EN_ATTENTE_RELECTURE` sans relecteur.
- Quand l'étudiant est l'auteur de l'exercice, alors il ne peut jamais être désigné comme son relecteur.
- Quand le relecteur consulte sa mission, alors seule sa propre identité (`relecteurId`) donne accès à l'exercice assigné (RG6).

**Endpoints :** effet de bord de `POST /api/exercices` — observable via `GET /api/exercices/{exerciceId}/relecteur` et `GET /api/relecteurs/{etudiantId}/relectures-en-attente`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test unitaire sur l'assignation : l'auteur est exclu du pool, le relecteur est un étudiant présent
- [ ] Test d'intégration sur `GET /api/exercices/{exerciceId}/relecteur` : `200` pour le relecteur assigné, `403` pour un autre appelant
- [ ] Documentation impactée à jour (`docs/diagrammes/D2_class_diagram.md` — contrainte RG4)
EOF
)"

# ---------------------------------------------------------------- Issue 5 (Must) EF6
gh issue create \
  --title "Le relecteur note et commente l'exercice qui lui est assigné" \
  --label "must,ef6" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF6 · Règles RG7, RG4 · **Priorité** Must · **Estimation** 2h

### Critères d'acceptation
- Quand le relecteur envoie une note entière entre 0 et 20 et un commentaire pour une relecture qui lui est assignée, alors la relecture passe au statut `RELU` et l'API répond `200`.
- Quand la note est hors 0–20 ou non entière, alors l'API répond `400 NOTE_INVALIDE`.
- Quand la relecture visée concerne son propre exercice, alors l'API répond `403 AUTO_RELECTURE`.

**Endpoints :** `POST /api/relectures/{id}` · `GET /api/relecteurs/{etudiantId}/relectures-en-attente` · `GET /api/exercices/{exerciceId}/relecteur`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `POST /api/relectures/{id}` : `200` / `400 NOTE_INVALIDE` / `403 AUTO_RELECTURE`
- [ ] Le relecteur retrouve sa mission via `GET /api/relecteurs/{etudiantId}/relectures-en-attente` et `GET /api/exercices/{exerciceId}/relecteur` (RG6)
- [ ] Documentation impactée à jour (`api/contrat.yaml`, `docs/diagrammes/D2_class_diagram.md`)
EOF
)"

# ---------------------------------------------------------------- Issue 6 (Must) EF8
gh issue create \
  --title "L'étudiant relu consulte sa note sans connaître son relecteur" \
  --label "must,ef8" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF8 · Règles RG6, RG9 · **Priorité** Must · **Estimation** 1h

### Critères d'acceptation
- Quand une relecture a été rendue, alors l'étudiant voit la note et le commentaire reçus.
- Quand l'étudiant consulte sa note, alors l'identité du relecteur n'apparaît dans aucune réponse de l'API.
- Quand aucune relecture n'est encore rendue, alors son exercice est affiché « en attente », sans note.

**Endpoints :** `GET /api/etudiants/{etudiantId}/relectures-recues`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `GET /api/etudiants/{etudiantId}/relectures-recues` : aucun `relecteurId` dans la réponse (RG6)
- [ ] Documentation impactée à jour (`api/contrat.yaml`)
EOF
)"

# ---------------------------------------------------------------- Issue 7 (Must) EF9
gh issue create \
  --title "Le formateur consulte le tableau de bord de sa promotion" \
  --label "must,ef9" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF9 · Règles RG9, RG5 · **Priorité** Must · **Estimation** 3h

### Critères d'acceptation
- Quand le formateur demande le tableau d'une promotion, alors il obtient par étudiant : nombre de présences, exercices déposés, moyenne des notes reçues et relectures en attente.
- Quand un étudiant de la promotion n'a aucune activité, alors il apparaît quand même dans le tableau (tous les étudiants sont listés, pas seulement les actifs).
- Quand aucune note n'a été reçue, alors la moyenne renvoyée est `null` et non `0`.
- Quand la moyenne est affichée, alors elle provient de l'API, jamais d'un recalcul côté client.
- Quand la promotion est inconnue, alors l'API répond `404 PROMOTION_INCONNUE`.

**Endpoints :** `GET /api/tableau` · `GET /api/promotions/{promotionId}/etudiants`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `GET /api/tableau` : moyenne `null` sans note, `404 PROMOTION_INCONNUE`
- [ ] Test : un étudiant sans activité figure bien dans le tableau (croisement avec `GET /api/promotions/{promotionId}/etudiants`)
- [ ] Documentation impactée à jour (`api/contrat.yaml`)
EOF
)"

# ---------------------------------------------------------------- Issue 8 (Must) EF11
gh issue create \
  --title "Le formateur clôture une session et fige les dépôts et les notes" \
  --label "must,ef11" --milestone "v0.1" \
  --body "$(cat <<'EOF'
**Réf.** EF11 · Règles RG14, RG8 · **Priorité** Must · **Estimation** 1h

### Critères d'acceptation
- Quand le formateur clôture une session, alors plus aucun dépôt ni remplacement de lien n'est accepté sur cette session.
- Quand le formateur clôture une session, alors plus aucune note ni commentaire ne peut être créé ou corrigé sur cette session.
- Quand des relectures restent en attente, alors la clôture réussit quand même et ces relectures restent définitivement en attente.

**Endpoints :** `POST /api/sessions/{id}/cloture`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test unitaire : après clôture, un dépôt et une notation sont refusés (RG14)
- [ ] Documentation impactée à jour (`api/contrat.yaml`)
EOF
)"

# ---------------------------------------------------------------- Issue 9 (Should) EF4
gh issue create \
  --title "L'étudiant remplace le lien de son exercice tant qu'il n'est pas relu" \
  --label "should,ef4" \
  --body "$(cat <<'EOF'
**Réf.** EF4 · Règles RG11, RG10, RG14 · **Priorité** Should · **Estimation** 1h30

### Critères d'acceptation
- Quand aucune relecture n'a été rendue sur son exercice, alors l'étudiant peut soumettre un nouveau lien qui remplace l'ancien.
- Quand une relecture a été rendue, alors le remplacement est refusé.
- Quand la session est clôturée, alors le remplacement est refusé.
- Quand le lien de remplacement est mal formé, alors l'API répond `400 LIEN_INVALIDE`.

**Endpoints :** `PUT /api/exercices/{id}/lien`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `PUT /api/exercices/{id}/lien` : `200` / `409` une fois la relecture rendue
- [ ] Documentation impactée à jour (`api/contrat.yaml`)
EOF
)"

# ---------------------------------------------------------------- Issue 10 (Should) EF7
gh issue create \
  --title "Le relecteur corrige sa note avant la clôture de la session" \
  --label "should,ef7" \
  --body "$(cat <<'EOF'
**Réf.** EF7 · Règles RG8, RG14 · **Priorité** Should · **Estimation** 1h30

### Critères d'acceptation
- Quand la session n'est pas clôturée, alors le relecteur peut renvoyer une nouvelle note et un nouveau commentaire pour une relecture qu'il a déjà rendue.
- Quand il corrige, alors la nouvelle note remplace l'ancienne et la relecture reste au statut `RELU`.
- Quand il corrige, alors la note précédente est conservée dans l'historique des corrections.
- Quand la session est clôturée, alors toute correction est refusée (`409 SESSION_CLOTUREE`).

**Endpoints :** `PUT /api/relectures/{id}/correction`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `PUT /api/relectures/{id}/correction` : `200` avant clôture, `409` après
- [ ] Test unitaire : l'historique conserve l'ancienne note (RG8)
- [ ] Documentation impactée à jour (`api/contrat.yaml`, `docs/diagrammes/D2_class_diagram.md`)
EOF
)"

# ---------------------------------------------------------------- Issue 11 (Should) EF10
gh issue create \
  --title "Le formateur ajoute manuellement une présence" \
  --label "should,ef10" \
  --body "$(cat <<'EOF'
**Réf.** EF10 · Règles RG12, RG13 · **Priorité** Should · **Estimation** 1h

### Critères d'acceptation
- Quand le formateur ajoute une présence pour un étudiant, alors elle est enregistrée avec `source = FORMATEUR`.
- Quand l'étudiant est déjà marqué présent pour cette session, alors l'ajout est refusé (`409 DEJA_PRESENT`).
- Quand la présence manuelle précède l'assignation d'un relecteur, alors cet étudiant entre dans le pool des relecteurs éligibles.

**Endpoints :** `POST /api/presences/manuelles` · prérequis `GET /api/promotions/{promotionId}/etudiants`

### Definition of Done
- [ ] PR fusionnée sur `main` fermant l'issue (`Closes #N`)
- [ ] Test d'intégration sur `POST /api/presences/manuelles` : `201` avec `source = FORMATEUR`, `409 DEJA_PRESENT`
- [ ] Documentation impactée à jour (`api/contrat.yaml`)
EOF
)"

echo "✅ 11 issues créées (8 Must avec milestone v0.1, 3 Should sans milestone)."
echo "Rappel : 13 endpoints du contrat sont référencés dans les corps d'issues."
