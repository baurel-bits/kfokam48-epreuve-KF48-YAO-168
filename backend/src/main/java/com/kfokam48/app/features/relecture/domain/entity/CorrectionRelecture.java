package com.kfokam48.app.features.relecture.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Une correction de note archivée (EF7, RG8).
 *
 * <p>La note <strong>courante</strong> reste sur {@link Relecture} : cette entité
 * ne conserve que ce qui a été remplacé, si bien qu'une correction n'efface
 * jamais la précédente. {@code ancienneNote} est non nulle en base, ce qui
 * interdit d'« historiser » une correction d'une relecture jamais rendue — la
 * raison pour laquelle le service exige le statut {@code RENDUE}.
 *
 * <p>La ligne est écrite par le serveur seul : ni son horodatage ni ses valeurs
 * ne viennent du client.
 */
@Entity
@Table(name = "correction_relecture")
public class CorrectionRelecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "relecture_id", nullable = false)
    private Long relectureId;

    /** Note remplacée par la correction (RG7 : même domaine 0–20 que la note). */
    @Column(name = "ancienne_note", nullable = false)
    private Integer ancienneNote;

    @Column(name = "ancien_commentaire", length = 2000)
    private String ancienCommentaire;

    @Column(name = "corrige_at", nullable = false)
    private LocalDateTime corrigeAt;

    protected CorrectionRelecture() {
        // requis par JPA
    }

    public CorrectionRelecture(Long relectureId, Integer ancienneNote, String ancienCommentaire,
                               LocalDateTime corrigeAt) {
        this.relectureId = relectureId;
        this.ancienneNote = ancienneNote;
        this.ancienCommentaire = ancienCommentaire;
        this.corrigeAt = corrigeAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRelectureId() {
        return relectureId;
    }

    public Integer getAncienneNote() {
        return ancienneNote;
    }

    public String getAncienCommentaire() {
        return ancienCommentaire;
    }

    public LocalDateTime getCorrigeAt() {
        return corrigeAt;
    }
}
