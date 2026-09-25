package com.kfokam48.app.features.presence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.presence.application.dto.AjoutPresenceManuelleRequete;
import com.kfokam48.app.features.presence.application.dto.MarquagePresenceRequete;
import com.kfokam48.app.features.presence.application.dto.PresenceReponse;
import com.kfokam48.app.features.presence.domain.entity.Presence;
import com.kfokam48.app.features.presence.domain.entity.SourcePresence;
import com.kfokam48.app.features.presence.domain.entity.TentativeSaisie;
import com.kfokam48.app.features.presence.domain.repository.PresenceRepository;
import com.kfokam48.app.features.presence.domain.repository.TentativeSaisieRepository;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitaire (B6) des deux règles métier de l'issue #9 :
 * RG1 (expiration du code) et RG3 (compteur d'échecs et blocage).
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    private static final Long SESSION_ID = 42L;
    private static final Long ETUDIANT_ID = 1L;
    private static final String CODE = "ABC234";
    private static final int MAX_ECHECS = 5;
    private static final int BLOCAGE_MINUTES = 2;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private TentativeSaisieRepository tentativeSaisieRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    private PresenceService presenceService;

    @BeforeEach
    void initialiserLeService() {
        presenceService = new PresenceService(sessionRepository, presenceRepository,
                tentativeSaisieRepository, etudiantRepository, MAX_ECHECS, BLOCAGE_MINUTES);
    }

    @Test
    @DisplayName("Un code valide et non expiré enregistre la présence avec source = ETUDIANT")
    void code_valide_enregistre_la_presence() {
        preparerSession(session(false));
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.empty());
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Le code est saisi en minuscules : il doit être normalisé.
        PresenceReponse reponse = presenceService.marquerPresence(
                new MarquagePresenceRequete("abc234", ETUDIANT_ID));

        assertThat(reponse.sessionId()).isEqualTo(SESSION_ID);
        assertThat(reponse.etudiantId()).isEqualTo(ETUDIANT_ID);
        assertThat(reponse.source()).isEqualTo(SourcePresence.ETUDIANT);
        verify(presenceRepository).save(any(Presence.class));
    }

    @Test
    @DisplayName("RG1 : un code expiré est refusé en 410 CODE_EXPIRE et compte un échec")
    void rg1_code_expire_refuse() {
        preparerSession(session(true));
        TentativeSaisie tentative = new TentativeSaisie(SESSION_ID, ETUDIANT_ID);
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.of(tentative));

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.CODE_EXPIRE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.GONE);
                });

        assertThat(tentative.getEchecs()).isEqualTo(1);
        verify(tentativeSaisieRepository).save(tentative);
        verify(presenceRepository, never()).save(any(Presence.class));
    }

    @Test
    @DisplayName("RG3 : le 5e échec bloque le couple (étudiant, session) pendant 2 minutes")
    void rg3_cinquieme_echec_bloque_deux_minutes() {
        preparerSession(session(true));
        TentativeSaisie tentative = new TentativeSaisie(SESSION_ID, ETUDIANT_ID);
        tentative.setEchecs(MAX_ECHECS - 1);
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.of(tentative));

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class);

        assertThat(tentative.getEchecs()).isEqualTo(MAX_ECHECS);
        assertThat(tentative.getBloqueJusqua()).isNotNull();
        assertThat(Duration.between(LocalDateTime.now(), tentative.getBloqueJusqua()))
                .isBetween(Duration.ofMinutes(BLOCAGE_MINUTES - 1), Duration.ofMinutes(BLOCAGE_MINUTES + 1));
    }

    @Test
    @DisplayName("RG3 : un blocage actif est refusé en 429 avant même la validation du code")
    void rg3_blocage_actif_refuse_en_429() {
        // Le code est expiré : le 429 doit primer sur le 410 (D3, ordre des contrôles).
        preparerSession(session(true));
        TentativeSaisie tentative = new TentativeSaisie(SESSION_ID, ETUDIANT_ID);
        tentative.setEchecs(MAX_ECHECS);
        tentative.setBloqueJusqua(LocalDateTime.now().plusMinutes(1));
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.of(tentative));

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.TROP_DE_TENTATIVES);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });

        verify(tentativeSaisieRepository, never()).save(any(TentativeSaisie.class));
    }

    @Test
    @DisplayName("RG3 : une fois les 2 minutes écoulées, le couple retrouve ses tentatives")
    void rg3_blocage_termine_reinitialise_le_compteur() {
        preparerSession(session(false));
        TentativeSaisie tentative = new TentativeSaisie(SESSION_ID, ETUDIANT_ID);
        tentative.setEchecs(MAX_ECHECS);
        tentative.setBloqueJusqua(LocalDateTime.now().minusMinutes(1));
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.of(tentative));
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PresenceReponse reponse = presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID));

        assertThat(tentative.getEchecs()).isZero();
        assertThat(tentative.getBloqueJusqua()).isNull();
        assertThat(reponse.source()).isEqualTo(SourcePresence.ETUDIANT);
    }

    @Test
    @DisplayName("RG3 : le compteur est cloisonné par session")
    void rg3_compteur_cloisonne_par_session() {
        preparerSession(session(false));
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.empty());
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(presenceRepository.save(any(Presence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID));

        // Le compteur consulté est celui du couple (session courante, étudiant) :
        // un blocage sur une autre session n'a donc aucun effet ici.
        verify(tentativeSaisieRepository).findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID);
    }

    @Test
    @DisplayName("RG3 : cinq codes inconnus bloquent 2 minutes, le 6e est refusé en 429")
    void rg3_codes_inconnus_bloquent_apres_cinq_echecs() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findByCode("ZZZZZZ")).thenReturn(Optional.empty());
        TentativeSaisie compteurDeLEtudiant = TentativeSaisie.sansSession(ETUDIANT_ID);
        when(tentativeSaisieRepository.findBySessionIdIsNullAndEtudiantId(ETUDIANT_ID))
                .thenReturn(Optional.of(compteurDeLEtudiant));

        for (int tentative = 1; tentative <= MAX_ECHECS; tentative++) {
            assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete("ZZZZZZ", ETUDIANT_ID)))
                    .isInstanceOf(ExceptionMetier.class)
                    .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                            .isEqualTo(CodeErreur.CODE_INCONNU));
        }

        assertThat(compteurDeLEtudiant.getEchecs()).isEqualTo(MAX_ECHECS);
        assertThat(compteurDeLEtudiant.getBloqueJusqua()).isNotNull();

        // La session reste inconnue : le blocage ne vient que du compteur par étudiant.
        verify(tentativeSaisieRepository, never()).findBySessionIdAndEtudiantId(any(), any());

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete("ZZZZZZ", ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.TROP_DE_TENTATIVES);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    @DisplayName("Un code inconnu est refusé en 400 CODE_INCONNU")
    void code_inconnu_refuse() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findByCode("ZZZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete("ZZZZZZ", ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.CODE_INCONNU);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Une présence déjà enregistrée est refusée en 409 DEJA_PRESENT")
    void deja_present_refuse() {
        preparerSession(session(false));
        when(tentativeSaisieRepository.findBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID))
                .thenReturn(Optional.empty());
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.DEJA_PRESENT));

        verify(presenceRepository, never()).save(any(Presence.class));
    }

    @Test
    @DisplayName("Un étudiant inconnu est refusé en 400, sans interroger la base sur le code")
    void etudiant_inconnu_refuse() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> presenceService.marquerPresence(new MarquagePresenceRequete(CODE, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.ETUDIANT_INCONNU));

        verify(sessionRepository, never()).findByCode(any());
    }

    // ---------- EF10 : ajout manuel d'une présence par le formateur (RG12) ----------

    @Test
    @DisplayName("EF10/RG12 : l'ajout manuel enregistre une présence de source FORMATEUR")
    void ajout_manuel_enregistre_une_presence_formateur() {
        preparerAjoutManuel();
        when(presenceRepository.saveAndFlush(any(Presence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PresenceReponse reponse = presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID));

        assertThat(reponse.sessionId()).isEqualTo(SESSION_ID);
        assertThat(reponse.etudiantId()).isEqualTo(ETUDIANT_ID);
        // La source est décidée par le service : le client ne peut pas la choisir.
        assertThat(reponse.source()).isEqualTo(SourcePresence.FORMATEUR);
    }

    @Test
    @DisplayName("EF10 : l'ajout manuel fonctionne même avec un code expiré, et sans code à fournir")
    void ajout_manuel_ignore_l_expiration_du_code() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(true)));
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(presenceRepository.saveAndFlush(any(Presence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID)).source())
                .isEqualTo(SourcePresence.FORMATEUR);

        // RG1 n'est jamais consulté par ce chemin : ni code, ni expiration.
        verify(sessionRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("EF10 : une session inconnue est refusée en 404 SESSION_INCONNUE")
    void ajout_manuel_session_inconnue() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.SESSION_INCONNUE);
                    // 404 sur cette opération, là où le dépôt d'exercice répond 400.
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(presenceRepository, never()).saveAndFlush(any(Presence.class));
    }

    @Test
    @DisplayName("EF10 : un étudiant inconnu est refusé en 404 ETUDIANT_INCONNU")
    void ajout_manuel_etudiant_inconnu() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(false)));
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.ETUDIANT_INCONNU);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(presenceRepository, never()).saveAndFlush(any(Presence.class));
    }

    @Test
    @DisplayName("EF10 : un doublon est refusé en 409 DEJA_PRESENT, sans rien écrire")
    void ajout_manuel_doublon_refuse() {
        preparerAjoutManuel();
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.DEJA_PRESENT));

        verify(presenceRepository, never()).saveAndFlush(any(Presence.class));
    }

    @Test
    @DisplayName("EF10 : deux clics simultanés donnent un 409 par la contrainte d'unicité, jamais un 500")
    void ajout_manuel_course_entre_deux_clics() {
        preparerAjoutManuel();
        // Le contrôle applicatif a laissé passer les deux requêtes : c'est
        // uk_presence_session_etudiant qui tranche, au flush.
        when(presenceRepository.saveAndFlush(any(Presence.class)))
                .thenThrow(new DataIntegrityViolationException("uk_presence_session_etudiant"));

        assertThatThrownBy(() -> presenceService.ajouterPresenceManuelle(
                new AjoutPresenceManuelleRequete(SESSION_ID, ETUDIANT_ID)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.DEJA_PRESENT);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    /** Contexte nominal de l'ajout manuel : session et étudiant existants, pas de doublon. */
    private void preparerAjoutManuel() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(false)));
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(presenceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
    }

    private void preparerSession(Session session) {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findByCode(CODE)).thenReturn(Optional.of(session));
    }

    /** Session du code {@code ABC234} ; {@code expiree} la place 5 minutes dans le passé. */
    private Session session(boolean expiree) {
        LocalDateTime ouverture = LocalDateTime.now().minusMinutes(expiree ? 20 : 1);
        LocalDateTime expiration = expiree ? LocalDateTime.now().minusMinutes(5) : LocalDateTime.now().plusMinutes(14);
        Session session = new Session("Cours de test", CODE, 1L, ouverture, expiration);
        // L'identifiant est normalement généré par la base ; en test unitaire, on le fixe.
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        return session;
    }
}
