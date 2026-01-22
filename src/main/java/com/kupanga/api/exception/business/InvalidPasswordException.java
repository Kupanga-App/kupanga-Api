package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BusinessException {

    public InvalidPasswordException(){

        super("Mot de passe invalide" , HttpStatus.UNAUTHORIZED);
    }
}
