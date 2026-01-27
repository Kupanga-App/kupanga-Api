package com.kupanga.api.user.service.impl;

import com.kupanga.api.exception.business.InvalidPasswordException;
import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.exception.business.UserNotFoundException;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

        return userRepository.findByMail(email)
                .orElseThrow( () -> new UserNotFoundException(email));
    }

    @Override
    public void verifyIfUserExistWithEmail(String email) throws UserAlreadyExistsException{

        if(userRepository.existsByMail(email)){
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
    @Transactional
    public UserDTO completeProfil(UserFormDTO userFormDTO){

        User user = getUserByEmail(userFormDTO.mail());
        verifyIfRoleOfUserValid(userFormDTO.role());
        isCorrectPassword(userFormDTO.password() , user.getPassword());
        user.setRole(userFormDTO.role());
        user.setFirstName(userFormDTO.firstName());
        user.setLastName(userFormDTO.lastName());
        user.setHasCompleteProfil(true);
        save(user);

        return userMapper.toDTO(user);
    }
}
