package com.kupanga.api.user.research;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.research.dto.LocatairePageDTO;
import com.kupanga.api.user.research.dto.LocataireSearchDTO;
import com.kupanga.api.user.research.specification.LocataireSpecification;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — LocataireSearchService")
@SuppressWarnings("unchecked")
class LocataireSearchServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private LocataireSpecification locataireSpecification;
    @Mock private UserMapper             userMapper;
    @Mock private UserService            userService;
    @Mock private BienService            bienService;

    @InjectMocks
    private LocataireSearchService locataireSearchService;

    private User proprietaire;
    private User locataire;
    private UserDTO locataireDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        locataire = User.builder()
                .id(2L)
                .mail("locataire@test.com")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        locataireDTO = UserDTO.builder()
                .id(2L)
                .mail("locataire@test.com")
                .build();
    }

    @Test
    @DisplayName("rechercher() — proprio valide, bien lui appartient → page de locataires")
    void rechercher_validOwner_returnsLocatairePage() {
        LocataireSearchDTO dto = new LocataireSearchDTO(null, null, null,
                0, 10, null, Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.existsByIdAndProprietaireId(1L, 1L)).thenReturn(true);
        when(locataireSpecification.build(eq(1L), eq(dto))).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(locataire)));
        when(userMapper.toDTOWithoutCredentials(locataire)).thenReturn(locataireDTO);

        LocatairePageDTO result = locataireSearchService.rechercher(proprietaire.getMail(), 1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
        assertThat(result.contenu().get(0).mail()).isEqualTo("locataire@test.com");
    }

    @Test
    @DisplayName("rechercher() — bien n'appartient pas au proprio → KupangaBusinessException 401")
    void rechercher_bienNotOwned_throwsUnauthorized() {
        LocataireSearchDTO dto = new LocataireSearchDTO(null, null, null,
                0, 10, null, Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.existsByIdAndProprietaireId(1L, 1L)).thenReturn(false);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> locataireSearchService.rechercher(proprietaire.getMail(), 1L, dto));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("rechercher() — aucun locataire trouvé → page vide")
    void rechercher_noLocataires_returnsEmptyPage() {
        LocataireSearchDTO dto = new LocataireSearchDTO(null, null, null,
                0, 10, null, Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.existsByIdAndProprietaireId(1L, 1L)).thenReturn(true);
        when(locataireSpecification.build(anyLong(), any())).thenReturn(mock(Specification.class));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        LocatairePageDTO result = locataireSearchService.rechercher(proprietaire.getMail(), 1L, dto);

        assertThat(result.contenu()).isEmpty();
    }
}
