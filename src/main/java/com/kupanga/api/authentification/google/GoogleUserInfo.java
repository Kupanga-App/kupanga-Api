package com.kupanga.api.authentification.google;

public record GoogleUserInfo(
        String googleId,
        String email,
        String firstName,
        String lastName,
        String pictureUrl
) {}
