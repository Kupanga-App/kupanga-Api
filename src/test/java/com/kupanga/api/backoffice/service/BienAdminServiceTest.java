package com.kupanga.api.backoffice.service;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.backoffice.dto.BienAdminPageDTO;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.backoffice.specification.BienAdminSpecification;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — BienAdminService")
@SuppressWarnings("unchecked")
class BienAdminServiceTest {

    @Mock private BienRepository        bienRepository;
    @Mock private ContratRepository contratRepository;
    @Mock private EtatDesLieuxRepository etatDesLieuxRepository;
    @Mock private BienAdminSpecification bienAdminSpecification;

    @InjectMocks
    private BienAdminService bienAdminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("rechercher() — retourne une page de BienAdminDTO")
    void rechercher_returnsPage() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, null, null, null, 0, 10);
        Bien bien = Bien.builder().pays(Pays.FR).id(1L).titre("Test").ville("Paris")
                .typeBien(TypeBien.APPARTEMENT).build();

        when(bienAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(bienRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bien)));

        BienAdminPageDTO result = bienAdminService.rechercher(dto);

        assertThat(result).isNotNull();
        assertThat(result.contenu()).hasSize(1);
    }

    @Test
    @DisplayName("B12 : archiver() — le bien est archivé, jamais supprimé")
    void archiver_archiveSansSupprimer() {
        Bien bien = Bien.builder().pays(Pays.FR).id(42L).build();
        when(bienRepository.findById(42L)).thenReturn(Optional.of(bien));

        assertThat(bienAdminService.archiver(42L)).isTrue();

        assertThat(bien.isArchive()).isTrue();
        assertThat(bien.getDateArchivage()).isNotNull();
        verify(bienRepository, never()).deleteById(any());
        verify(bienRepository, never()).delete(any(Bien.class));
        verify(contratRepository).expirerNonSignesDuBien(42L, StatutContrat.SIGNE, StatutContrat.EXPIRE);
        verify(etatDesLieuxRepository).expirerNonSignesDuBien(42L, StatutEdl.SIGNE, StatutEdl.EXPIRE);
    }

    @Test
    @DisplayName("B12 : archiver() — bien introuvable → false")
    void archiver_introuvable() {
        when(bienRepository.findById(42L)).thenReturn(Optional.empty());

        assertThat(bienAdminService.archiver(42L)).isFalse();
    }

    @Test
    @DisplayName("B12 : desarchiver() — remis en ligne ; refusé si le propriétaire est anonymisé")
    void desarchiver() {
        User proprio = User.builder().id(1L).build();
        Bien bien = Bien.builder().pays(Pays.FR).id(42L).proprietaire(proprio).archive(true).dateArchivage(LocalDateTime.now()).build();
        when(bienRepository.findById(42L)).thenReturn(Optional.of(bien));

        proprio.setAnonymise(true);
        assertThat(bienAdminService.desarchiver(42L)).isFalse();
        assertThat(bien.isArchive()).isTrue();

        proprio.setAnonymise(false);
        assertThat(bienAdminService.desarchiver(42L)).isTrue();
        assertThat(bien.isArchive()).isFalse();
        assertThat(bien.getDateArchivage()).isNull();
    }

    @Test
    @DisplayName("countAll() — délègue count au repository")
    void countAll_delegatesToRepository() {
        when(bienRepository.count()).thenReturn(7L);

        assertThat(bienAdminService.countAll()).isEqualTo(7L);
    }

    @Test
    @DisplayName("countDistinctVilles() — délègue countDistinctVilles au repository")
    void countDistinctVilles_delegatesToRepository() {
        when(bienRepository.countDistinctVilles()).thenReturn(3L);

        assertThat(bienAdminService.countDistinctVilles()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getBiensParVille() — construit une map ville → nombre trié")
    void getBiensParVille_returnsMap() {
        when(bienRepository.countParVille()).thenReturn(List.of(
                new Object[]{"Paris", 5L},
                new Object[]{"Lyon", 3L}
        ));

        Map<String, Long> result = bienAdminService.getBiensParVille();

        assertThat(result).containsEntry("Paris", 5L).containsEntry("Lyon", 3L);
    }

    @Test
    @DisplayName("getBiensParType() — construit une map typeBien → nombre")
    void getBiensParType_returnsMap() {
        when(bienRepository.countParType()).thenReturn(List.of(
                new Object[]{TypeBien.APPARTEMENT, 4L},
                new Object[]{TypeBien.MAISON, 2L}
        ));

        Map<String, Long> result = bienAdminService.getBiensParType();

        assertThat(result).containsEntry("APPARTEMENT", 4L).containsEntry("MAISON", 2L);
    }

    @Test
    @DisplayName("rechercher() — page vide → contenu vide")
    void rechercher_emptyPage_returnsEmptyContent() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, null, null, null, 0, 10);

        when(bienAdminSpecification.build(dto)).thenReturn(mock(Specification.class));
        when(bienRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        BienAdminPageDTO result = bienAdminService.rechercher(dto);

        assertThat(result.contenu()).isEmpty();
    }
}
