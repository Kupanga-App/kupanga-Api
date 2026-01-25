package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BusinessException {

    public TokenExpiredException() {
        super("Token expiré", HttpStatus.BAD_REQUEST);
    }
}
