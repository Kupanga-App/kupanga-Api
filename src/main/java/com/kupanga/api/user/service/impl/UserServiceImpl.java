package com.kupanga.api.user.service.impl;

import com.kupanga.api.exception.business.*;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.service.UserService;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.utils.EmailUtils;
import com.kupanga.api.user.utils.TelephoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public static final List<Role> ROLES = List.of(
            Role.ROLE_PROPRIETAIRE,
            Role.ROLE_LOCATAIRE
    );

    @Override
    public User getUserByEmail(String email){

        return userRepository.findByMail(EmailUtils.normaliser(email))
                .orElseThrow( () -> new UserNotFoundException(email));
    }

    @Override
    public void verifyIfUserExistWithEmail(String email) throws UserAlreadyExistsException{

        if(userRepository.existsByMail(EmailUtils.normaliser(email))){
            throw new UserAlreadyExistsException(email);
        }
    }

    @Override
    public void save(User utilisateur){

        userRepository.save(utilisateur);
    }

    @Override
    public void verifyIfRoleOfUserValid(Role role) throws InvalidRoleException{

        if(role == null || !ROLES.contains(role)){
            throw new InvalidRoleException();
        }
    }

    @Override
    public void verifyIfUserIsTenant(Role role) throws InvalidRoleException{

        if(!Role.ROLE_LOCATAIRE.equals(role)){

            throw new InvalidRoleException("L'utilisateur n'a pas le rôle de locataire pour accéder à cette ressource :" + role);
        }
    }

    @Override
    public void verifyIfUserIsOwner(Role role) throws InvalidRoleException{

        if(!Role.ROLE_PROPRIETAIRE.equals(role)){

            throw new InvalidRoleException("L'utilisateur n'a pas le rôle de propriétaire pour accéder à cette ressource :" + role);
        }
    }

    @Override
    public void isCorrectPassword(String passwordLogin , String passwordUser){

        if(!passwordEncoder.matches(passwordLogin , passwordUser) ){

            throw new InvalidPasswordException();
        }
    }

    @Override
    public User findById(Long userId) {

        return userRepository.findById(userId).orElseThrow(
                () -> new KupangaBusinessException("Aucun utilisateur trouvé pour l'Id : " + userId ,
                        HttpStatus.NOT_FOUND)
        );
    }

    @Override
    public java.util.Optional<User> findOptionalByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }

    @Override
    public java.util.Optional<User> findOptionalByMail(String mail) {
        return userRepository.findByMail(EmailUtils.normaliser(mail));
    }

    @Override
    @Transactional
    public UserDTO modifierTelephone(String email, String telephone) {
        User user = getUserByEmail(email);
        user.setTelephone(TelephoneUtils.normaliser(telephone));
        userRepository.save(user);
        return userMapper.toDTO(user);
    }
}
