package com.kfokam48.app.features.promotion.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Promotion (diagramme D2). Dans le périmètre de l'issue #8, elle sert uniquement
 * à vérifier que la promotion d'une session à ouvrir existe.
 */
@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 120)
    private String nom;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    protected Promotion() {
        // requis par JPA
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Integer getAnnee() {
        return annee;
    }
}
