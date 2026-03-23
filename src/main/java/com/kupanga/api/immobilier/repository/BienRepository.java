package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Bien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BienRepository extends JpaRepository<Bien, Long>, JpaSpecificationExecutor<Bien> {

    @Query(
            """
            select b
            from Bien b
            inner join fetch b.proprietaire
            left join fetch b.locataire
            left join fetch b.contrats
            left join fetch b.quittances
            left join fetch b.etatsDesLieux
            left join fetch b.documents
            inner join fetch b.images
            where b.id = :id
            """
    )
    Optional<Bien> findWithAllProperties(Long id);

    @Query(
            """
            select b
            from Bien b
            inner join b.proprietaire p
            where p.id = :userId
            """
    )
    List<Bien> findAllPropertiesAssociateToUser(Long userId);
}
