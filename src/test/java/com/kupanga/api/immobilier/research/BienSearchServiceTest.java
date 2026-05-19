package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.research.dto.BienPageDTO;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import com.kupanga.api.immobilier.research.specification.BienSpecification;
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

@DisplayName("Tests unitaires — BienSearchService")
@SuppressWarnings("unchecked")
class BienSearchServiceTest {

    @Mock private BienRepository    bienRepository;
    @Mock private BienSpecification bienSpecification;
    @Mock private BienMapper        bienMapper;

    @InjectMocks
    private BienSearchService bienSearchService;

    private Bien bien;
    private BienDTO bienDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bien = Bien.builder()
                .id(1L)
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .ville("Nantes")
                .build();

        bienDTO = BienDTO.builder()
                .id(1L)
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // rechercher
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("rechercher() — retourne page de BienDTO depuis le repository")
    void rechercher_returnsBienPage() {
        BienSearchDTO dto = new BienSearchDTO(null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC);

        Specification<Bien> spec = mock(Specification.class);
        when(bienSpecification.build(dto)).thenReturn(spec);
        when(bienRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bien)));
        when(bienMapper.toPublicDTO(bien)).thenReturn(bienDTO);

        BienPageDTO result = bienSearchService.rechercher(dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
        assertThat(result.contenu().get(0).titre()).isEqualTo("Appartement T3");
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("rechercher() — aucun résultat → page vide retournée")
    void rechercher_noResults_returnsEmptyPage() {
        BienSearchDTO dto = new BienSearchDTO(List.of("Paris"), null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC);

        when(bienSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(bienRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        BienPageDTO result = bienSearchService.rechercher(dto);

        assertThat(result.contenu()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    @DisplayName("rechercher() — pageable construit avec page/size/sort corrects")
    void rechercher_buildCorrectPageable() {
        BienSearchDTO dto = new BienSearchDTO(null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                2, 5, "loyerMensuel", Sort.Direction.DESC);

        when(bienSpecification.build(dto)).thenReturn(mock(Specification.class));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(bienRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(Page.empty());

        bienSearchService.rechercher(dto);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(2);
        assertThat(captured.getPageSize()).isEqualTo(5);
        assertThat(captured.getSort().getOrderFor("loyerMensuel").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
