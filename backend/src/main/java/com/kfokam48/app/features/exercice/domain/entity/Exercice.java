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
