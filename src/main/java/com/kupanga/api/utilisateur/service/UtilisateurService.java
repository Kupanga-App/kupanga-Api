package com.kupanga.api.utilisateur.service;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.utilisateur.entity.Utilisateur;

import java.util.Optional;

public interface UtilisateurService {

    Optional<Utilisateur> getUtilisateurByEmail(String email);

    void verifieSiUtilisateurEstPresent(String email) throws UserAlreadyExistsException;

    void save(Utilisateur utilisateur);
}
