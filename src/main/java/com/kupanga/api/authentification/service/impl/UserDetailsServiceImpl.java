package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
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


    private final UserRepository userRepository;

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
        User user = userRepository.findByMail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur non trouvé avec l'email : " + email
                        )
                );

        // Utilisateur Google sans rôle encore attribué → authorities vides (phase d'onboarding)
        Role role = user.getRole();
        var authorities = role != null
                ? Collections.singleton(new SimpleGrantedAuthority(role.name()))
                : Collections.<GrantedAuthority>emptySet();

        // Utilisateur Google sans mot de passe → chaîne vide pour satisfaire Spring Security
        String password = user.getPassword() != null ? user.getPassword() : "";

        return new org.springframework.security.core.userdetails.User(
                user.getMail(),
                password,
                authorities
        );
    }
}

