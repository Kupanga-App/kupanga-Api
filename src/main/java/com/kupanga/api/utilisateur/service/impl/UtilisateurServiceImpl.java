package com.kupanga.api.utilisateur.service.impl;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import com.kupanga.api.utilisateur.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public Optional<Utilisateur> getUtilisateurByEmail(String email){

        return utilisateurRepository.findByEmail(email);
    }

    @Override
    public void verifieSiUtilisateurEstPresent(String email) throws UserAlreadyExistsException{

        if(utilisateurRepository.existsByEmail(email)){
            throw new UserAlreadyExistsException(email);
        }
    }

    @Override
    public void save(Utilisateur utilisateur){

        utilisateurRepository.save(utilisateur);
    }

}
