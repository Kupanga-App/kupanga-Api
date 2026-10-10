package com.kupanga.api.authentification.repository;

import com.kupanga.api.authentification.entity.JetonVerificationEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JetonVerificationEmailRepository extends JpaRepository<JetonVerificationEmail, Long> {

    Optional<JetonVerificationEmail> findByToken(String token);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM JetonVerificationEmail j WHERE j.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
