package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.research.dto.ContratSearchDTO;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — ContratSpecification")
class ContratSpecificationTest {

    @Autowired private UserRepository    userRepository;
    @Autowired private BienRepository    bienRepository;
    @Autowired private ContratRepository contratRepository;

    private final ContratSpecification spec = new ContratSpecification();

    private User proprio1;
    private User proprio2;
    private User locataire;
    private Contrat c1;
    private Contrat c2;

    @BeforeEach
    void setUp() {
        proprio1  = userRepository.save(User.builder().mail("proprio1@test.com").role(Role.ROLE_PROPRIETAIRE).build());
        proprio2  = userRepository.save(User.builder().mail("proprio2@test.com").role(Role.ROLE_PROPRIETAIRE).build());
        locataire = userRepository.save(User.builder().mail("loc@test.com").role(Role.ROLE_LOCATAIRE).build());

        Bien bien1 = bienRepository.save(Bien.builder().titre("Bien1").proprietaire(proprio1).build());
        Bien bien2 = bienRepository.save(Bien.builder().titre("Bien2").proprietaire(proprio2).build());

        c1 = contratRepository.save(Contrat.builder()
                .proprietaire(proprio1).locataire(locataire).bien(bien1)
                .loyerMensuel(800.0).dureeBailMois(12)
                .dateDebut(LocalDate.of(2024, 1, 1))
                .statut(StatutContrat.SIGNE)
                .build());

        c2 = contratRepository.save(Contrat.builder()
                .proprietaire(proprio2).locataire(locataire).bien(bien2)
                .loyerMensuel(1500.0).dureeBailMois(24)
                .dateDebut(LocalDate.of(2025, 3, 1))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE)
                .build());
    }

    private ContratSearchDTO emptyDto() {
        return new ContratSearchDTO(null, null, null, null, null, null, null, null,
                0, 10, null, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("parUtilisateur PROPRIETAIRE — retourne les contrats dont il est proprio")
    void build_roleProprietaire_retourneSeulementSesContrats() {
        List<Contrat> result = contratRepository.findAll(spec.build(emptyDto(), proprio1.getId(), Role.ROLE_PROPRIETAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(c1.getId());
    }

    @Test
    @DisplayName("parUtilisateur LOCATAIRE — retourne tous les contrats du locataire")
    void build_roleLocataire_retourneContratsLocataire() {
        List<Contrat> result = contratRepository.findAll(spec.build(emptyDto(), locataire.getId(), Role.ROLE_LOCATAIRE));

        assertThat(result).extracting(Contrat::getId)
                .containsExactlyInAnyOrder(c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("parStatut SIGNE — retourne uniquement les contrats signés")
    void build_parStatut_filtreStatut() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, null, null, null, null, StatutContrat.SIGNE,
                0, 10, null, Sort.Direction.ASC);

        List<Contrat> result = contratRepository.findAll(spec.build(dto, locataire.getId(), Role.ROLE_LOCATAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatut()).isEqualTo(StatutContrat.SIGNE);
    }

    @Test
    @DisplayName("loyerMin — exclut les contrats avec loyer inférieur au minimum")
    void build_loyerMin_filtreParLoyerMinimum() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, 1000.0, null, null, null, null,
                0, 10, null, Sort.Direction.ASC);

        List<Contrat> result = contratRepository.findAll(spec.build(dto, locataire.getId(), Role.ROLE_LOCATAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoyerMensuel()).isGreaterThanOrEqualTo(1000.0);
    }

    @Test
    @DisplayName("dateDebutApres — retourne uniquement les contrats commençant après la date")
    void build_dateDebutApres_filtreParDate() {
        ContratSearchDTO dto = new ContratSearchDTO(null, null, null, null, null,
                LocalDate.of(2025, 1, 1), null, null,
                0, 10, null, Sort.Direction.ASC);

        List<Contrat> result = contratRepository.findAll(spec.build(dto, locataire.getId(), Role.ROLE_LOCATAIRE));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDateDebut()).isAfterOrEqualTo(LocalDate.of(2025, 1, 1));
    }
}
