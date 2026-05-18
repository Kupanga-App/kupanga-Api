package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {

    Optional<Contrat> findByTokenSignature(String token);

    @Query("SELECT c FROM Contrat c LEFT JOIN FETCH c.locataire WHERE c.bien.id = :bienId ORDER BY c.createdAt DESC")
    List<Contrat> findByBienId(@Param("bienId") Long bienId);

    long countByBienId(Long bienId);

    @Query("SELECT c.bien.id, COUNT(c) FROM Contrat c GROUP BY c.bien.id")
    List<Object[]> countParBien();
}
