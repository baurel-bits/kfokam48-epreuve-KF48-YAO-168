package com.kfokam48.app.features.presence.domain.entity;

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
 * Présence d'un étudiant à une session (EF2, EF10 ; diagramme D2).
 * L'unicité du couple (session, étudiant) est garantie par
 * {@code uk_presence_session_etudiant} : c'est elle qui matérialise
 * la règle {@code 409 DEJA_PRESENT}.
 */
@Entity
@Table(name = "presence")
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SourcePresence source;

    @Column(name = "date_presence", nullable = false)
    private LocalDateTime datePresence;

    protected Presence() {
        // requis par JPA
    }

    public Presence(Long sessionId, Long etudiantId, SourcePresence source, LocalDateTime datePresence) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.source = source;
        this.datePresence = datePresence;
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

    public SourcePresence getSource() {
        return source;
    }

    public LocalDateTime getDatePresence() {
        return datePresence;
    }
}
