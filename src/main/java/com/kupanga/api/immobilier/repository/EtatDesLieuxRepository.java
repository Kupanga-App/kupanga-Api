package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.EtatDesLieux;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EtatDesLieuxRepository extends JpaRepository<EtatDesLieux, Long>, JpaSpecificationExecutor<EtatDesLieux> {

    Optional<EtatDesLieux> findByTokenSignature(String tokenSignature);

    @Query("SELECT e FROM EtatDesLieux e LEFT JOIN FETCH e.locataire WHERE e.bien.id = :bienId ORDER BY e.createdAt DESC")
    List<EtatDesLieux> findByBienId(@Param("bienId") Long bienId);

    long countByBienId(Long bienId);

    @Query("SELECT e.bien.id, COUNT(e) FROM EtatDesLieux e GROUP BY e.bien.id")
    List<Object[]> countParBien();

    /**
     * Charge l'EDL avec toutes ses relations en une seule requête
     * pour éviter les N+1 lors de la génération PDF.
     */
    @Query("""
            SELECT e FROM EtatDesLieux e
            LEFT JOIN FETCH e.bien
            LEFT JOIN FETCH e.proprietaire
            LEFT JOIN FETCH e.locataire
            LEFT JOIN FETCH e.pieces p
            LEFT JOIN FETCH p.elements
            LEFT JOIN FETCH e.compteurs
            LEFT JOIN FETCH e.cles
            WHERE e.id = :id
            """)
    Optional<EtatDesLieux> findWithAllRelations(@Param("id") Long id);
}