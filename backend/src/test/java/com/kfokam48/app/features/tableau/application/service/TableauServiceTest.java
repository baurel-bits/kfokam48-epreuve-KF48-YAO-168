package com.kfokam48.app.features.tableau.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.promotion.domain.repository.PromotionRepository;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant;
import com.kfokam48.app.features.tableau.application.dto.IdentiteEtudiant;
import com.kfokam48.app.features.tableau.application.dto.LigneTableauReponse;
import com.kfokam48.app.features.tableau.application.dto.MoyenneParEtudiant;
import com.kfokam48.app.features.tableau.domain.repository.TableauRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Test unitaire (B6) de l'agrégation du tableau de bord (EF9).
 *
 * <p>Il fixe ce que le tableau répond <em>par étudiant</em>, y compris les cas
 * qui ne se distinguent que par une valeur : moyenne nulle plutôt que zéro, et
 * étudiant sans activité présent dans la liste.
 */
@ExtendWith(MockitoExtension.class)
class TableauServiceTest {

    private static final Long PROMOTION_ID = 7L;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private TableauRepository tableauRepository;

    private TableauService tableauService;

    @BeforeEach
    void initialiserLeService() {
        tableauService = new TableauService(promotionRepository, tableauRepository);
    }

    @Test
    @DisplayName("EF9 : une promotion inconnue est refusée en 404 PROMOTION_INCONNUE")
    void refuse_une_promotion_inconnue() {
        when(promotionRepository.existsById(PROMOTION_ID)).thenReturn(false);

        assertThatThrownBy(() -> tableauService.tableauDeLaPromotion(PROMOTION_ID))
                .isInstanceOf(ExceptionMetier.class)
                .satisfies(echec -> {
                    ExceptionMetier metier = (ExceptionMetier) echec;
                    assertThat(metier.getCode()).isEqualTo(CodeErreur.PROMOTION_INCONNUE);
                    assertThat(metier.getStatut()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        // Aucune agrégation n'est lancée pour une promotion qui n'existe pas.
        verifyNoInteractions(tableauRepository);
    }

    @Test
    @DisplayName("EF9 : un étudiant sans activité figure avec des compteurs à zéro et une moyenne nulle")
    void affiche_un_etudiant_sans_activite() {
        preparerPromotion(List.of(new IdentiteEtudiant(11L, "Ana", "Absente")));
        preparerAgregatsVides(List.of(11L));

        assertThat(tableauService.tableauDeLaPromotion(PROMOTION_ID))
                .singleElement()
                .satisfies(ligne -> {
                    assertThat(ligne.etudiantId()).isEqualTo(11L);
                    assertThat(ligne.nom()).isEqualTo("Ana Absente");
                    assertThat(ligne.presences()).isZero();
                    assertThat(ligne.exercicesDeposes()).isZero();
                    // Critère explicite de l'EF9 : aucune note ⇒ `null`, jamais 0.
                    assertThat(ligne.moyenne()).isNull();
                    assertThat(ligne.relecturesEnAttente()).isZero();
                });
    }

    @Test
    @DisplayName("EF9 : les quatre indicateurs sont recollés par étudiant et la moyenne est arrondie par le serveur")
    void agrege_les_indicateurs_par_etudiant() {
        preparerPromotion(List.of(
                new IdentiteEtudiant(11L, "Ana", "Absente"),
                new IdentiteEtudiant(12L, "Bo", "Present")));
        List<Long> ids = List.of(11L, 12L);

        when(tableauRepository.compterPresencesParEtudiant(ids))
                .thenReturn(List.of(new ComptageParEtudiant(12L, 3L)));
        when(tableauRepository.compterExercicesParEtudiant(ids))
                .thenReturn(List.of(new ComptageParEtudiant(12L, 1L)));
        when(tableauRepository.compterRelecturesEnAttenteParAuteur(ids, StatutRelecture.EN_ATTENTE))
                .thenReturn(List.of(new ComptageParEtudiant(11L, 2L)));
        when(tableauRepository.moyennesDesNotesRecues(ids, StatutRelecture.RENDUE))
                .thenReturn(List.of(new MoyenneParEtudiant(12L, 14.333333333333334)));

        List<LigneTableauReponse> tableau = tableauService.tableauDeLaPromotion(PROMOTION_ID);

        // Un étudiant absent d'une agrégation est complété par zéro, pas omis.
        assertThat(tableau).containsExactly(
                new LigneTableauReponse(11L, "Ana Absente", 0, 0, null, 2),
                // 14.333… arrondi côté serveur : le client n'arrondit jamais (F3).
                new LigneTableauReponse(12L, "Bo Present", 3, 1, 14.33, 0));
    }

    @Test
    @DisplayName("EF9 : seules les notes rendues entrent dans la moyenne, les relectures en attente sont comptées à part")
    void distingue_les_notes_rendues_des_relectures_en_attente() {
        preparerPromotion(List.of(new IdentiteEtudiant(11L, "Ana", "Absente")));
        preparerAgregatsVides(List.of(11L));

        tableauService.tableauDeLaPromotion(PROMOTION_ID);

        // RG9 : une relecture non rendue ne porte pas de note ; elle est comptée
        // comme « en attente », côté auteur (ce que l'étudiant doit encore recevoir).
        verify(tableauRepository).moyennesDesNotesRecues(List.of(11L), StatutRelecture.RENDUE);
        verify(tableauRepository).compterRelecturesEnAttenteParAuteur(List.of(11L), StatutRelecture.EN_ATTENTE);
    }

    @Test
    @DisplayName("EF9 : une promotion sans étudiant renvoie une liste vide sans lancer d'agrégation")
    void promotion_sans_etudiant() {
        preparerPromotion(List.of());

        assertThat(tableauService.tableauDeLaPromotion(PROMOTION_ID)).isEmpty();

        // `in ()` n'est pas du SQL valide : aucune agrégation ne doit être tentée.
        verify(tableauRepository).listerIdentitesDeLaPromotion(PROMOTION_ID);
        verifyNoMoreInteractions(tableauRepository);
    }

    @Test
    @DisplayName("EF9 : le repository est sollicité une fois par indicateur, quel que soit l'effectif (pas de N+1)")
    void nombre_appels_constant_quel_que_soit_leffectif() {
        List<IdentiteEtudiant> promotion = new ArrayList<>();
        for (long id = 1; id <= 12; id++) {
            promotion.add(new IdentiteEtudiant(id, "Prenom", "Nom" + id));
        }
        List<Long> ids = promotion.stream().map(IdentiteEtudiant::etudiantId).toList();

        preparerPromotion(promotion);
        when(tableauRepository.compterPresencesParEtudiant(ids)).thenReturn(List.of());
        when(tableauRepository.compterExercicesParEtudiant(ids)).thenReturn(List.of());
        when(tableauRepository.compterRelecturesEnAttenteParAuteur(ids, StatutRelecture.EN_ATTENTE))
                .thenReturn(List.of());
        when(tableauRepository.moyennesDesNotesRecues(ids, StatutRelecture.RENDUE)).thenReturn(List.of());

        assertThat(tableauService.tableauDeLaPromotion(PROMOTION_ID)).hasSize(promotion.size());

        // Chaque indicateur est demandé UNE fois pour toute la promotion, jamais une
        // fois par étudiant — c'est la définition de l'absence de N+1 (ENF3).
        verify(tableauRepository).listerIdentitesDeLaPromotion(PROMOTION_ID);
        verify(tableauRepository).compterPresencesParEtudiant(ids);
        verify(tableauRepository).compterExercicesParEtudiant(ids);
        verify(tableauRepository).moyennesDesNotesRecues(ids, StatutRelecture.RENDUE);
        verify(tableauRepository).compterRelecturesEnAttenteParAuteur(ids, StatutRelecture.EN_ATTENTE);
        verifyNoMoreInteractions(tableauRepository);
    }

    private void preparerPromotion(List<IdentiteEtudiant> etudiants) {
        when(promotionRepository.existsById(PROMOTION_ID)).thenReturn(true);
        when(tableauRepository.listerIdentitesDeLaPromotion(PROMOTION_ID)).thenReturn(etudiants);
    }

    private void preparerAgregatsVides(List<Long> etudiantIds) {
        when(tableauRepository.compterPresencesParEtudiant(etudiantIds)).thenReturn(List.of());
        when(tableauRepository.compterExercicesParEtudiant(etudiantIds)).thenReturn(List.of());
        when(tableauRepository.compterRelecturesEnAttenteParAuteur(etudiantIds, StatutRelecture.EN_ATTENTE))
                .thenReturn(List.of());
        when(tableauRepository.moyennesDesNotesRecues(etudiantIds, StatutRelecture.RENDUE)).thenReturn(List.of());
    }
}
