# D1 — Cas d'utilisation

> **Acteurs** : `Formateur`, `Étudiant`.
> Il n'existe **pas d'acteur « Relecteur » distinct** : un étudiant agit comme relecteur
> lorsqu'un exercice lui a été assigné (EF5 → EF6 / EF7).

```mermaid
flowchart LR
    %% ---------- Acteurs ----------
    Formateur(("Formateur"))
    Etudiant(("Étudiant"))

    subgraph APP["Application KFOKAM48"]
        direction TB

        subgraph SESS["Sessions"]
            EF1(["EF1 · Ouvrir une session et obtenir un code de présence"])
            EF11(["EF11 · Clôturer une session"])
        end

        subgraph PRES["Présences"]
            EF2(["EF2 · Marquer sa présence avec le code"])
            EF10(["EF10 · Ajouter une présence manuellement"])
        end

        subgraph EXO["Exercices"]
            EF3(["EF3 · Déposer le lien de son exercice"])
            EF4(["EF4 · Remplacer le lien de son exercice"])
            EF5(["EF5 · Assigner automatiquement un relecteur"])
        end

        subgraph RL["Relecture — rôle porté par un étudiant"]
            EF6(["EF6 · Noter et commenter l'exercice assigné"])
            EF7(["EF7 · Corriger sa note avant la clôture"])
        end

        subgraph SUIVI["Suivi"]
            EF8(["EF8 · Consulter sa note et le commentaire reçu"])
            EF9(["EF9 · Consulter le tableau de bord de la promotion"])
        end
    end

    %% ---------- Formateur ----------
    Formateur --> EF1
    Formateur --> EF10
    Formateur --> EF9
    Formateur --> EF11

    %% ---------- Étudiant ----------
    Etudiant --> EF2
    Etudiant --> EF3
    Etudiant --> EF4
    Etudiant --> EF8

    %% Un étudiant joue le rôle de relecteur (aucun acteur Relecteur séparé)
    Etudiant -->|dans le rôle de relecteur| EF6
    Etudiant -->|dans le rôle de relecteur| EF7

    %% ---------- Dépendances entre cas d'usage ----------
    EF3 -.->|«include»| EF5
    EF5 -.->|assigne| EF6
```
