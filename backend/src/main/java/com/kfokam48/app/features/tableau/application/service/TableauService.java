package com.kfokam48.app.features.tableau.application.service;

import com.kfokam48.app.common.error.CodeErreur;
import com.kfokam48.app.common.exception.ExceptionMetier;
import com.kfokam48.app.features.promotion.domain.repository.PromotionRepository;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant;
import com.kfokam48.app.features.tableau.application.dto.IdentiteEtudiant;
import com.kfokam48.app.features.tableau.application.dto.LigneTableauReponse;
import com.kfokam48.app.features.tableau.application.dto.MoyenneParEtudiant;
import com.kfokam48.app.features.tableau.domain.repository.TableauRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord du formateur, par promotion (EF9) — opération imposée
 * {@code GET /api/tableau}.
 *
 * <p>Les indicateurs sont agrégés par la base (cf. {@link TableauRepository}) :
 * le service ne fait que recoller des résultats déjà comptés, ce qui maintient
 * un nombre de requêtes constant quel que soit l'effectif (ENF2, ENF3) et laisse
 * le frontend sans aucun calcul métier (F3).
 *
 * <p>Un étudiant <strong>sans aucune activité</strong> figure malgré tout dans le
 * tableau, avec des compteurs à zéro et une moyenne nulle : c'est un critère
 * d'acceptation explicite de l'EF9 (il doit rester visible pour être relancé).
 */
@Service
public class TableauService {

    private final PromotionRepository promotionRepository;
    private final TableauRepository tableauRepository;

    public TableauService(PromotionRepository promotionRepository, TableauRepository tableauRepository) {
        this.promotionRepository = promotionRepository;
        this.tableauRepository = tableauRepository;
    }

    /**
     * @throws ExceptionMetier {@code 404 PROMOTION_INCONNUE} si la promotion n'existe pas.
     */
    @Transactional(readOnly = true)
    public List<LigneTableauReponse> tableauDeLaPromotion(Long promotionId) {
        if (!promotionRepository.existsById(promotionId)) {
            throw new ExceptionMetier(CodeErreur.PROMOTION_INCONNUE, HttpStatus.NOT_FOUND,
                    "La promotion %d est inconnue.".formatted(promotionId));
        }

        List<IdentiteEtudiant> etudiants = tableauRepository.listerIdentitesDeLaPromotion(promotionId);
        if (etudiants.isEmpty()) {
            // Rien à agréger, et `in ()` n'est pas du SQL valide : on s'arrête là.
            return List.of();
        }

        List<Long> etudiantIds = etudiants.stream().map(IdentiteEtudiant::etudiantId).toList();

        Map<Long, Long> presences = indexerComptages(tableauRepository.compterPresencesParEtudiant(etudiantIds));
        Map<Long, Long> depots = indexerComptages(tableauRepository.compterExercicesParEtudiant(etudiantIds));
        Map<Long, Long> relecturesEnAttente = indexerComptages(
                tableauRepository.compterRelecturesEnAttenteParAuteur(etudiantIds, StatutRelecture.EN_ATTENTE));
        // Seules les relectures rendues portent une note (RG7/RG9) : les autres ne
        // comptent pas dans la moyenne, et aucune note rendue ⇒ moyenne nulle.
        Map<Long, Double> moyennes = indexerMoyennes(
                tableauRepository.moyennesDesNotesRecues(etudiantIds, StatutRelecture.RENDUE));

        return etudiants.stream()
                .map(etudiant -> new LigneTableauReponse(
                        etudiant.etudiantId(),
                        "%s %s".formatted(etudiant.prenom(), etudiant.nom()),
                        presences.getOrDefault(etudiant.etudiantId(), 0L),
                        depots.getOrDefault(etudiant.etudiantId(), 0L),
                        moyennes.get(etudiant.etudiantId()),
                        relecturesEnAttente.getOrDefault(etudiant.etudiantId(), 0L)))
                .toList();
    }

    private static Map<Long, Long> indexerComptages(List<ComptageParEtudiant> comptages) {
        Map<Long, Long> index = new HashMap<>();
        comptages.forEach(comptage -> index.put(comptage.etudiantId(), comptage.total()));
        return index;
    }

    private static Map<Long, Double> indexerMoyennes(List<MoyenneParEtudiant> moyennes) {
        Map<Long, Double> index = new HashMap<>();
        moyennes.forEach(moyenne -> index.put(moyenne.etudiantId(), arrondir(moyenne.moyenne())));
        return index;
    }

    /**
     * Arrondit à deux décimales, côté serveur : afficher « 14,3333333 » n'aiderait
     * pas le formateur, et laisser le frontend arrondir serait un calcul métier
     * côté client (F3).
     */
    private static Double arrondir(Double moyenne) {
        return moyenne == null ? null
                : BigDecimal.valueOf(moyenne).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
