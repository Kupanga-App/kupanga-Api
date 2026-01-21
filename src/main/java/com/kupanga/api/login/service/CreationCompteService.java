package com.kupanga.api.login.service;


import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;

public interface CreationCompteService {

    UtilisateurDTO creationUtilisateur(String email , String role);
}
