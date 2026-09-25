package com.kfokam48.app.features.relecture.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Relecture confiée à un étudiant (EF5, EF6, EF7, EF8 ; diagramme D2).
 *
 * <p>Deux garanties sont portées par la base et non par ce code :
 * <ul>
 *   <li>{@code uk_relecture_exercice} : un exercice n'a qu'<strong>un seul</strong>
 *       relecteur (RG5) ;</li>
 *   <li>{@code ck_relecture_pas_auto_relecture} et la clé étrangère composite
 *       {@code (exercice_id, auteur_id)} : l'auteur ne peut pas être son propre
 *       relecteur (RG4), et {@code auteurId} est bien l'auteur de l'exercice.</li>
 * </ul>
 *
 * <p>{@code auteurId} est dénormalisé depuis l'exercice uniquement pour rendre
 * RG4 vérifiable en SQL.
 */
@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercice_id", nullable = false, unique = true)
    private Long exerciceId;

    @Column(name = "relecteur_id", nullable = false)
    private Long relecteurId;

    @Column(name = "auteur_id", nullable = false)
    private Long auteurId;

    @Column(name = "note")
    private Integer note;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutRelecture statut;

    @Column(name = "rendu_at")
    private LocalDateTime renduAt;

    protected Relecture() {
        // requis par JPA
    }

    /** Relecture attribuée, en attente de note (EF5). */
    public Relecture(Long exerciceId, Long relecteurId, Long auteurId) {
        this.exerciceId = exerciceId;
        this.relecteurId = relecteurId;
        this.auteurId = auteurId;
        this.statut = StatutRelecture.EN_ATTENTE;
    }

    /**
     * EF6 : le relecteur rend sa note et son commentaire. La note a déjà été
     * contrôlée par le service (RG7) et l'est de nouveau par
     * {@code ck_relecture_note} ; l'horodatage du rendu est décidé par le serveur.
     */
    public void rendre(int note, String commentaire) {
        this.note = note;
        this.commentaire = commentaire;
        this.statut = StatutRelecture.RENDUE;
        this.renduAt = LocalDateTime.now();
    }

    /**
     * EF7 (RG8) : le relecteur remplace sa note et son commentaire.
     *
     * <p>Le statut reste {@code RENDUE} et {@code renduAt} n'est pas réécrit : une
     * correction ne rejoue pas le rendu, elle en remplace le contenu. Ni l'un ni
     * l'autre ne sont donc des paramètres — c'est {@code correction_relecture} qui
     * date le changement, et la note remplacée y est archivée avant l'appel.
     */
    public void corriger(int note, String commentaire) {
        this.note = note;
        this.commentaire = commentaire;
    }

    public Long getId() {
        return id;
    }

    public Long getExerciceId() {
        return exerciceId;
    }

    public Long getRelecteurId() {
        return relecteurId;
    }

    public Long getAuteurId() {
        return auteurId;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public StatutRelecture getStatut() {
        return statut;
    }

    public LocalDateTime getRenduAt() {
        return renduAt;
    }
}
