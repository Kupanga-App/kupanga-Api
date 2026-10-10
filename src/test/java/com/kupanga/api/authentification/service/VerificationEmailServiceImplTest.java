package com.kupanga.api.authentification.service;

import com.kupanga.api.authentification.entity.JetonVerificationEmail;
import com.kupanga.api.authentification.repository.JetonVerificationEmailRepository;
import com.kupanga.api.authentification.service.impl.VerificationEmailServiceImpl;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.kupanga.api.authentification.constant.AuthConstant.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — VerificationEmailServiceImpl (A14)")
class VerificationEmailServiceImplTest {

    private final JetonVerificationEmailRepository jetonRepository = mock(JetonVerificationEmailRepository.class);
    private final UserService userService = mock(UserService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final VerificationEmailServiceImpl service =
            new VerificationEmailServiceImpl(jetonRepository, userService, emailService);

    private User alice;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(7L).mail("alice@test.com").firstName("Alice").emailVerifie(false).build();
        when(jetonRepository.save(any(JetonVerificationEmail.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private JetonVerificationEmail jeton(LocalDateTime expiration) {
        return JetonVerificationEmail.builder().id(1L).token("jeton").user(alice).expiration(expiration).build();
    }

    @Test
    @DisplayName("envoyerLien — ancien jeton supprimé, nouveau jeton aléatoire valable 24 h, e-mail avec ce jeton")
    void envoyerLien() {
        service.envoyerLien(alice);

        verify(jetonRepository).deleteByUserId(7L);
        ArgumentCaptor<JetonVerificationEmail> captor = ArgumentCaptor.forClass(JetonVerificationEmail.class);
        verify(jetonRepository).save(captor.capture());
        JetonVerificationEmail jeton = captor.getValue();
        assertThat(jeton.getToken()).hasSize(36);
        assertThat(jeton.getExpiration()).isCloseTo(LocalDateTime.now().plus(Duration.ofHours(24)), within(1, java.time.temporal.ChronoUnit.MINUTES));
        verify(emailService).envoyerVerificationEmail("alice@test.com", "Alice", jeton.getToken());
    }

    @Test
    @DisplayName("verifier — jeton valide : compte vérifié, jeton supprimé (usage unique), e-mail de bienvenue")
    void verifier_ok() {
        JetonVerificationEmail jeton = jeton(LocalDateTime.now().plusHours(1));
        when(jetonRepository.findByToken("jeton")).thenReturn(Optional.of(jeton));

        assertThat(service.verifier("jeton")).isEqualTo(EMAIL_VERIFIE);

        assertThat(alice.isEmailVerifie()).isTrue();
        verify(userService).save(alice);
        verify(jetonRepository).delete(jeton);
        verify(emailService).sendWelcomeMessage("alice@test.com", "Alice");
    }

    @Test
    @DisplayName("verifier — jeton inconnu ou expiré : 400 avec le même message, compte non vérifié")
    void verifier_inconnuOuExpire() {
        when(jetonRepository.findByToken("inconnu")).thenReturn(Optional.empty());
        when(jetonRepository.findByToken("jeton")).thenReturn(Optional.of(jeton(LocalDateTime.now().minusMinutes(1))));

        KupangaBusinessException inconnu = assertThrows(KupangaBusinessException.class, () -> service.verifier("inconnu"));
        KupangaBusinessException expire = assertThrows(KupangaBusinessException.class, () -> service.verifier("jeton"));

        assertThat(inconnu.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(expire.getMessage()).isEqualTo(inconnu.getMessage()).isEqualTo(LIEN_VERIFICATION_INVALIDE);
        assertThat(alice.isEmailVerifie()).isFalse();
        verify(userService, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("renvoyer — compte non vérifié : nouveau lien ; compte vérifié ou inconnu : rien ; même réponse")
    void renvoyer() {
        User verifie = User.builder().id(8L).mail("bob@test.com").emailVerifie(true).build();
        when(userService.findOptionalByMail("alice@test.com")).thenReturn(Optional.of(alice));
        when(userService.findOptionalByMail("bob@test.com")).thenReturn(Optional.of(verifie));
        when(userService.findOptionalByMail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThat(service.renvoyer("alice@test.com"))
                .isEqualTo(service.renvoyer("bob@test.com"))
                .isEqualTo(service.renvoyer("inconnu@test.com"))
                .isEqualTo(LIEN_VERIFICATION_ENVOYE);

        verify(emailService, times(1)).envoyerVerificationEmail(any(), any(), any());
        verify(emailService).envoyerVerificationEmail(eq("alice@test.com"), any(), any());
    }

    private static String eq(String valeur) {
        return org.mockito.ArgumentMatchers.eq(valeur);
    }
}
