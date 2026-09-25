# Cahier des charges — KFOKAM48 Fullstack
Auteur : Candidat · Version 1 · Frontend choisi : React, pour sa grande écosystème et sa réactivité pour les tableaux de bord.

## 1. Contexte et objectif
L'application vise à automatiser la gestion de la présence en cours, le dépôt d'exercices pratiques, la relecture croisée par les pairs (peer-reviewing) et la restitution synthétique sous forme de tableau de bord pour la direction de formation KFOKAM48.

## 2. Acteurs et rôles
| Acteur | Ce qu'il peut faire |
| :--- | :--- |
| **Formateur** | Ouvrir une session de cours, obtenir un code de présence, visualiser le tableau de suivi global, ajouter une présence manuellement. |
| **Étudiant** | Saisir un code de présence, déposer le lien de son exercice, relire anonymement l'exercice d'un pair avec note et commentaire. |

## 3. Périmètre
* **Inclus :** Ouverture des sessions, validation par code temporaire, dépôt de liens d'exercice, attribution anonyme de relecture, tableau de bord agrégé.
* **Exclu :** Authentification par mot de passe (Q1), stockage direct des fichiers d'exercice.

## 4. Exigences fonctionnelles
| Réf | Exigence | Critère d'acceptation | Priorité |
| :--- | :--- | :--- | :--- |
| **EF1** | Enregistrement de présence | L'étudiant saisit un code valide dans les 15 min suivant l'ouverture. | Must |
| **EF2** | Dépôt d'exercice | L'étudiant dépose l'URL de son travail tant que la session n'est pas clôturée. | Must |
| **EF3** | Relecture par pair | Le système attribue un exercice à un étudiant présent pour évaluation (note 0-20 + commentaire). | Must |
| **EF4** | Dashboard Formateur | Affichage de la présence, du nombre d'exercices, de la moyenne et des relectures en attente. | Must |
| **EF5** | Ajout de présence manuel | Le formateur peut forcer une présence avec la source `FORMATEUR`. | Should |

## 5. Exigences non fonctionnelles
| Réf | Exigence | Comment on la vérifie |
| :--- | :--- | :--- |
| **ENF1** | Performance API | Temps de réponse < 200ms sur `/api/tableau`. |
| **ENF2** | Gestion d'erreurs | Réponse sous le format `{ "code": "...", "message": "..." }` sans stacktrace. |
| **ENF3** | Traçabilité | Schéma de base de données versionné via Flyway. |

## 6. Règles de gestion
| Réf | Règle | Source |
| :--- | :--- | :--- |
| **RG1** | Un code de présence expire 15 minutes après l'ouverture de la session. | Q2 |
| **RG2** | Blocage de 2 minutes après 5 tentatives de code erronées. | Q4 |
| **RG3** | Un étudiant ne peut pas relire son propre exercice. | Q5 |
| **RG4** | Un exercice a un seul relecteur. | Q6 |
| **RG5** | Relecteur attribué au hasard parmi les étudiants présents. | Q7, Q8 |
| **RG6** | La note est un entier de 0 à 20. | Q9 |
| **RG7** | Relecture modifiable tant que la session n'est pas clôturée par le formateur. | Q10 |
| **RG8** | Remplacement du lien d'exercice possible tant qu'aucune relecture n'a commencé. | Q13 |

## 7. Zones d'ombre, hypothèses et contradictions
| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Pourquoi |
| :--- | :--- | :--- | :--- |
| **Contradiction Q10 / Q15** | Q10 autorise l'édition avant clôture, Q15 dit que la note est définitive. | **Décision Q10 :** Note éditable tant que la session reste ouverte. | Offre de la souplesse en cas d'erreur de saisie de l'étudiant. |
| **Absence de relecteur** | Aucun autre étudiant présent lors du dépôt d'exercice. | **Décision :** Attribution dynamique différée au moment où un autre étudiant est marqué présent. | Évite de bloquer la soumission d'exercices. |

## 8. Contraintes techniques
Backend Java 17 / Spring Boot / Flyway, Frontend React, contrat OpenAPI respecté.

## 9. Livrables
Code source sur GitHub, migrations Flyway SQL, documentation Markdown.

## 10. Démarche prévue
* **Definition of Done :** Code fonctionnel, contrat d'API validé, PR fusionnée sur `main`.