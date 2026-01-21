package com.kupanga.api.login.service.impl;

import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import com.kupanga.api.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * <p>
 * Implémentation de {@link UserDetailsService} utilisée par Spring Security
 * pour charger un utilisateur lors du processus d'authentification.
 * </p>
 *
 * <p>
 * Dans cette application :
 * </p>
 * <ul>
 *     <li>L'identifiant de connexion est l'email</li>
 *     <li>L'utilisateur est récupéré depuis la base de données</li>
 *     <li>Les rôles sont convertis en {@link GrantedAuthority}</li>
 * </ul>
 *
 * <p>
 * Spring Security invoque automatiquement cette classe :
 * </p>
 * <ul>
 *     <li>lors de l'authentification (login)</li>
 *     <li>lors de la validation d'un JWT (via le filtre JWT)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * Repository permettant l'accès aux utilisateurs en base de données.
     */
    private final UtilisateurRepository utilisateurRepository;

    /**
     * Charge un utilisateur à partir de son email.
     *
     * <p>
     * Le paramètre {@code email} correspond au "username" dans Spring Security
     * (nom générique utilisé par le framework).
     * </p>
     *
     * <p>
     * Si aucun utilisateur n'est trouvé, une {@link UsernameNotFoundException}
     * est levée, ce qui provoque l'échec de l'authentification.
     * </p>
     *
     * @param email Email utilisé comme identifiant de connexion
     * @return {@link UserDetails} utilisé par Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Récupération de l'utilisateur à partir de son email
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur non trouvé avec l'email : " + email
                        )
                );

        // Vérification que l'utilisateur a bien un rôle défini
        Role role = user.getRole();
        if (role == null) {
            throw new IllegalArgumentException("L'utilisateur a un rôle invalide");
        }

        /*
         * Construction de l'objet UserDetails attendu par Spring Security.
         *
         * 1. email
         *    → Identifiant de l'utilisateur (username).
         *
         * 2. password
         *    → Mot de passe déjà encodé en base (ex: BCrypt).
         *
         * 3. authorities
         *    → Rôles / permissions de l'utilisateur.
         *      Exemple : ROLE_PROPRIETAIRE, ROLE_LOCATAIRE, etc.
         *
         * ⚠️ Important :
         * Si tu utilises hasRole("PROPRIETAIRE") ou hasRole("LOCATAIRE") dans
         * ta configuration, le rôle doit être stocké sous la forme "ROLE_PROPRIETAIRE",
         * "ROLE_LOCATAIRE".
         */
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getMotDePasse(),
                Collections.singleton(
                        new SimpleGrantedAuthority(role.name())
                )
        );
    }
}

