package com.kupanga.api.authentification.service.impl;

import com.kupanga.api.authentification.entity.PasswordResetToken;
import com.kupanga.api.authentification.repository.PasswordResetTokenRepository;
import com.kupanga.api.authentification.service.PasswordResetTokenService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static com.kupanga.api.authentification.constant.AuthConstant.TOKEN_REINITIALISATION_INVALIDE;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public PasswordResetToken getByToken(String token){

        return passwordResetTokenRepository.findByToken(token)
                .orElseThrow(()-> new KupangaBusinessException(TOKEN_REINITIALISATION_INVALIDE, HttpStatus.BAD_REQUEST));
    }

    @Override
    public void save(PasswordResetToken passwordResetToken){

        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public void delete(PasswordResetToken passwordResetToken){

        passwordResetTokenRepository.delete(passwordResetToken);
    }

    @Override
    @Transactional
    public void deleteIfExist(Long userId) {
        passwordResetTokenRepository.deleteByUserId(userId);
    }
}
