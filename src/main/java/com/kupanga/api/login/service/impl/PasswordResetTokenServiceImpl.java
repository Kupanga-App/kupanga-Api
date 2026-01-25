package com.kupanga.api.login.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.login.entity.PasswordResetToken;
import com.kupanga.api.login.repository.PasswordResetTokenRepository;
import com.kupanga.api.login.service.PasswordResetTokenService;
import com.kupanga.api.utilisateur.entity.User;
import com.kupanga.api.utilisateur.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.kupanga.api.email.constantes.Constante.RESET_LINK;
import static com.kupanga.api.login.constant.LoginConstant.MOT_DE_PASSE_A_JOUR;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

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
