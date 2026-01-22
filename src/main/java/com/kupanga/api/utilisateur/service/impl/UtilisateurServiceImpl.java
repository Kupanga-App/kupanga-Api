package com.kupanga.api.utilisateur.service.impl;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import com.kupanga.api.utilisateur.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    public static final List<Role> ROLES = List.of(
            Role.ROLE_PROPRIETAIRE,
            Role.ROLE_LOCATAIRE
    );

    @Override
    public Utilisateur getUtilisateurByEmail(String email){

        return utilisateurRepository.findByEmail(email)
                .orElseThrow( () -> new UserNotFoundException(email));
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

    @Override
    public void verifieSiRoleUtilisateurCorrect(Role role) throws InvalidRoleException{

        if(role == null || !ROLES.contains(role)){
            throw new InvalidRoleException();
        }
    }

    @Override
    public void verifieSiUtilisateurEstLocataire(Role role) throws InvalidRoleException{

        if(!Role.ROLE_LOCATAIRE.equals(role)){

            throw new InvalidRoleException("L'utilisateur n'a pas le rôle de locataire pour accéder à cette ressource :" + role);
        }
    }

    @Override
    public void verifieSiUtilisateurEstProprietaire(Role role) throws InvalidRoleException{

        if(!Role.ROLE_PROPRIETAIRE.equals(role)){

            throw new InvalidRoleException("L'utilisateur n'a pas le rôle de propriétaire pour accéder à cette ressource :" + role);
        }
    }

}
