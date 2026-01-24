package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {


    public UserNotFoundException(String email) {
        super("Aucun utilisateur trouvé pour l'email : " + email, HttpStatus.NOT_FOUND);
    }
}

