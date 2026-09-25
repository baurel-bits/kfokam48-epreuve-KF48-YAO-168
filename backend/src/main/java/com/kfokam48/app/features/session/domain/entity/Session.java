package com.kfokam48.app.features.session.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Session de présence ouverte par le formateur (EF1 ; diagramme D2).
 * Le code est unique (contrainte {@code uk_session_code}) et expire
 * {@code app.session.code-validity-minutes} minutes après l'ouverture (RG1).
 */
@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false, length = 150)
    private String titre;

    @Column(name = "code", nullable = false, length = 12, unique = true)
    private String code;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "ouverture_at", nullable = false)
    private LocalDateTime ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private LocalDateTime expirationAt;

    @Column(name = "cloturee", nullable = false)
    private boolean cloturee;

    protected Session() {
        // requis par JPA
    }

    public Session(String titre, String code, Long promotionId, LocalDateTime ouvertureAt, LocalDateTime expirationAt) {
        this.titre = titre;
        this.code = code;
        this.promotionId = promotionId;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
        this.cloturee = false;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getCode() {
        return code;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public LocalDateTime getOuvertureAt() {
        return ouvertureAt;
    }

    public LocalDateTime getExpirationAt() {
        return expirationAt;
    }

    public boolean isCloturee() {
        return cloturee;
    }
}
