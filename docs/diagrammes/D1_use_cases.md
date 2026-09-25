# D1 — Cas d'utilisation
```mermaid
flowchart TD
    Etudiant((Étudiant))
    Formateur((Formateur))

    subgraph Application KFOKAM48
        UC1[Ouvrir une session & Générer code]
        UC2[Consulter le tableau de bord]
        UC3[Ajouter une présence manuellement]
        UC4[Saisir le code de présence]
        UC5[Déposer le lien de l'exercice]
        UC6[Faire la relecture d'un pair]
    end

    Formateur --> UC1
    Formateur --> UC2
    Formateur --> UC3
    Etudiant --> UC4
    Etudiant --> UC5
    Etudiant --> UC6