package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {

    Optional<Contrat> findByTokenSignature( String token);
}
