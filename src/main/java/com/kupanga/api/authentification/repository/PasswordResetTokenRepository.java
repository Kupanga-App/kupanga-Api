package com.kupanga.api.authentification.repository;

import com.kupanga.api.authentification.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser_Id(Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
