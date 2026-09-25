# Journal de bord — KFOKAM48

## Étape 1 — Analyse et conception
- **Fait :** Rédaction complète du cahier des charges (`docs/CAHIER_DES_CHARGES.md`), modélisation des 4 diagrammes Mermaid (D1 à D4), création de la structure du projet et validation du contrat d'API.
- **Bloqué :** 15 minutes d'analyse pour trancher la contradiction Q10/Q15. Option retenue : Q10 pour permettre la modification avant clôture.
- **IA :** Proposition de 15 règles de gestion par l'IA, conservé uniquement 8 règles directement issues du besoin réel. Vérification manuelle effectuée sur la cohérence des codes HTTP HTTP 400/409/410.

## Étape 2 — v0.1, périmètre Must (EF1, EF2, EF3, EF5, EF6, EF8, EF9, EF11)
- **Fait :** Les 8 exigences `Must` implémentées, une branche par issue et une PR qui la ferme (PR #19 à #25, puis #27). Trois couches séparées côté backend (contrôleur / service / repository, DTO en sortie), trois migrations Flyway versionnées, trois écrans Next.js. RG3 rendu réellement effectif sur les codes **inconnus** (le cas le plus fréquent) par un compteur à deux portées et la migration `V3` ; Flyway fiabilisé sur un schéma préexistant non vide (`baseline-version: 0`) ; page d'accueil servie à la racine.
- **Bloqué :** Outillage local incompatible avec le JDK 26 du poste. Lombok inopérant → getters, setters et builders écrits explicitement dans toutes les entités et tous les DTO. Mockito ne parvient pas à simuler une classe concrète → l'assignation du relecteur (EF5) a été isolée derrière une interface (`AssignateurRelecteur`) pour rester testable.
- **IA :** L'IA a proposé le code et les tests des huit EF ; chaque proposition a été rejouée avant acceptation. Deux pièges ont été trouvés par les tests et non par la relecture : le compteur RG3 aveugle aux codes inconnus, et le démarrage Flyway sur un schéma non vide. Les tests d'intégration partagent une base H2 entre classes : chaque test filtre donc sa propre entité au lieu d'attester un comptage global, qui deviendrait faux dès qu'une autre classe s'exécute.

## Étape 3 — Enveloppe : issues Should et correctif de session
- **Fait :** Les trois exigences `Should` réalisées après la v0.1, chacune sous sa propre issue ouverte avant son code : EF4 (issue #16, PR #30) — remplacer le lien d'un exercice ; EF7 (issue #17, PR #29) — corriger une note avant la clôture ; EF10 (issue #18, PR #28) — ajouter une présence manuellement. Deux statuts absents du contrat ont été ajoutés pour ne pas transformer une erreur de saisie en `500` : `404 RELECTURE_INCONNUE` (EF6) et `409 RELECTURE_NON_RENDUE` (EF7). Correctif d'usage : le code de session ne survivait pas au rechargement de la page alors qu'il reste valable 15 min (PR #26).
- **Bloqué :** Pour l'EF4, l'ébauche d'issue ouvrait le remplacement de lien jusqu'à la note **rendue**, ce que le contrat contredisait (`409 RELECTURE_COMMENCEE`, jamais `RELECTURE_RENDUE`) — RG11 (Q13) parle d'« aucune relecture commencée ». Arbitrage : la règle source et le contrat font foi, l'ébauche est abandonnée, et sa conséquence (EF4 atteignable seulement sur un exercice resté `DEPOSE`) est assumée et documentée.
- **IA :** L'IA a proposé les trois EF et le correctif ; la contradiction de l'EF4 n'a pas été détectée par elle mais en relisant RG11 et le contrat avant d'écrire le test. Les points non tranchés par le sujet ont été arbitrés en revenant au texte (RG11, RG12, RG14, Q1) et regroupés en section 11 du cahier des charges, plutôt qu'en suivant la proposition initiale.

## Étape 4 — v1.0 : finalisation
- **Fait :** Retour complet sur le livrable. `CHANGELOG.md` rédigé (rétrospectif par jalon), `SOUMISSION.md` rempli, `README.md` mis à jour (tableau des trois écrans, section « Vérifications », prérequis, limites) puis **rejoué depuis un clone vierge** : les trois commandes documentées (conteneur PostgreSQL, `./mvnw spring-boot:run`, `npm install && npm run dev`) démarrent l'application et les données de démonstration sont présentes. Vérifications rejouées : **161 tests, 0 échec**, `tsc --noEmit` et `npm run build` sans erreur, parcours navigateur **44/44**. Les 11 issues du backlog (#8 à #18) fermées avec un commentaire de preuve (livrable, tests, branche, PR de fusion).
- **Bloqué :** Aucun blocage. Deux arbitrages de forme : le contrat d'API est resté intact (aucune de ses opérations n'a été modifiée ni ajoutée depuis l'analyse), et les écarts touchant une ébauche d'issue — et non le contrat — sont documentés en section 11 plutôt que répercutés dans `api/contrat.yaml`.
- **IA :** L'IA a rédigé la documentation et déroulé la relecture ; chaque affirmation du README a été vérifiée en l'exécutant, pas en la relisant — c'est ce qui a fait ressortir que le parcours d'installation n'avait encore jamais été testé sur un clone neuf. La limite du §7 de `SOUMISSION.md` (RG6 sans véritable contrôle d'accès) a été maintenue explicitement plutôt que lissée.

## Étape 5 — Soumission
- **Fait :** Commit final `[JALON] v1.0`, hash relevé et reporté dans `SOUMISSION.md`, dépôt public sur `main` à jour.
- **Bloqué :** Aucun.
- **IA :** Aucun élément produit par l'IA n'a été soumis sans avoir été exécuté sur la machine de vérification.
