package com.kupanga.api.utilisateur.repository;

import com.kupanga.api.utilisateur.entity.Utilisateur;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository {

    Optional<Utilisateur> findByEmail(String email);
}
