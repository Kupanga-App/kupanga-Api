package com.kupanga.api.exception.business;


import org.springframework.http.HttpStatus;

public class InvalidRoleException extends BusinessException {


    public InvalidRoleException() {
        super("Rôle métier invalide ", HttpStatus.BAD_REQUEST);
    }


    public InvalidRoleException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}


