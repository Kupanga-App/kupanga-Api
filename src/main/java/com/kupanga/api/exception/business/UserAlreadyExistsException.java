package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String email) {

        super("Un utilisateur existe déjà avec l'email : " + email, HttpStatus.CONFLICT);
    }
}
