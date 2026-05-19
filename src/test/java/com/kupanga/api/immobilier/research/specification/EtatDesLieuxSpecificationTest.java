package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxSearchDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — EtatDesLieuxSpecification")
class EtatDesLieuxSpecificationTest {

    @Autowired private UserRepository        userRepository;
    @Autowired private BienRepository        bienRepository;
    @Autowired private EtatDesLieuxRepository edlRepository;

    private final EtatDesLieuxSpecification spec = new EtatDesLieuxSpecification();

    private User proprio;
    private User locataire;
    private EtatDesLieux e1;
    private EtatDesLieux e2;

    @BeforeEach
    void setUp() {
        proprio   = userRepository.save(User.builder().mail("proprio@test.com").role(Role.ROLE_PROPRIETAIRE).build());
        locataire = userRepository.save(User.builder().mail("loc@test.com").role(Role.ROLE_LOCATAIRE).build());

        Bien bien = bienRepository.save(Bien.builder().titre("Bien Test").proprietaire(proprio).build());

        e1 = edlRepository.save(EtatDesLieux.builder()
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .type(TypeEtat.ENTREE)
                .statut(StatutEdl.SIGNE)
                .dateRealisation(LocalDate.of(2024, 3, 15))
                .build());

        e2 = edlRepository.save(EtatDesLieux.builder()
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .type(TypeEtat.SORTIE)
                .statut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE)
                .dateRealisation(LocalDate.of(2024, 6, 20))
                .build());
    }

    private EtatDesLieuxSearchDTO emptyDto() {
        return new EtatDesLieuxSearchDTO(null, null, null, null, null, 0, 10, null, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("parUtilisateur PROPRIETAIRE — retourne les EDL du proprio")
    void build_roleProprietaire_retourneEdlDuProprio() {
        List<EtatDesLieux> result = edlRepository.findAll(
                spec.build(emptyDto(), proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).extracting(EtatDesLieux::getId)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    @DisplayName("parType ENTREE — retourne uniquement les EDL d'entrée")
    void build_parType_filtreTypeEntree() {
        EtatDesLieuxSearchDTO dto = new EtatDesLieuxSearchDTO(TypeEtat.ENTREE, null, null, null, null,
                0, 10, null, Sort.Direction.ASC);

        List<EtatDesLieux> result = edlRepository.findAll(
                spec.build(dto, proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(TypeEtat.ENTREE);
    }

    @Test
    @DisplayName("parStatut SIGNE — retourne uniquement les EDL signés")
    void build_parStatut_filtreStatutSigne() {
        EtatDesLieuxSearchDTO dto = new EtatDesLieuxSearchDTO(null, StatutEdl.SIGNE, null, null, null,
                0, 10, null, Sort.Direction.ASC);

        List<EtatDesLieux> result = edlRepository.findAll(
                spec.build(dto, proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatut()).isEqualTo(StatutEdl.SIGNE);
    }
}
