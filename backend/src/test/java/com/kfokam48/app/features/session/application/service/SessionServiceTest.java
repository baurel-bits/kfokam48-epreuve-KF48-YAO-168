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
import com.kfokam48.app.features.session.application.dto.SessionOuverteReponse;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Test unitaire (B6) sur la règle métier la plus significative de l'issue #8 :
 * RG1, l'expiration du code de présence à ouverture + 15 minutes.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final int VALIDITE_CODE_MINUTES = 15;
    private static final long PROMOTION_EXISTANTE = 1L;

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

    private void promotionExistante() {
        when(promotionRepository.existsById(PROMOTION_EXISTANTE)).thenReturn(true);
        when(sessionRepository.existsByCode(anyString())).thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
