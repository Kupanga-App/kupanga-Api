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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreationCompteServiceImpl implements CreationCompteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreationCompteServiceImpl.class);
    private final UtilisateurService utilisateurService;
    private final EmailService emailService;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UtilisateurDTO creationUtilisateur(String email ,Role role) throws UserAlreadyExistsException {

        LOGGER.info("Service pour la création du compte utilisateur démarré");

        utilisateurService.verifieSiUtilisateurEstPresent(email);
        utilisateurService.verifieSiRoleUtilisateurCorrect(role);

        String motDePasseTemporaire = generationMotDePasseTemporaire();

        Utilisateur utilisateur = Utilisateur.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasseTemporaire))
                .role(role)
                .build();

        utilisateurService.save(utilisateur);
        LOGGER.debug("utilisateur sauvegardé {} " , utilisateur  );

        try {

            emailService.envoyerMailMotDePasseTemporaire(email, motDePasseTemporaire);

            LOGGER.info("Email renvoyé à l'utilisateur à l'adresse {} avec le mot de passe temporaire" , email);

        } catch (Exception e) {

            LOGGER.warn("Erreur lors de l'envoi  de l'email de mot de passe temporaire à {} " +
                    "mais l'utilisateur a été crée" , email);

            LOGGER.debug("détail de l'exception : {}" ,e.getMessage());
        }

        LOGGER.info(" Fin du service de création du compte utilisateur ");
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
