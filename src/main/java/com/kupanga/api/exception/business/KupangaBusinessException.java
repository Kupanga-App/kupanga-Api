package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class KupangaBusinessException extends BusinessException{

    public KupangaBusinessException(String message , HttpStatus status){

        super( message , status);
    }
}
