package com.kupanga.api.login.repository;

import com.kupanga.api.login.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken , Long> {

    Optional<RefreshToken> findByToken(String token);

    @Query("""
            select r
            from RefreshToken r
            inner join r.user u
            where u.id =:userId
    """)
    RefreshToken findByUserId(Long userId);
}
