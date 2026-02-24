package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Bien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BienRepository extends JpaRepository<Bien, Long> {

    List<Bien> findByProprietaire_Id(Long proprietaireId);

    List<Bien> findByVille(String ville);

    List<Bien> findByTitreContainingIgnoreCase(String keyword);

    List<Bien> findByLocataireIsNull();
}
