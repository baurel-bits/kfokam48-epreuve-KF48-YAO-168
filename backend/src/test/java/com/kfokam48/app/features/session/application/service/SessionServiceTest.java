package com.kfokam48.app.features.session.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.promotion.domain.repository.PromotionRepository;
import com.kfokam48.app.features.session.application.dto.CreationSessionRequete;
import com.kfokam48.app.features.session.application.dto.SessionClotureeReponse;
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitaire (B6) sur les règles métier des issues #8 (EF1 : RG1, expiration
 * du code à ouverture + 15 minutes) et #15 (EF11 : clôture, RG14).
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final int VALIDITE_CODE_MINUTES = 15;
    private static final long PROMOTION_EXISTANTE = 1L;
    private static final Long SESSION_ID = 7L;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PromotionRepository promotionRepository;

    private SessionService sessionService;

    @BeforeEach
    void initialiserLeService() {
        sessionService = new SessionService(sessionRepository, promotionRepository, VALIDITE_CODE_MINUTES);
    }

    @Test
    @DisplayName("RG1 : le code expire 15 minutes après l'ouverture de la session")
    void rg1_expiration_quinze_minutes_apres_ouverture() {
        promotionExistante();

        SessionOuverteReponse reponse = sessionService.ouvrir(
                new CreationSessionRequete("Cours du 25 septembre", PROMOTION_EXISTANTE));

        assertThat(reponse.ouvertureAt()).isNotNull();
        assertThat(reponse.expirationAt()).isEqualTo(reponse.ouvertureAt().plusMinutes(VALIDITE_CODE_MINUTES));
        assertThat(Duration.between(reponse.ouvertureAt(), reponse.expirationAt()))
                .isEqualTo(Duration.ofMinutes(VALIDITE_CODE_MINUTES));
    }

    @Test
    @DisplayName("Le code de présence fait 6 caractères non ambigus et est unique")
    void code_de_presence_au_format_attendu() {
        promotionExistante();

        SessionOuverteReponse reponse = sessionService.ouvrir(
                new CreationSessionRequete("Cours du 25 septembre", PROMOTION_EXISTANTE));

        assertThat(reponse.code()).hasSize(6).matches("[A-HJ-NP-Z2-9]{6}");
        verify(sessionRepository).existsByCode(reponse.code());
    }

    @Test
    @DisplayName("Un code déjà attribué est régénéré (contrainte uk_session_code)")
    void code_deja_attribue_regenere() {
        when(promotionRepository.existsById(PROMOTION_EXISTANTE)).thenReturn(true);
        when(sessionRepository.existsByCode(anyString())).thenReturn(true, false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionOuverteReponse reponse = sessionService.ouvrir(
                new CreationSessionRequete("Cours du 25 septembre", PROMOTION_EXISTANTE));

        assertThat(reponse.code()).isNotBlank();
        verify(sessionRepository, times(2)).existsByCode(anyString());
    }

    @Test
    @DisplayName("Une promotion inconnue est refusée en 400 PROMOTION_INCONNUE, sans créer de session")
    void promotion_inconnue_refusee() {
        when(promotionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.ouvrir(new CreationSessionRequete("Cours", 999L)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.PROMOTION_INCONNUE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Aucune session n'est ouverte si aucun code unique n'est trouvé")
    void echec_generation_code_unique() {
        when(promotionRepository.existsById(PROMOTION_EXISTANTE)).thenReturn(true);
        when(sessionRepository.existsByCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> sessionService.ouvrir(
                new CreationSessionRequete("Cours", PROMOTION_EXISTANTE)))
                .isInstanceOf(ExceptionMetier.class);

        verify(sessionRepository, never()).save(any(Session.class));
    }

    // ---------- EF11 : clôture d'une session (RG14) ----------

    @Test
    @DisplayName("EF11/RG14 : la clôture est persistée et renvoyée à l'appelant")
    void cloture_une_session() {
        Session session = session(false);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionClotureeReponse reponse = sessionService.cloturer(SESSION_ID);

        assertThat(reponse.id()).isEqualTo(SESSION_ID);
        assertThat(reponse.cloturee()).isTrue();
        assertThat(session.isCloturee()).isTrue();
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("EF11 : une session inconnue est refusée en 404 SESSION_INCONNUE")
    void cloture_une_session_inconnue() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.cloturer(999L))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.SESSION_INCONNUE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("EF11 : reclôturer une session clôturée répond 200 sans réécriture (le contrat ne déclare que 200 et 404)")
    void recloture_idempotente() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(true)));

        SessionClotureeReponse reponse = sessionService.cloturer(SESSION_ID);

        assertThat(reponse.id()).isEqualTo(SESSION_ID);
        assertThat(reponse.cloturee()).isTrue();
        verify(sessionRepository, never()).save(any(Session.class));
    }

    private void promotionExistante() {
        when(promotionRepository.existsById(PROMOTION_EXISTANTE)).thenReturn(true);
        when(sessionRepository.existsByCode(anyString())).thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Session persistée, avec son identifiant : c'est elle que `findById` restitue. */
    private Session session(boolean cloturee) {
        Session session = new Session("Cours du 25 septembre", "ABC234", PROMOTION_EXISTANTE,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(VALIDITE_CODE_MINUTES));
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        ReflectionTestUtils.setField(session, "cloturee", cloturee);
        return session;
    }
}
