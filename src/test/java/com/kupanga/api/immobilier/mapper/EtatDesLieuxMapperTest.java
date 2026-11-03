package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.CleRemise;
import com.kupanga.api.immobilier.entity.CompteurReleve;
import com.kupanga.api.immobilier.entity.ElementEdl;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.PieceEdl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EtatDesLieuxMapper : collections ordonnées dans la réponse JSON")
class EtatDesLieuxMapperTest {

    private final EtatDesLieuxMapper mapper = creerMapper();

    private static EtatDesLieuxMapper creerMapper() {
        EtatDesLieuxMapperImpl impl = new EtatDesLieuxMapperImpl();
        ReflectionTestUtils.setField(impl, "documentPdfUrlMapper", Mockito.mock(DocumentPdfUrlMapper.class));
        return impl;
    }

    @Test
    @DisplayName("pièces par « ordre » puis saisie (ordre absent en dernier), éléments, compteurs et clés par ordre de saisie")
    void collectionsTriees() {
        EtatDesLieux edl = EtatDesLieux.builder()
                .pieces(new HashSet<>()).compteurs(new HashSet<>()).cles(new HashSet<>()).build();
        // ordre d'itération du HashSet quelconque : seul le tri doit compter
        Object[][] pieces = {{"Sans ordre", null, 5L}, {"Cuisine", 3, 30L}, {"Entrée", 1, 10L},
                {"Séjour", 2, 20L}, {"Séjour bis", 2, 21L}};
        for (Object[] p : pieces) {
            PieceEdl piece = PieceEdl.builder().id((Long) p[2]).nomPiece((String) p[0]).ordre((Integer) p[1])
                    .etatDesLieux(edl).elements(new HashSet<>()).build();
            for (long id : new long[]{300, 100, 200}) {
                piece.getElements().add(ElementEdl.builder().id(id).description("e" + id).piece(piece).build());
            }
            edl.getPieces().add(piece);
        }
        for (long id : new long[]{3, 1, 2}) {
            edl.getCompteurs().add(CompteurReleve.builder().id(id).numeroCompteur("c" + id).etatDesLieux(edl).build());
            edl.getCles().add(CleRemise.builder().id(id).typeCle("k" + id).quantite(1).etatDesLieux(edl).build());
        }

        EtatDesLieuxDTO dto = mapper.toDTO(edl);

        assertThat(dto.getPieces()).extracting(EtatDesLieuxDTO.PieceEdlDTO::getNomPiece)
                .containsExactly("Entrée", "Séjour", "Séjour bis", "Cuisine", "Sans ordre");
        assertThat(dto.getPieces()).allSatisfy(p -> assertThat(p.getElements())
                .extracting(EtatDesLieuxDTO.ElementEdlDTO::getDescription).containsExactly("e100", "e200", "e300"));
        assertThat(dto.getCompteurs()).extracting(EtatDesLieuxDTO.CompteurReleveDTO::getNumeroCompteur)
                .containsExactly("c1", "c2", "c3");
        assertThat(dto.getCles()).extracting(EtatDesLieuxDTO.CleRemiseDTO::getTypeCle)
                .containsExactly("k1", "k2", "k3");
    }

    @Test
    @DisplayName("collections absentes → listes vides, jamais d'erreur")
    void collectionsAbsentes() {
        EtatDesLieuxDTO dto = mapper.toDTO(EtatDesLieux.builder().build());

        assertThat(dto.getPieces()).isEqualTo(List.of());
        assertThat(dto.getCompteurs()).isEqualTo(List.of());
        assertThat(dto.getCles()).isEqualTo(List.of());
    }
}
