package com.kupanga.api.utilisateur.service;

import com.kupanga.api.utilisateur.entity.Utilisateur;

import java.util.Optional;

public interface UtilisateurService {

    Optional<Utilisateur> getUtilisateurByEmail(String email);
}
