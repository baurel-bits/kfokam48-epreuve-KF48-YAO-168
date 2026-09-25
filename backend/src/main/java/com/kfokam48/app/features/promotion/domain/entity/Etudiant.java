package com.kfokam48.app.features.promotion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Étudiant d'une promotion (diagramme D2). Dans le périmètre de l'issue #9,
 * il sert à alimenter la liste des étudiants (prérequis Q1 : le sujet exclut
 * l'authentification, l'étudiant est choisi dans une liste) et à valider
 * l'{@code etudiantId} d'une présence.
 */
@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prenom", nullable = false, length = 80)
    private String prenom;

    @Column(name = "nom", nullable = false, length = 80)
    private String nom;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    protected Etudiant() {
        // requis par JPA
    }

    public Long getId() {
        return id;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public Long getPromotionId() {
        return promotionId;
    }
}
