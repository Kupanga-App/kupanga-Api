package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.mapper.ContratMapper;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.research.dto.ContratPageDTO;
import com.kupanga.api.immobilier.research.dto.ContratSearchDTO;
import com.kupanga.api.immobilier.research.specification.ContratSpecification;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — ContratSearchService")
@SuppressWarnings("unchecked")
class ContratSearchServiceTest {

    @Mock private ContratRepository    contratRepository;
    @Mock private ContratSpecification contratSpecification;
    @Mock private ContratMapper        contratMapper;
    @Mock private UserService          userService;

    @InjectMocks
    private ContratSearchService contratSearchService;

    private User proprietaire;
    private Contrat contrat;
    private ContratDTO contratDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        contrat = Contrat.builder()
                .id(1L)
                .loyerMensuel(850.0)
                .build();

        contratDTO = mock(ContratDTO.class);
    }

    @Test
    @DisplayName("rechercher() — retourne la page de contrats de l'utilisateur")
    void rechercher_returnsContratPage() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, null, null,
                null, null, null, 0, 10, null, Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(contratSpecification.build(eq(dto), eq(1L), eq(Role.ROLE_PROPRIETAIRE)))
                .thenReturn(mock(Specification.class));
        when(contratRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contrat)));
        when(contratMapper.toDTO(contrat)).thenReturn(contratDTO);

        ContratPageDTO result = contratSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("rechercher() — aucun contrat → page vide")
    void rechercher_noContrats_returnsEmptyPage() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, null, null,
                null, null, null, 0, 10, null, Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(contratSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));
        when(contratRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ContratPageDTO result = contratSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result.contenu()).isEmpty();
    }

    @Test
    @DisplayName("rechercher() — pageable transmis avec bons paramètres")
    void rechercher_buildsCorrectPageable() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, null, null,
                null, null, null, 1, 5, "dateDebut", Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(contratSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(contratRepository.findAll(any(Specification.class), captor.capture()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        contratSearchService.rechercher(dto, proprietaire.getMail());

        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(5);
    }
}
