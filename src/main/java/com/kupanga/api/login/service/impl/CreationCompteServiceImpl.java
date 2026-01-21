package com.kupanga.api.login.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.login.service.CreationCompteService;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.mapper.UtilisateurMapper;
import com.kupanga.api.utilisateur.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreationCompteServiceImpl implements CreationCompteService {

    private final UtilisateurService utilisateurService;
    private final EmailService emailService;
    private final UtilisateurMapper utilisateurMapper;

    @Override
    public UtilisateurDTO creationUtilisateur(String email ,String role){

        if (utilisateurService.getUtilisateurByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà : " + email);
        }
        String motDePasseTemporaire = generationMotDePasseTemporaire();
        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse(motDePasseTemporaire)
                .build();
        try {
            emailService.envoyerMailMotDePasseTemporaire(email, motDePasseTemporaire);

        } catch (Exception e) {

            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        return utilisateurMapper.toDTO(utilisateur);
    }

    private String generationMotDePasseTemporaire(){

        return java.util.UUID.randomUUID().toString().substring(0, 8);

    }
}
