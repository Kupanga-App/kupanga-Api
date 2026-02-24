package com.kupanga.api.exception.business;

import org.springframework.http.HttpStatus;

public class BienNotFoundException extends BusinessException {

    public BienNotFoundException(Long id) {
        super("Aucun bien trouvé avec l'ID : " + id, HttpStatus.NOT_FOUND);
    }
}
