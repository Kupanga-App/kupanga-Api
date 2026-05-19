package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutQuittance;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — QuittanceSpecification")
class QuittanceSpecificationTest {

    @Autowired private UserRepository      userRepository;
    @Autowired private BienRepository      bienRepository;
    @Autowired private QuittanceRepository quittanceRepository;

    private final QuittanceSpecification spec = new QuittanceSpecification();

    private User proprio;
    private User locataire;
    private Quittance q1;
    private Quittance q2;
    private Quittance q3;

    @BeforeEach
    void setUp() {
        proprio   = userRepository.save(User.builder().mail("proprio@test.com").role(Role.ROLE_PROPRIETAIRE).build());
        locataire = userRepository.save(User.builder().mail("loc@test.com").role(Role.ROLE_LOCATAIRE).build());

        Bien bien = bienRepository.save(Bien.builder().titre("Bien Test").proprietaire(proprio).build());

        q1 = quittanceRepository.save(Quittance.builder()
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .mois("janvier").annee(2024).statut(StatutQuittance.EN_ATTENTE)
                .build());

        q2 = quittanceRepository.save(Quittance.builder()
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .mois("fevrier").annee(2024).statut(StatutQuittance.PAYEE)
                .build());

        q3 = quittanceRepository.save(Quittance.builder()
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .mois("janvier").annee(2025).statut(StatutQuittance.EN_ATTENTE)
                .build());
    }

    // Ordre du record : (annee, mois, statut, bienId, page, size, sortBy, sortDirection)
    private QuittanceSearchDTO emptyDto() {
        return new QuittanceSearchDTO(null, null, null, null, 0, 10, null, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("parUtilisateur PROPRIETAIRE — retourne ses propres quittances")
    void build_roleProprietaire_retourneQuittancesDuProprio() {
        List<Quittance> result = quittanceRepository.findAll(
                spec.build(emptyDto(), proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).extracting(Quittance::getId)
                .containsExactlyInAnyOrder(q1.getId(), q2.getId(), q3.getId());
    }

    @Test
    @DisplayName("parUtilisateur LOCATAIRE — retourne uniquement les quittances du locataire")
    void build_roleLocataire_retourneQuittancesLocataire() {
        List<Quittance> result = quittanceRepository.findAll(
                spec.build(emptyDto(), locataire.getId(), Role.ROLE_LOCATAIRE));

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("parAnnee — retourne uniquement les quittances de 2025")
    void build_parAnnee_filtreParAnnee() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(2025, null, null, null, 0, 10, null, Sort.Direction.ASC);

        List<Quittance> result = quittanceRepository.findAll(
                spec.build(dto, proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnnee()).isEqualTo(2025);
    }

    @Test
    @DisplayName("parMois — retourne uniquement les quittances de janvier")
    void build_parMois_filtreParMois() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, "janvier", null, null, 0, 10, null, Sort.Direction.ASC);

        List<Quittance> result = quittanceRepository.findAll(
                spec.build(dto, proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Quittance::getMois).containsOnly("janvier");
    }

    @Test
    @DisplayName("parStatut — retourne uniquement les quittances PAYEE")
    void build_parStatut_filtreStatut() {
        QuittanceSearchDTO dto = new QuittanceSearchDTO(null, null, StatutQuittance.PAYEE, null,
                0, 10, null, Sort.Direction.ASC);

        List<Quittance> result = quittanceRepository.findAll(
                spec.build(dto, proprio.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatut()).isEqualTo(StatutQuittance.PAYEE);
    }
}
