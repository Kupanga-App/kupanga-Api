package com.kupanga.api.login.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.service.CreationCompteService;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.mapper.UtilisateurMapper;
import com.kupanga.api.utilisateur.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreationCompteServiceImpl implements CreationCompteService {

    private final UtilisateurService utilisateurService;
    private final EmailService emailService;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UtilisateurDTO creationUtilisateur(String email ,String role) throws UserAlreadyExistsException {

        utilisateurService.verifieSiUtilisateurEstPresent(email);
        utilisateurService.verifieSiRoleUtilisateurCorrect(role);
        String motDePasseTemporaire = generationMotDePasseTemporaire();
        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasseTemporaire))
                .role(Role.valueOf(role))
                .build();
        utilisateurService.save(utilisateur);
        try {
            emailService.envoyerMailMotDePasseTemporaire(email, motDePasseTemporaire);

        } catch (Exception e) {

            System.err.println("Erreur envoi email: " + e.getMessage());
        }
        return utilisateurMapper.toDTO(utilisateur);
    }

    /**
     * Génère un Mot de passe provisoire
     * @return un mot de passe provisoire
     */

    private String generationMotDePasseTemporaire(){

        return java.util.UUID.randomUUID().toString().substring(0, 8);

    }
}
