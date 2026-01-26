package com.kupanga.api.utilisateur.controller;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.utilisateur.service.AvatarProfilService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du contrôleur AvatarProfil")
class AvatarProfilControllerTest {

    @Mock
    private AvatarProfilService avatarProfilService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AvatarProfilController avatarProfilController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(avatarProfilController).build();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Doit créer les profils d'avatar avec succès pour un administrateur")
    void createAvatarProfil_WithAdminRole_ReturnsOk() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "images", "test.png", MediaType.IMAGE_PNG_VALUE, "test content".getBytes());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();

        when(avatarProfilService.createAvatarsProfil(anyList())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(multipart("/avatar/admin")
                        .file(file))
                .andExpect(status().isOk());

        verify(avatarProfilService).createAvatarsProfil(anyList());
    }

    @Test
    @DisplayName("Doit lever une exception si l'utilisateur n'est pas ADMIN")
    void createAvatarProfil_WithUserRole_ThrowsInvalidRoleException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "images", "test.png", MediaType.IMAGE_PNG_VALUE, "test content".getBytes());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        doReturn(Collections.singleton(authority)).when(authentication).getAuthorities();

        // Act & Assert
        // En mode standalone sans ExceptionHandler, l'exception remonte enveloppée dans
        // une ServletException
        jakarta.servlet.ServletException exception = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class, () -> mockMvc.perform(multipart("/avatar/admin").file(file)));

        assertInstanceOf(InvalidRoleException.class, exception.getCause());
        assertEquals(
                "l'utilisateur n'a pas les droits suffisants pour accéder à cette ressource. Rôle actuel de l'utilisateur : ROLE_USER",
                exception.getCause().getMessage());

        verify(avatarProfilService, never()).createAvatarsProfil(anyList());
    }
}
