package com.kupanga.api.authentification.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A14 : jeton du lien de vérification de l'adresse e-mail (un par compte, valable 24 h). */
@Entity
@Table(name = "jeton_verification_email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JetonVerificationEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiration;
}
