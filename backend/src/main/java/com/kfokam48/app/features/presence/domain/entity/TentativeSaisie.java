package com.kfokam48.app.features.presence.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Compteur d'échecs de saisie de code (RG3, diagramme D2).
 *
 * <p>Deux portées :
 * <ul>
 *   <li><strong>par couple (étudiant, session)</strong> lorsque le code est
 *       attribuable à une session (code expiré) : être bloqué sur une session
 *       n'empêche pas de saisir un code pour une autre ;</li>
 *   <li><strong>par étudiant</strong> ({@code session_id IS NULL}) lorsque le code
 *       est inconnu et ne peut être rattaché à aucune session (V3).</li>
 * </ul>
 */
@Entity
@Table(name = "tentative_saisie")
public class TentativeSaisie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(name = "echecs", nullable = false)
    private int echecs;

    @Column(name = "bloque_jusqua")
    private LocalDateTime bloqueJusqua;

    protected TentativeSaisie() {
        // requis par JPA
    }

    public TentativeSaisie(Long sessionId, Long etudiantId) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.echecs = 0;
    }

    /**
     * Compteur des codes <strong>non attribuables à une session</strong> (code inconnu).
     * Le contrat n'envoyant que {@code { code, etudiantId }}, la session est
     * indéterminable : la ligne porte donc {@code session_id = NULL}.
     */
    public static TentativeSaisie sansSession(Long etudiantId) {
        return new TentativeSaisie(null, etudiantId);
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

    public int getEchecs() {
        return echecs;
    }

    public void setEchecs(int echecs) {
        this.echecs = echecs;
    }

    public LocalDateTime getBloqueJusqua() {
        return bloqueJusqua;
    }

    public void setBloqueJusqua(LocalDateTime bloqueJusqua) {
        this.bloqueJusqua = bloqueJusqua;
    }
}
