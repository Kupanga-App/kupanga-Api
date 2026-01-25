package com.kupanga.api.login.service.impl;

import com.kupanga.api.login.entity.PasswordResetToken;
import com.kupanga.api.login.repository.PasswordResetTokenRepository;
import com.kupanga.api.login.service.PasswordResetTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public PasswordResetToken getByToken(String token){

        return passwordResetTokenRepository.findByToken(token)
                .orElseThrow(()-> new RuntimeException("Token invalide"));
    }

    @Override
    public void save(PasswordResetToken passwordResetToken){

        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public void delete(PasswordResetToken passwordResetToken){

        passwordResetTokenRepository.delete(passwordResetToken);
    }
}
