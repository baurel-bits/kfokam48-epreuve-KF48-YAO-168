package com.kfokam48.app.features.exercice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.exercice.application.dto.DepotExerciceRequete;
import com.kfokam48.app.features.exercice.application.dto.ExerciceDeposeReponse;
import com.kfokam48.app.features.exercice.application.dto.RemplacementLienRequete;
import com.kfokam48.app.features.exercice.domain.entity.Exercice;
import com.kfokam48.app.features.exercice.domain.entity.StatutExercice;
import com.kfokam48.app.features.exercice.domain.repository.ExerciceRepository;
import com.kfokam48.app.features.promotion.domain.repository.EtudiantRepository;
import com.kfokam48.app.features.relecture.application.service.AssignateurRelecteur;
import com.kfokam48.app.features.relecture.domain.repository.RelectureRepository;
import com.kfokam48.app.features.session.domain.entity.Session;
import com.kfokam48.app.features.session.domain.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test unitaire (B6) des règles métier des issues #10 (RG10 : un exercice peut
 * être déposé jusqu'à la clôture de la session, même après l'expiration du code)
 * et #16 (RG11 : le lien est remplaçable tant qu'aucune relecture n'a été
 * commencée).
 */
@ExtendWith(MockitoExtension.class)
class ExerciceServiceTest {

    private static final Long SESSION_ID = 7L;
    private static final Long ETUDIANT_ID = 1L;
    private static final Long EXERCICE_ID = 42L;
    private static final String LIEN_VALIDE = "https://exemple.org/exercice-1.pdf";
    private static final String LIEN_REMPLACEMENT = "https://exemple.org/exercice-1-corrige.pdf";

    @Mock
    private ExerciceRepository exerciceRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private AssignateurRelecteur assignateurRelecteur;

    @Mock
    private RelectureRepository relectureRepository;

    private ExerciceService exerciceService;

    @BeforeEach
    void initialiserLeService() {
        exerciceService = new ExerciceService(exerciceRepository, sessionRepository, etudiantRepository,
                assignateurRelecteur, relectureRepository);
    }

    @Test
    @DisplayName("Un lien valide crée l'exercice au statut DEPOSE (D4)")
    void depose_un_exercice_au_statut_depose() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(false)));
        when(exerciceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Aucun étudiant présent : aucun relecteur ne peut être assigné (EF5).
        when(assignateurRelecteur.assignerUnRelecteur(any(), any(), any())).thenReturn(Optional.empty());

        ExerciceDeposeReponse reponse = exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.DEPOSE);
        verify(exerciceRepository).save(any(Exercice.class));
    }

    @Test
    @DisplayName("EF5 : quand un relecteur est assigné, l'exercice passe en EN_ATTENTE_RELECTURE (D4)")
    void depot_avec_relecteur_assigne_passe_en_attente_de_relecture() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(false)));
        when(exerciceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignateurRelecteur.assignerUnRelecteur(any(), eq(SESSION_ID), eq(ETUDIANT_ID)))
                .thenReturn(Optional.of(2L));

        ExerciceDeposeReponse reponse = exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    @DisplayName("RG10 : le dépôt reste possible après l'expiration du code de présence")
    void rg10_depot_possible_apres_expiration_du_code() {
        // Le code a expiré il y a 15 minutes : aucun contrôle d'expiration ne doit
        // s'appliquer au dépôt, seul l'état de clôture compte.
        Session sessionExpiree = session(false);
        ReflectionTestUtils.setField(sessionExpiree, "expirationAt", LocalDateTime.now().minusMinutes(15));

        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(sessionExpiree));
        when(exerciceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(false);
        when(exerciceRepository.save(any(Exercice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignateurRelecteur.assignerUnRelecteur(any(), any(), any())).thenReturn(Optional.empty());

        ExerciceDeposeReponse reponse = exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE));

        assertThat(reponse.statut()).isEqualTo(StatutExercice.DEPOSE);
    }

    @Test
    @DisplayName("RG10/RG14 : une session clôturée refuse tout dépôt en 409 SESSION_CLOTUREE")
    void session_cloturee_refuse_le_depot() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(true)));

        assertThatThrownBy(() -> exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.SESSION_CLOTUREE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(exerciceRepository, never()).save(any(Exercice.class));
    }

    @Test
    @DisplayName("Un second dépôt sur la même session est refusé en 409 EXERCICE_DEJA_DEPOSE")
    void second_depot_refuse() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session(false)));
        when(exerciceRepository.existsBySessionIdAndEtudiantId(SESSION_ID, ETUDIANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.EXERCICE_DEJA_DEPOSE));

        verify(exerciceRepository, never()).save(any(Exercice.class));
    }

    @ParameterizedTest(name = "lien refusé : [{0}]")
    @ValueSource(strings = {"", "   ", "pas-une-url", "exemple.org/x.pdf", "ftp://exemple.org/x.pdf", "http://"})
    @DisplayName("Un lien mal formé est refusé en 400 LIEN_INVALIDE, sans atteindre la base")
    void lien_invalide_refuse(String lienInvalide) {
        assertThatThrownBy(() -> exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, lienInvalide)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.LIEN_INVALIDE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(exerciceRepository, never()).save(any(Exercice.class));
    }

    @Test
    @DisplayName("Une session inconnue est refusée en 400")
    void session_inconnue_refusee() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(true);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.SESSION_INCONNUE));
    }

    @Test
    @DisplayName("Un étudiant inconnu est refusé en 400, sans interroger la session")
    void etudiant_inconnu_refuse() {
        when(etudiantRepository.existsById(ETUDIANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> exerciceService.deposer(
                new DepotExerciceRequete(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.ETUDIANT_INCONNU));

        verify(sessionRepository, never()).findById(any());
    }

    // ---------- EF4 (#16) : remplacer le lien tant qu'aucune relecture n'a commencé ----------

    @Test
    @DisplayName("RG11 (EF4) : sans relecture commencée, le lien est remplacé et le statut ne change pas")
    void remplace_le_lien_sans_relecture() {
        Exercice exercice = exercice(StatutExercice.DEPOSE);
        unExercice(exercice, session(false));
        when(relectureRepository.existsByExerciceId(EXERCICE_ID)).thenReturn(false);

        ExerciceDeposeReponse reponse = exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(LIEN_REMPLACEMENT));

        assertThat(reponse.id()).isEqualTo(EXERCICE_ID);
        // D4 : remplacer un lien ne franchit aucune transition d'état.
        assertThat(reponse.statut()).isEqualTo(StatutExercice.DEPOSE);
        assertThat(exercice.getLien()).isEqualTo(LIEN_REMPLACEMENT);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.DEPOSE);
    }

    @Test
    @DisplayName("RG11 (EF4) : une relecture commencée refuse le remplacement en 409 RELECTURE_COMMENCEE")
    void relecture_commencee_refuse_le_remplacement() {
        // Un relecteur a été tiré au dépôt (EF5) : la relecture est commencée, même
        // si aucune note n'a encore été rendue.
        Exercice exercice = exercice(StatutExercice.EN_ATTENTE_RELECTURE);
        unExercice(exercice, session(false));
        when(relectureRepository.existsByExerciceId(EXERCICE_ID)).thenReturn(true);

        assertThatThrownBy(() -> exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(LIEN_REMPLACEMENT)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.RELECTURE_COMMENCEE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.CONFLICT);
                });

        assertThat(exercice.getLien()).isEqualTo(LIEN_VALIDE);
    }

    @Test
    @DisplayName("RG11 (EF4) : une relecture rendue refuse aussi le remplacement")
    void relecture_rendue_refuse_le_remplacement() {
        Exercice exercice = exercice(StatutExercice.RELU);
        unExercice(exercice, session(false));
        when(relectureRepository.existsByExerciceId(EXERCICE_ID)).thenReturn(true);

        assertThatThrownBy(() -> exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(LIEN_REMPLACEMENT)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.RELECTURE_COMMENCEE));
    }

    @Test
    @DisplayName("RG14 (EF4) : une session clôturée refuse le remplacement en 409 SESSION_CLOTUREE")
    void session_cloturee_refuse_le_remplacement() {
        Exercice exercice = exercice(StatutExercice.DEPOSE);
        unExercice(exercice, session(true));

        assertThatThrownBy(() -> exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(LIEN_REMPLACEMENT)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.SESSION_CLOTUREE);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.CONFLICT);
                });

        // La clôture est contrôlée avant l'état de la relecture : le gel de RG14
        // prime, et la relecture n'est même pas interrogée.
        verify(relectureRepository, never()).existsByExerciceId(any());
    }

    @Test
    @DisplayName("EF4 : un exercice inconnu est refusé en 404 EXERCICE_INCONNU")
    void remplacement_d_un_exercice_inconnu() {
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(LIEN_REMPLACEMENT)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> {
                    ExceptionMetier erreur = (ExceptionMetier) thrown;
                    assertThat(erreur.getCode()).isEqualTo(CodeErreur.EXERCICE_INCONNU);
                    assertThat(erreur.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(sessionRepository, never()).findById(any());
    }

    @ParameterizedTest(name = "lien refusé : [{0}]")
    @ValueSource(strings = {"", "   ", "pas-une-url", "exemple.org/x.pdf", "ftp://exemple.org/x.pdf"})
    @DisplayName("EF4 : un lien mal formé est refusé en 400 LIEN_INVALIDE, sans atteindre la base")
    void remplacement_avec_un_lien_invalide(String lienInvalide) {
        assertThatThrownBy(() -> exerciceService.remplacerLien(EXERCICE_ID,
                new RemplacementLienRequete(lienInvalide)))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(thrown -> assertThat(((ExceptionMetier) thrown).getCode())
                        .isEqualTo(CodeErreur.LIEN_INVALIDE));

        // Le format est contrôlé avant toute lecture : aucun exercice n'est chargé.
        verify(exerciceRepository, never()).findById(any());
    }

    /** Exercice existant, rattaché à la session donnée. */
    private void unExercice(Exercice exercice, Session session) {
        when(exerciceRepository.findById(EXERCICE_ID)).thenReturn(Optional.of(exercice));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
    }

    private Exercice exercice(StatutExercice statut) {
        Exercice exercice = new Exercice(SESSION_ID, ETUDIANT_ID, LIEN_VALIDE, statut, LocalDateTime.now());
        ReflectionTestUtils.setField(exercice, "id", EXERCICE_ID);
        return exercice;
    }

    private Session session(boolean cloturee) {
        Session session = new Session("Cours de test", "ABC234", 1L,
                LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(10));
        // Identifiant et état de clôture normalement portés par la base.
        ReflectionTestUtils.setField(session, "id", SESSION_ID);
        ReflectionTestUtils.setField(session, "cloturee", cloturee);
        return session;
    }
}
