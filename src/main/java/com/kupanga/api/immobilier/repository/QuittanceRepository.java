package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutQuittance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuittanceRepository extends JpaRepository<Quittance, Long>, JpaSpecificationExecutor<Quittance> {

    /**
     * Toutes les quittances d'un locataire pour un bien donné.
     */
    List<Quittance> findByBienIdAndLocataireId(Long bienId, Long locataireId);

    @Query("SELECT q FROM Quittance q LEFT JOIN FETCH q.locataire WHERE q.bien.id = :bienId ORDER BY q.annee DESC, q.createdAt DESC")
    List<Quittance> findByBienId(@Param("bienId") Long bienId);

    long countByBienId(Long bienId);

    @Query("SELECT q.bien.id, COUNT(q) FROM Quittance q GROUP BY q.bien.id")
    List<Object[]> countParBien();

    /**
     * Toutes les quittances d'un propriétaire.
     */
    List<Quittance> findByProprietaireId(Long proprietaireId);

    @Query("""
            SELECT q
            FROM Quittance q
            WHERE q.bien.id = :bienId
              AND q.mois = :mois
              AND q.annee = :annee
    """)
    Optional<Quittance> findByBienIdAndMoisAndAnnee(
            @Param("bienId") Long bienId,
            @Param("mois") String mois,
            @Param("annee") Integer annee
    );

    /**
     * Quittances en attente ou en retard pour un bien — utile pour les relances.
     */
    List<Quittance> findByBienIdAndStatutIn(Long bienId, List<StatutQuittance> statuts);

    /**
     * Charge la quittance avec toutes ses relations pour la génération PDF.
     */
    @Query("""
            SELECT q FROM Quittance q
            LEFT JOIN FETCH q.bien
            LEFT JOIN FETCH q.proprietaire
            LEFT JOIN FETCH q.locataire
            LEFT JOIN FETCH q.contrat
            WHERE q.id = :id
            """)
    Optional<Quittance> findWithAllRelations(@Param("id") Long id);
}