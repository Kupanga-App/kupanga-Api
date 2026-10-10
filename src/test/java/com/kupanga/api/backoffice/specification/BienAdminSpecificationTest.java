package com.kupanga.api.backoffice.specification;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — BienAdminSpecification")
class BienAdminSpecificationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;

    private final BienAdminSpecification spec = new BienAdminSpecification();

    private Bien b1;
    private Bien b2;

    @BeforeEach
    void setUp() {
        User proprio = userRepository.save(User.builder().mail("admin@test.com").role(Role.ROLE_PROPRIETAIRE).build());

        b1 = bienRepository.save(Bien.builder()
                .titre("Appartement Nantes hypercentre")
                .ville("Nantes")
                .pays(Pays.FR).devise(Devise.EUR)
                .typeBien(TypeBien.APPARTEMENT)
                .proprietaire(proprio)
                .build());

        b2 = bienRepository.save(Bien.builder()
                .titre("Maison Lyon calme")
                .ville("Lyon")
                .pays(Pays.CD).devise(Devise.USD)
                .typeBien(TypeBien.MAISON)
                .proprietaire(proprio)
                .build());
    }

    @Test
    @DisplayName("parTitre — filtre partiel insensible à la casse")
    void build_parTitre_filtrePartiellement() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO("APPART", null, null, null, 0, 10);

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(b1.getId());
    }

    @Test
    @DisplayName("parVille — filtre partiel sur la ville")
    void build_parVille_filtreParVille() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, "lyon", null, null, 0, 10);

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVille()).isEqualToIgnoringCase("Lyon");
    }

    @Test
    @DisplayName("J1 : parPays — filtre exact sur le code ISO")
    void build_parPays_filtreParPays() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, null, Pays.CD, null, 0, 10);

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).extracting(Bien::getId).containsExactly(b2.getId());
    }

    @Test
    @DisplayName("parType MAISON — retourne uniquement les maisons")
    void build_parType_filtreParTypeBien() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, null, null, TypeBien.MAISON, 0, 10);

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeBien()).isEqualTo(TypeBien.MAISON);
    }

    @Test
    @DisplayName("sans filtre — retourne tous les biens")
    void build_sansFiltre_retourneTousLesBiens() {
        BienAdminSearchDTO dto = new BienAdminSearchDTO(null, null, null, null, 0, 10);

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(2);
    }
}
