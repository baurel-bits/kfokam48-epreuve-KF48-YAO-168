# Cahier des charges — KFOKAM48 (gestion de présence, exercices et relectures)

Auteur : `168` · Version 1 · Frontend choisi : `Next.js`, parce que `Je le trouve plus abordable `


## 1. Contexte et objectif

La direction de la formation KFOKAM48 souhaite outiller trois moments récurrents de ses sessions de cours : la prise de présence, le dépôt d'exercices par les étudiants, et la relecture par les pairs. Aujourd'hui ces trois étapes sont gérées de façon informelle (papier, messages), ce qui rend le suivi du formateur peu fiable et les relectures difficiles à tracer.

L'objectif de l'application est de permettre à un formateur d'ouvrir une session et de suivre, pour chaque étudiant, sa présence, ses dépôts d'exercices, sa moyenne de notes reçues et l'état de ses relectures — pendant qu'un étudiant peut marquer sa présence, déposer son exercice, et relire l'exercice d'un pair qui lui est assigné.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire |
|---|---|
| Formateur | Ouvre une session et obtient un code de présence ; ajoute une présence manuellement ; clôture une session ; consulte le tableau de bord de sa promotion |
| Étudiant | Saisit un code pour marquer sa présence ; dépose (ou remplace) le lien de son exercice ; consulte sa note et le commentaire reçu sur son exercice |
| Relecteur | C'est un étudiant, assigné automatiquement à la relecture de l'exercice d'un pair ; note (0–20, entier) et commente ; peut corriger sa note tant que la session n'est pas clôturée |

Le relecteur n'est pas un rôle applicatif distinct : c'est un étudiant dans un contexte précis (une relecture qui lui a été assignée).

## 3. Périmètre

**Inclus**
- Ouverture de session et génération d'un code de présence à durée limitée
- Marquage de présence par code, et ajout manuel par le formateur
- Dépôt et remplacement du lien d'un exercice
- Assignation automatique et aléatoire d'un relecteur par exercice
- Notation et commentaire d'une relecture, avec correction possible avant clôture
- Tableau de bord du formateur par promotion
- Clôture de session par le formateur

**Exclu**
- Authentification par mot de passe (Q1 — l'étudiant est choisi dans une liste)
- Consultation par l'étudiant relu de l'identité de son relecteur (Q8)
- Gestion de plusieurs relecteurs par exercice (Q6 — un seul)
- Habillage graphique / CSS avancé (hors barème selon le sujet)
- Notifications (email, push) — non demandées par le client

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | Quand je crée une session avec un titre et une promotion, je reçois un code, une heure d'ouverture et une heure d'expiration à +15 min | Must |
| EF2 | L'étudiant marque sa présence avec un code | Quand je saisis un code valide et non expiré, ma présence apparaît dans le tableau du formateur avec `source = ETUDIANT` | Must |
| EF3 | L'étudiant dépose le lien de son exercice | Quand je soumets un lien valide pour une session où j'ai déposé aucun exercice, l'exercice est créé avec le statut `déposé` | Must |
| EF4 | L'étudiant remplace le lien de son exercice | Quand aucune relecture n'a commencé sur mon exercice, je peux soumettre un nouveau lien qui remplace l'ancien | Should |
| EF5 | Le système assigne un relecteur à chaque exercice déposé | Quand un exercice est déposé, un relecteur est choisi au hasard parmi les étudiants présents à la session, différent de l'auteur | Must |
| EF6 | Le relecteur note et commente l'exercice assigné | Quand j'envoie une note entière entre 0 et 20 et un commentaire pour une relecture qui m'est assignée, la relecture passe au statut `relu` | Must |
| EF7 | Le relecteur corrige sa note avant clôture de la session | Quand la session n'est pas clôturée, je peux renvoyer une nouvelle note/commentaire pour une relecture que j'ai déjà rendue | Should |
| EF8 | L'étudiant relu consulte sa note | Quand ma relecture est rendue, je vois la note et le commentaire, sans le nom du relecteur | Must |
| EF9 | Le formateur consulte le tableau de bord de sa promotion | Quand je demande le tableau d'une promotion, je vois par étudiant : présences, exercices déposés, moyenne des notes, relectures en attente | Must |
| EF10 | Le formateur ajoute une présence manuellement | Quand j'ajoute une présence pour un étudiant, elle apparaît avec `source = FORMATEUR` | Should |
| EF11 | Le formateur clôture une session | Quand je clôture une session, plus aucune note ni aucun dépôt d'exercice ne peut être modifié pour cette session | Must |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | Usage mobile en priorité côté étudiant (saisie de code en session) | Écrans étudiant utilisables sur un viewport ≤ 400px de large, sans scroll horizontal |
| ENF2 | Volumétrie modeste | L'application doit rester fonctionnelle pour une promotion de 40 étudiants et une dizaine de sessions actives simultanément |
| ENF3 | Temps de réponse | Les endpoints du contrat répondent en moins d'1 seconde en conditions normales (poste de démo, données de test) |
| ENF4 | Disponibilité des messages d'erreur | Toute erreur métier renvoie le format `{ code, message }` imposé, jamais une stack trace |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| RG2 | Un code invalide ou expiré est refusé (400 code inconnu, 410 code expiré) | Q2, Q3 |
| RG3 | Après 5 échecs de saisie de code, l'étudiant est bloqué 2 minutes | Q4 |
| RG4 | Un étudiant ne peut pas relire son propre exercice | Q5 |
| RG5 | Un exercice a un seul relecteur, choisi au hasard parmi les étudiants présents à la session au moment de l'assignation | Q6, Q7 |
| RG6 | L'étudiant relu voit sa note et le commentaire, jamais l'identité du relecteur | Q8 |
| RG7 | La note est un entier compris entre 0 et 20 | Q9 |
| RG8 | Le relecteur peut corriger une relecture déjà rendue tant que le formateur n'a pas clôturé la session (contradiction Q10/Q15 tranchée, voir section 7) | Q10 |
| RG9 | Un exercice sans relecture rendue reste au statut `en attente` et doit apparaître comme tel dans le tableau du formateur | Q11 |
| RG10 | Un exercice peut être déposé jusqu'à la clôture de la session, même après la fin (expiration du code) de celle-ci | Q12 |
| RG11 | Le lien d'un exercice peut être remplacé tant qu'aucune relecture n'a été commencée dessus | Q13 |
| RG12 | Le formateur peut ajouter une présence manuellement ; elle est marquée `source = FORMATEUR` pour rester distinguable | Q14 |
| RG13 | Le pool des relecteurs éligibles pour un exercice est recalculé au moment de l'assignation de ce relecteur, et non figé à la fin de la session (voir zone d'ombre, section 7) | déduit de Q7 + Q12 |
| RG14 | La clôture d'une session est possible même si des relectures restent en attente ; elle gèle ensuite tout dépôt et toute note sur cette session | déduit de Q11 + Q10/Q15 |

## 7. Zones d'ombre, hypothèses et contradictions tranchées

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
|---|---|---|---|
| Modification d'une note après envoi | Q10 dit que le relecteur peut corriger tant que la session n'est pas clôturée ; Q15 dit que la note est définitive dès l'envoi | **Q10 fait foi** (RG8) : le relecteur peut corriger jusqu'à la clôture de la session | Q10 répond à une situation concrète déjà rencontrée par le client (un relecteur qui se trompe) ; Q15 exprime une intention générale de principe, formulée avant que le cas réel ne soit remonté. Une réponse à un cas vécu prime sur une déclaration de principe antérieure. La clôture de session (déjà présente dans le besoin via Q10/Q11/Q12) devient donc le seul verrou de fin de modification |
| Éligibilité du relecteur pour un exercice déposé tardivement | Q7 : relecteur choisi parmi les présents à la session ; Q12 : un exercice peut être déposé après la fin de la session, jusqu'à la clôture. Aucune question ne précise à quel instant le pool de présents est évalué | **Trou identifié** — RG13 : le pool des étudiants éligibles est recalculé à l'instant où l'assignation a lieu (au dépôt de l'exercice), pas figé à la fin de la session. Un ajout manuel de présence (Q14) fait donc partie du pool s'il précède l'assignation | Comme les dépôts peuvent arriver tard (Q12), figer le pool à la fin de la session risquerait de le vider ou de le rendre incohérent avec les présences ajoutées ensuite. Recalculer au moment de l'assignation garde une règle simple et cohérente avec Q7 |
| Sort des relectures non rendues à la clôture | Q11 : l'exercice reste "en attente" et doit rester visible ainsi | **Décision** (RG14) : la clôture n'oblige pas à ce que toutes les relectures soient rendues ; une relecture en attente au moment de la clôture le reste définitivement (plus aucune notation possible ensuite) | Q11 décrit ce cas comme un état normal à afficher, pas comme un blocage à la clôture. Rien n'indique que le formateur doive attendre toutes les relectures pour clôturer |
| Sens de « fin de session » (Q3) vs expiration du code (Q2) | Q3 interdit de marquer sa présence après la fin de la session, Q2 fixe une expiration de code à 15 min | **Décision** : « fin de session » pour la présence désigne l'expiration du code (RG1/RG2) ; il n'existe pas de notion de fin de session distincte de l'expiration du code pour cette règle précise | Aucune autre notion de durée de session n'est mentionnée pour la présence ; Q2 et Q3 décrivent donc la même limite vue sous deux angles |
| Blocage de 5 erreurs (Q4) — par étudiant ou par session | Q4 ne précise pas la portée du compteur d'erreurs | **Décision** : le compteur est par couple (étudiant, session) — un étudiant bloqué sur une session peut toujours saisir un code pour une autre session | Empêche qu'un étudiant bloqué sur une session ancienne soit pénalisé sur une session en cours ; limite le risque d'un blocage global abusif |

## 8. Contraintes techniques

Imposées par le sujet, non négociables :

- **Backend** : Java 17+, Spring Boot, Maven avec wrapper `mvnw` commité (B1) ; respect strict de `api/contrat.yaml` (B2) ; séparation contrôleur / service / repository, DTO uniquement en sortie (B3) ; validation des entrées + `@RestControllerAdvice` centralisé, jamais de stack trace exposée (B4) ; migrations Flyway ou Liquibase versionnées, `ddl-auto=update` interdit hors tests (B5) ; au moins un test unitaire sur une règle métier réelle et un test d'intégration sur un endpoint, exécutables sur poste vierge (B6)
- **Frontend** : React, Angular ou Next.js, choix justifié dans le README (F1) ; trois écrans — formateur, étudiant, relecteur (F2) ; couche d'appel API dédiée, gestion des états de chargement/erreur, aucun recalcul de règle métier côté client (moyenne fournie par l'API) (F3)
- **Démarrage** : `docker compose up` ou trois commandes documentées et testées depuis un clone vierge, avec données de démonstration chargées au démarrage
- **Format d'erreur imposé** : `{ "code": "...", "message": "..." }` pour toute erreur, sans exception

## 9. Livrables

- Dépôt GitHub public `kfokam48-epreuve-<matricule>` avec la structure imposée (`/docs`, `/api`, `/backend`, `/frontend`)
- `docs/CAHIER_DES_CHARGES.md` (ce document)
- `docs/diagrammes/` : D1 (cas d'utilisation), D2 (classes / modèle de données), D3 (séquence présence), en Mermaid
- Backlog sous forme d'issues GitHub, priorisées et reliées aux EFx/RGx
- `api/contrat.yaml` complété
- Backend Spring Boot conforme à B1–B6, migrations versionnées
- Frontend conforme à F1–F3, trois écrans
- `CHANGELOG.md`, `README.md` d'installation testé, `JOURNAL.md` tenu au fil de l'eau
- `SOUMISSION.md` rempli au dépôt final

## 10. Démarche prévue

1. **Analyse** (cette étape) : cahier des charges, diagrammes, backlog, contrat API — commit `[JALON] analyse` avant tout code
2. **v0.1** : implémentation des issues Must uniquement (EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11), une branche par issue, PR liée par `Closes #N` — commit `[JALON] v0.1`
3. **Enveloppe** : récupération du bug et du changement de besoin auprès du surveillant, issue ouverte avant correction, migration versionnée, contrat et documentation mis à jour dans des commits séparés du correctif
4. **v1.0** : finalisation, `CHANGELOG.md`, `README.md` vérifié depuis un clone vierge — commit `[JALON] v1.0`
5. **Soumission** : commit final, hash relevé, `SOUMISSION.md` déposé avant 18h00

**Definition of Done** : une issue est terminée quand son code est fusionné sur `main` via une PR qui la ferme, que ses critères d'acceptation sont vérifiés manuellement ou par un test, et que la documentation impactée (contrat API, diagrammes) est à jour.

## 11. Trous identifiés après revue du contrat d'API (décisions)

Ces points n'étaient pas explicitement tranchés par le sujet ou par le contrat ; ils le sont ici
et répercutés dans `api/contrat.yaml`.

| Point | Constat | Décision retenue |
|---|---|---|
| **EF5** — assignation automatique d'un relecteur | Le contrat n'impose aucune opération d'assignation : c'est un **effet de bord** du dépôt (`POST /api/exercices`), conformément à RG5/RG13. | Aucune opération d'écriture ajoutée. L'assignation est observable par le relecteur via `GET /api/relecteurs/{etudiantId}/relectures-en-attente` et `GET /api/exercices/{exerciceId}/relecteur`. |
| **RG3** — blocage après 5 échecs de saisie | Le contrat ne prévoyait **aucun statut ni code** pour une saisie effectuée pendant le blocage de 2 minutes. | **`429 TROP_DE_TENTATIVES`** renvoyé par `POST /api/presences`, contrôlé **avant** la validation du code. Message : « Trop de tentatives : réessayez dans 2 minutes. » Compteur à **deux portées** : par couple (étudiant, session) quand le code est attribuable à une session (expiré), et par étudiant (`session_id IS NULL`, migration `V3`) quand le code est inconnu — le contrat n'envoyant que `{ code, etudiantId }`, un code erroné n'est rattachable à aucune session ; sans cette seconde portée, cinq codes erronés (l'échec le plus courant) ne déclenchaient aucun blocage. Les codes imposés `400 CODE_INCONNU`, `409 DEJA_PRESENT` et `410 CODE_EXPIRE` restent **inchangés** : le `429` est un **ajout**. |
| **Liste des étudiants d'une promotion** | Q1 exclut l'authentification par mot de passe (l'étudiant est choisi dans une liste) mais **aucun endpoint** ne fournissait cette liste. | Ajout de **`GET /api/promotions/{promotionId}/etudiants`**, prérequis explicite de EF2, EF3 et EF10 (les `etudiantId` doivent venir de quelque part). |
| **EF6** — note rendue sur une relecture inconnue | L'opération **imposée** `POST /api/relectures/{id}` ne déclare que `400`, `403` et `409` : aucun statut pour un identifiant qui n'existe pas. | **`404 RELECTURE_INCONNUE`**, ajouté pour qu'une erreur de saisie ne devienne pas un `500`. Le `403 AUTO_RELECTURE` reste implémenté en **garde-fou**, mais il est **inatteignable** par l'API : RG4 est déjà garantie à l'assignation (l'auteur est écarté du pool) et par le `CHECK (relecteur_id <> auteur_id)`. |

> ⚠️ **Limite de sécurité assumée** : le sujet excluant l'authentification (Q1), la
> restriction « réservé au relecteur » (RG6) repose uniquement sur la comparaison du
> `relecteurId` fourni avec `relecture.relecteur_id`. Ce n'est **pas** un véritable
> contrôle d'accès : cela évite l'affichage accidentel de l'identité du relecteur à
> l'étudiant relu, mais ne résiste pas à un appel volontairement falsifié.