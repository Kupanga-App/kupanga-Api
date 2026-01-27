package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.repository.RefreshTokenRepository;
import com.kupanga.api.authentification.service.RefreshTokenService;
import com.kupanga.api.utilisateur.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String createRefreshToken(User user) {

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId());

        if(refreshToken != null){

            refreshTokenRepository.delete(refreshToken);
        }

        String token = UUID.randomUUID().toString();

        refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiration(Instant.now().plus(14, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Override
    public RefreshToken getByToken(String token){

        return refreshTokenRepository.findByToken(token)
                .orElseThrow( () -> new KupangaBusinessException("token non autorisé " , HttpStatus.UNAUTHORIZED));
    }

    @Override
    public void deleteRefreshToken(String token){

        refreshTokenRepository.delete(getByToken(token));

    }

}
