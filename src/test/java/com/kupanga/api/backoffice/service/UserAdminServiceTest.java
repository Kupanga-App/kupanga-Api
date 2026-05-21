package com.kupanga.api.backoffice.service;

import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.repository.PasswordResetTokenRepository;
import com.kupanga.api.authentification.repository.RefreshTokenRepository;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.backoffice.dto.UserAdminPageDTO;
import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.backoffice.specification.UserAdminSpecification;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — UserAdminService")
@SuppressWarnings("unchecked")
class UserAdminServiceTest {

    @Mock private UserRepository                userRepository;
    @Mock private UserAdminSpecification        userAdminSpecification;
    @Mock private RefreshTokenRepository        refreshTokenRepository;
    @Mock private PasswordResetTokenRepository  passwordResetTokenRepository;
    @Mock private NotificationRepository        notificationRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder().id(1L).mail("alice@test.com").role(Role.ROLE_PROPRIETAIRE).build();
    }

    @Test
    @DisplayName("rechercher() — retourne une page de UserAdminDTO")
    void rechercher_returnsPage() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, null, 0, 10);

        when(userAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        UserAdminPageDTO result = userAdminService.rechercher(dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("supprimer() — supprime refresh token, password reset token puis l'utilisateur")
    void supprimer_withTokens_deletesTokensThenUser() {
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(refreshToken);
        when(passwordResetTokenRepository.findByUser_Id(1L)).thenReturn(Optional.of(mock(com.kupanga.api.authentification.entity.PasswordResetToken.class)));

        userAdminService.supprimer(1L);

        InOrder order = inOrder(refreshTokenRepository, passwordResetTokenRepository, userRepository);
        order.verify(refreshTokenRepository).delete(refreshToken);
        order.verify(passwordResetTokenRepository).findByUser_Id(1L);
        order.verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("supprimer() — sans refresh token → suppression directe de l'utilisateur")
    void supprimer_noRefreshToken_deletesUserDirectly() {
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(null);
        when(passwordResetTokenRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        userAdminService.supprimer(1L);

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("countAll() — délègue count au repository")
    void countAll_delegatesToRepository() {
        when(userRepository.count()).thenReturn(12L);

        assertThat(userAdminService.countAll()).isEqualTo(12L);
    }

    @Test
    @DisplayName("countByRole() — délègue countByRole au repository")
    void countByRole_delegatesToRepository() {
        when(userRepository.countByRole(Role.ROLE_PROPRIETAIRE)).thenReturn(4L);

        assertThat(userAdminService.countByRole(Role.ROLE_PROPRIETAIRE)).isEqualTo(4L);
    }

    @Test
    @DisplayName("rechercher() — page vide → contenu vide")
    void rechercher_emptyPage_returnsEmptyContent() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, null, 0, 10);

        when(userAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        UserAdminPageDTO result = userAdminService.rechercher(dto);

        assertThat(result.contenu()).isEmpty();
    }
}
