package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.research.dto.QuittancePageDTO;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.immobilier.research.specification.QuittanceSpecification;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — QuittanceSearchService")
@SuppressWarnings("unchecked")
class QuittanceSearchServiceTest {

    @Mock private QuittanceRepository    quittanceRepository;
    @Mock private QuittanceSpecification quittanceSpecification;
    @Mock private QuittanceMapper        quittanceMapper;
    @Mock private UserService            userService;

    @InjectMocks
    private QuittanceSearchService quittanceSearchService;

    private User proprietaire;
    private Quittance quittance;
    private QuittanceDTO quittanceDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        quittance = Quittance.builder()
                .id(1L)
                .mois("janvier")
                .annee(2025)
                .build();

        quittanceDTO = new QuittanceDTO();
        quittanceDTO.setId(1L);
    }

    // ══════════════════════════════════════════════════════════════
    // rechercher
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("rechercher() — retourne la page de quittances")
    void rechercher_returnsQuittancePage() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, null, null, null,
                0, 10, "annee", Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(quittanceSpecification.build(eq(dto), eq(1L), eq(Role.ROLE_PROPRIETAIRE)))
                .thenReturn(mock(Specification.class));
        when(quittanceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(quittance)));
        when(quittanceMapper.toDTO(quittance)).thenReturn(quittanceDTO);

        QuittancePageDTO result = quittanceSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("rechercher() — tri compound (annee + mois) quand sortBy=annee")
    void rechercher_sortByAnnee_usesCompoundSort() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, null, null, null,
                0, 10, "annee", Sort.Direction.DESC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(quittanceSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(quittanceRepository.findAll(any(Specification.class), captor.capture()))
                .thenReturn(Page.empty());

        quittanceSearchService.rechercher(dto, proprietaire.getMail());

        Sort capturedSort = captor.getValue().getSort();
        assertThat(capturedSort.getOrderFor("annee")).isNotNull();
        assertThat(capturedSort.getOrderFor("mois")).isNotNull();
        assertThat(capturedSort.getOrderFor("annee").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(capturedSort.getOrderFor("mois").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("rechercher() — tri simple quand sortBy != annee")
    void rechercher_sortByOtherField_usesSingleSort() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, null, null, null,
                0, 10, "montantTotal", Sort.Direction.ASC);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(quittanceSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(quittanceRepository.findAll(any(Specification.class), captor.capture()))
                .thenReturn(Page.empty());

        quittanceSearchService.rechercher(dto, proprietaire.getMail());

        Sort capturedSort = captor.getValue().getSort();
        assertThat(capturedSort.getOrderFor("montantTotal")).isNotNull();
        assertThat(capturedSort.getOrderFor("mois")).isNull();
    }

    @Test
    @DisplayName("rechercher() — page vide quand aucune quittance")
    void rechercher_noQuittances_returnsEmptyPage() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, null, null, null,
                0, 10, null, null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(quittanceSpecification.build(any(), anyLong(), any())).thenReturn(mock(Specification.class));
        when(quittanceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        QuittancePageDTO result = quittanceSearchService.rechercher(dto, proprietaire.getMail());

        assertThat(result.contenu()).isEmpty();
    }
}
