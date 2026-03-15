package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat , Long > {

    Optional<Contrat> findByTokenSignature( String token);
}
