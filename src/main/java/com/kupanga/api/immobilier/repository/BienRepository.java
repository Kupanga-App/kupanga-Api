package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Bien;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BienRepository extends JpaRepository<Bien, Long> {
}
