package com.kfokam48.app.features.exercice.domain.entity;

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
 * Exercice déposé par un étudiant (EF3, EF4, EF5 ; diagrammes D2 et D4).
 * L'unicité du couple (session, étudiant) est garantie par
 * {@code uk_exercice_session_etudiant} : c'est elle qui matérialise la règle
 * {@code 409 EXERCICE_DEJA_DEPOSE}.
 */
@Entity
@Table(name = "exercice")
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(name = "lien", nullable = false, length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutExercice statut;

    @Column(name = "depot_at", nullable = false)
    private LocalDateTime depotAt;

    protected Exercice() {
        // requis par JPA
    }

    public Exercice(Long sessionId, Long etudiantId, String lien, StatutExercice statut, LocalDateTime depotAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.lien = lien;
        this.statut = statut;
        this.depotAt = depotAt;
    }

    /**
     * RG5 : l'assignation d'un relecteur fait passer l'exercice de {@code DEPOSE}
     * à {@code EN_ATTENTE_RELECTURE} (D4). Sans relecteur éligible, il reste
     * {@code DEPOSE} : aucun relecteur ne lui est attaché.
     */
    public void attribuerRelecteur() {
        this.statut = StatutExercice.EN_ATTENTE_RELECTURE;
    }

    /**
     * EF4 (RG11) : le lien est remplacé par celui-ci. Le <strong>statut ne change
     * pas</strong> : la seule situation où RG11 autorise un remplacement est celle
     * d'un exercice resté {@code DEPOSE}, faute de relecteur éligible au dépôt.
     *
     * <p>Le lien reçu a déjà été validé par le service (format {@code http(s)},
     * 500 caractères) et l'est de nouveau par la colonne {@code lien}.
     */
    public void remplacerLien(String lien) {
        this.lien = lien;
    }

    /**
     * EF6/D4 : une note rendue fait passer l'exercice de
     * {@code EN_ATTENTE_RELECTURE} à {@code RELU}. Cette transition n'est
     * atteignable que si une relecture existe, ce que garantit l'assignation
     * effectuée au dépôt (EF5).
     */
    public void marquerRelu() {
        this.statut = StatutExercice.RELU;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public String getLien() {
        return lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public LocalDateTime getDepotAt() {
        return depotAt;
    }
}
