package com.kupanga.api.utilisateur.service;

import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import com.kupanga.api.utilisateur.mapper.AvatarProfilMapper;
import com.kupanga.api.utilisateur.repository.AvatarProfilRepository;
import com.kupanga.api.utilisateur.service.impl.AvatarProfilServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires pour AvatarProfilServiceImpl")
class AvatarProfilServiceImplTest {

    @Mock
    private AvatarProfilRepository avatarProfilRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private AvatarProfilMapper avatarProfilMapper;

    @InjectMocks
    private AvatarProfilServiceImpl avatarProfilService;

    private MockMultipartFile file1;
    private MockMultipartFile file2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        file1 = new MockMultipartFile(
                "images",
                "avatar1.png",
                "image/png",
                "content1".getBytes()
        );

        file2 = new MockMultipartFile(
                "images",
                "avatar2.jpg",
                "image/jpeg",
                "content2".getBytes()
        );
    }

    @Test
    @DisplayName("createAvatarsProfil : succès avec plusieurs fichiers")
    void testCreateAvatarsProfilSuccess() {

        // Minio (IMPORTANT)
        when(minioService.uploadImage(any(MultipartFile.class), anyString()))
                .thenReturn(
                        "http://minio/avatar1.png",
                        "http://minio/avatar2.jpg"
                );

        // Mapper
        when(avatarProfilMapper.toDTO(any(AvatarProfil.class)))
                .thenAnswer(invocation -> {
                    AvatarProfil avatar = invocation.getArgument(0);
                    return new AvatarProfilDTO(null, avatar.getUrl());
                });

        ArgumentCaptor<AvatarProfil> captor =
                ArgumentCaptor.forClass(AvatarProfil.class);

        // WHEN
        List<AvatarProfilDTO> result =
                avatarProfilService.createAvatarsProfil(List.of(file1, file2));

        // THEN
        assertEquals(2, result.size());
        assertEquals("http://minio/avatar1.png", result.get(0).url());
        assertEquals("http://minio/avatar2.jpg", result.get(1).url());

        verify(avatarProfilRepository, times(2)).save(captor.capture());

        List<AvatarProfil> saved = captor.getAllValues();
        assertEquals("http://minio/avatar1.png", saved.get(0).getUrl());
        assertEquals("http://minio/avatar2.jpg", saved.get(1).getUrl());
    }



    @Test
    @DisplayName("createAvatarsProfil : liste vide retourne liste vide")
    void testCreateAvatarsProfilEmptyList() {
        List<AvatarProfilDTO> result = avatarProfilService.createAvatarsProfil(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Vérifier qu'aucune interaction avec Minio ou repo n'a eu lieu
        verifyNoInteractions(minioService);
        verifyNoInteractions(avatarProfilRepository);
        verifyNoInteractions(avatarProfilMapper);
    }
}
