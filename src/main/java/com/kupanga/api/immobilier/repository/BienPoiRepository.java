package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.BienPoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BienPoiRepository extends JpaRepository<BienPoi, Long> {

    /** C2 : POI d'une ancienne position, effacés avant un nouveau calcul (contrainte unique bien + type). */
    @Modifying
    @Transactional
    @Query("DELETE FROM BienPoi p WHERE p.bien.id = :bienId")
    void supprimerParBien(@Param("bienId") Long bienId);

    /** C2 (revue) : remplace les POI d'un bien d'un seul bloc (deux calculs concurrents ne se mélangent pas). */
    @Transactional
    default void remplacer(Long bienId, List<BienPoi> pois) {
        supprimerParBien(bienId);
        saveAll(pois);
    }
}
