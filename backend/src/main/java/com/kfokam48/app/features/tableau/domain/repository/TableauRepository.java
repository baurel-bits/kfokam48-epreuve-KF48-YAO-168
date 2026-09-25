package com.kfokam48.app.features.tableau.domain.repository;

import com.kfokam48.app.features.promotion.domain.entity.Etudiant;
import com.kfokam48.app.features.relecture.domain.entity.StatutRelecture;
import com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant;
import com.kfokam48.app.features.tableau.application.dto.IdentiteEtudiant;
import com.kfokam48.app.features.tableau.application.dto.MoyenneParEtudiant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Requêtes d'agrégation du tableau de bord (EF9).
 *
 * <p>Interface volontairement réduite à {@link Repository} — et non à
 * {@code JpaRepository} — pour que cette feature n'expose aucune opération
 * d'écriture sur {@code Etudiant}, qui n'est ici qu'un point d'ancrage.
 *
 * <p><strong>Anti-N+1 :</strong> le nombre de requêtes est <em>constant</em>,
 * quel que soit l'effectif de la promotion. Chaque indicateur est un
 * {@code GROUP BY} ramené une fois pour toute la promotion, puis recollé en
 * mémoire par le service ; il n'y a jamais « une requête par étudiant ». Aucune
 * entité lourde n'est chargée : les cinq requêtes ne renvoient que des
 * projections.
 */
public interface TableauRepository extends Repository<Etudiant, Long> {

    /**
     * Identités des étudiants de la promotion, dans l'ordre d'affichage
     * (nom puis prénom) — même ordre que la liste du sélecteur d'étudiant (Q1).
     */
    @Query("""
            select new com.kfokam48.app.features.tableau.application.dto.IdentiteEtudiant(
                e.id, e.prenom, e.nom)
            from Etudiant e
            where e.promotionId = :promotionId
            order by e.nom asc, e.prenom asc
            """)
    List<IdentiteEtudiant> listerIdentitesDeLaPromotion(@Param("promotionId") Long promotionId);

    /** Nombre de présences par étudiant, présences manuelles incluses (EF2, EF10). */
    @Query("""
            select new com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant(
                p.etudiantId, count(p.id))
            from Presence p
            where p.etudiantId in :etudiantIds
            group by p.etudiantId
            """)
    List<ComptageParEtudiant> compterPresencesParEtudiant(
            @Param("etudiantIds") Collection<Long> etudiantIds);

    /**
     * Nombre d'exercices déposés par étudiant.
     *
     * <p>Le comptage porte sur les lignes d'{@code exercice}, dont l'unicité
     * {@code (session_id, etudiant_id)} interdit les doublons : un remplacement
     * de lien (EF4) n'augmente donc pas le total, il réécrit la même ligne.
     */
    @Query("""
            select new com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant(
                x.etudiantId, count(x.id))
            from Exercice x
            where x.etudiantId in :etudiantIds
            group by x.etudiantId
            """)
    List<ComptageParEtudiant> compterExercicesParEtudiant(
            @Param("etudiantIds") Collection<Long> etudiantIds);

    /**
     * Moyenne des notes reçues, en tant qu'auteur. Seules les relectures rendues
     * portent une note : le {@code statut} est donc filtré ici, et une relecture
     * en attente ne tire pas la moyenne vers le bas.
     *
     * <p>Un étudiant absent du résultat n'a aucune note rendue : le service lui
     * affecte {@code null}, jamais {@code 0}.
     */
    @Query("""
            select new com.kfokam48.app.features.tableau.application.dto.MoyenneParEtudiant(
                r.auteurId, avg(r.note))
            from Relecture r
            where r.auteurId in :etudiantIds and r.statut = :statut
            group by r.auteurId
            """)
    List<MoyenneParEtudiant> moyennesDesNotesRecues(
            @Param("etudiantIds") Collection<Long> etudiantIds,
            @Param("statut") StatutRelecture statut);

    /**
     * Relectures non encore rendues des exercices de ces étudiants (RG9).
     * Vue depuis l'{@code auteurId} : on compte ce qui reste à recevoir.
     */
    @Query("""
            select new com.kfokam48.app.features.tableau.application.dto.ComptageParEtudiant(
                r.auteurId, count(r.id))
            from Relecture r
            where r.auteurId in :etudiantIds and r.statut = :statut
            group by r.auteurId
            """)
    List<ComptageParEtudiant> compterRelecturesEnAttenteParAuteur(
            @Param("etudiantIds") Collection<Long> etudiantIds,
            @Param("statut") StatutRelecture statut);
}
