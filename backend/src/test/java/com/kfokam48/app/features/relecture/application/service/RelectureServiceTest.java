package com.kfokam48.app.features.relecture.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.entity.StatutExercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.presence.domain.entity.Presence;
import com.kfokam48.app.features.presence.domain.entity.SourcePresence;
import com.kfokam48.app.features.presence.domain.repository.PresenceRepository;
import com.kfokam48.app.features.relecture.application.dto.MissionRelecteurReponse;
import com.kfokam48.app.features.relecture.domain.entity.Relecture;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import com.kfokam48.app.features.relecture.domain.repository.RelectureRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitaire (B6) de la règle métier de l'issue #11 : RG5/RG13 (tirage d'un
 * relecteur présent à l'instant de l'assignation) et RG4 (l'auteur est exclu).
 */
@ExtendWith(MockitoExtension.class)
class RelectureServiceTest {

    private static final Long EXERCICE_ID = 11L;
    private static final Long SESSION_ID = 3L;
    private static final Long AUTEUR_ID = 1L;
    private static final Long RELECTEUR_ID = 2L;
    private static final Long AUTRE_PRESENT = 4L;
    private static final String LIEN = "https://exemple.org/exercice.pdf";

    @Mock
    private RelectureRepository relectureRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private ExerciceRepository exerciceRepository;

    private RelectureService relectureService;

    @BeforeEach
    void initialiserLeService() {
        relectureService = new RelectureService(relectureRepository, presenceRepository, exerciceRepository);
    }

    @Test
    @DisplayName("RG5/RG13 : le relecteur est tiré parmi les étudiants présents à la session")
    void relecteur_choisi_parmi_les_presents() {
        // L'auteur lui-même est présent : il doit malgré tout être écarté (RG4).
        when(presenceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(
                presence(AUTEUR_ID), presence(2L), presence(3L)));

        Long relecteurId = relectureService
                .assignerUnRelecteur(EXERCICE_ID, SESSION_ID, AUTEUR_ID)
                .orElseThrow();

        // Le relecteur est nécessairement l'un des présents, et jamais l'auteur (RG4).
        assertThat(relecteurId).isIn(2L, 3L);
        assertThat(relecteurId).isNotEqualTo(AUTEUR_ID);

        Relecture relecture = relectureEnregistree();
        assertThat(relecture.getRelecteurId()).isEqualTo(relecteurId);
        assertThat(relecture.getAuteurId()).isEqualTo(AUTEUR_ID);
        assertThat(relecture.getExerciceId()).isEqualTo(EXERCICE_ID);
        assertThat(relecture.getStatut()).isEqualTo(StatutRelecture.EN_ATTENTE);
        assertThat(relecture.getNote()).isNull();
    }

    @Test
    @DisplayName("RG13 : le pool est lu à l'instant de l'assignation, présences manuelles incluses")
    void presence_manuelle_eligible() {
        // Presence ajoutée par le formateur (EF10, RG12) : elle doit compter comme les autres.
        when(presenceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(
                presence(AUTEUR_ID), presence(AUTRE_PRESENT, SourcePresence.FORMATEUR)));

        Long relecteurId = relectureService
                .assignerUnRelecteur(EXERCICE_ID, SESSION_ID, AUTEUR_ID)
                .orElseThrow();

        assertThat(relecteurId).isEqualTo(AUTRE_PRESENT);
        assertThat(relectureEnregistree().getRelecteurId()).isEqualTo(AUTRE_PRESENT);
        // Le pool n'est jamais figé : il est relu à chaque appel.
        verify(presenceRepository).findBySessionId(SESSION_ID);
    }

    @Test
    @DisplayName("RG4 : l'auteur ne peut jamais être désigné relecteur de son propre exercice")
    void auteur_exclu_du_pool() {
        when(presenceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(presence(AUTEUR_ID)));

        assertThat(relectureService.assignerUnRelecteur(EXERCICE_ID, SESSION_ID, AUTEUR_ID)).isEmpty();
        verify(relectureRepository, never()).save(any(Relecture.class));
    }

    @Test
    @DisplayName("EF5 : sans étudiant éligible, aucune relecture n'est créée")
    void aucun_etudiant_eligible() {
        when(presenceRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());

        assertThat(relectureService.assignerUnRelecteur(EXERCICE_ID, SESSION_ID, AUTEUR_ID)).isEmpty();
        verify(relectureRepository, never()).save(any(Relecture.class));
    }

    @Test
    @DisplayName("RG6 : le relecteur assigné consulte la mission de son exercice")
    void relecteur_assigne_consulte_sa_mission() {
        when(relectureRepository.findByExerciceId(EXERCICE_ID)).thenReturn(Optional.of(relectureAssignee()));
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice()));

        MissionRelecteurReponse mission = relectureService.consulterMission(EXERCICE_ID, RELECTEUR_ID);

        assertThat(mission.relectureId()).isEqualTo(EXERCICE_ID + 100);
        assertThat(mission.exerciceId()).isEqualTo(EXERCICE_ID);
        assertThat(mission.sessionId()).isEqualTo(SESSION_ID);
        assertThat(mission.lien()).isEqualTo(LIEN);
        assertThat(mission.statut()).isEqualTo(StatutRelecture.EN_ATTENTE);
    }

    @Test
    @DisplayName("RG6 : tout autre appelant est refusé en 403 APPELANT_NON_AUTORISE")
    void autre_appelant_refuse() {
        when(relectureRepository.findByExerciceId(EXERCICE_ID)).thenReturn(Optional.of(relectureAssignee()));

        assertThatThrownBy(() -> relectureService.consulterMission(EXERCICE_ID, 999L))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.APPELANT_NON_AUTORISE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(exerciceRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Sans relecture assignée, la mission est introuvable en 404 RELECTURE_INCONNUE")
    void relecture_absente_renvoie_404() {
        when(relectureRepository.findByExerciceId(EXERCICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> relectureService.consulterMission(EXERCICE_ID, RELECTEUR_ID))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.RELECTURE_INCONNUE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    /** RG5 : la relecture réellement confiée est celle qui a été écrite en base. */
    private Relecture relectureEnregistree() {
        ArgumentCaptor<Relecture> capture = ArgumentCaptor.forClass(Relecture.class);
        verify(relectureRepository).save(capture.capture());
        return capture.getValue();
    }

    private Presence presence(Long etudiantId) {
        return presence(etudiantId, SourcePresence.ETUDIANT);
    }

    private Presence presence(Long etudiantId, SourcePresence source) {
        return new Presence(SESSION_ID, etudiantId, source, LocalDateTime.now());
    }

    private Relecture relectureAssignee() {
        Relecture relecture = new Relecture(EXERCICE_ID, RELECTEUR_ID, AUTEUR_ID);
        ReflectionTestUtils.setField(relecture, "id", EXERCICE_ID + 100);
        return relecture;
    }

    private Exercice exercice() {
        Exercice exercice = new Exercice(SESSION_ID, AUTEUR_ID, LIEN, StatutExercice.DEPOSE, LocalDateTime.now());
        ReflectionTestUtils.setField(exercice, "id", EXERCICE_ID);
        return exercice;
    }
}
