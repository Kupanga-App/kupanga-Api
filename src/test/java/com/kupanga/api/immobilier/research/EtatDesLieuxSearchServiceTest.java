package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxPageDTO;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxSearchDTO;
import com.kupanga.api.immobilier.research.specification.EtatDesLieuxSpecification;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — EtatDesLieuxSearchService")
@SuppressWarnings("unchecked")
class EtatDesLieuxSearchServiceTest {

    @Mock private EtatDesLieuxRepository    edlRepository;
    @Mock private EtatDesLieuxSpecification edlSpecification;
    @Mock private EtatDesLieuxMapper        edlMapper;
    @Mock private UserService               userService;

    @InjectMocks
    private EtatDesLieuxSearchService edlSearchService;

    private User proprietaire;
    private EtatDesLieux edl;
    private EtatDesLieuxDTO edlDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        edl = EtatDesLieux.builder()
                .id(1L)
                .type(TypeEtat.ENTREE)
                .dateRealisation(LocalDate.now())
                .build();

        edlDTO = new EtatDesLieuxDTO();
        edlDTO.setId(1L);
    }

    @Test
    @DisplayName("rechercher() — retourne la page d'EDL de l'utilisateur")
    void rechercher_returnsEdlPage() {
        EtatDesLieuxSearchDTO dto = new EtatDesLieuxSearchDTO(null, null, null, null, null,
                0, 10, null, Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(edlSpecification.build(eq(dto), eq(1L), eq(Role.ROLE_PROPRIETAIRE)))
                .thenReturn(mock(Specification.class));
        when(edlRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(edl)));
        when(edlMapper.toDTO(edl)).thenReturn(edlDTO);

        EtatDesLieuxPageDTO result = edlSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("rechercher() — page vide quand aucun EDL")
    void rechercher_noEdl_returnsEmptyPage() {
        EtatDesLieuxSearchDTO dto = new EtatDesLieuxSearchDTO(null, null, null, null, null,
                0, 10, null, Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(edlSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));
        when(edlRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        EtatDesLieuxPageDTO result = edlSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result.contenu()).isEmpty();
    }

    @Test
    @DisplayName("rechercher() — pageable construit avec les bons paramètres")
    void rechercher_buildsCorrectPageable() {
        EtatDesLieuxSearchDTO dto = new EtatDesLieuxSearchDTO(null, null, null, null, null,
                1, 3, "dateRealisation", Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(edlSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(edlRepository.findAll(any(Specification.class), captor.capture()))
                .thenReturn(Page.empty());

        edlSearchService.rechercher(dto, proprietaire.getMail());

        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(3);
    }
}
