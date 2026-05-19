package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
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
@DisplayName("Tests d'intégration — BienSpecification")
class BienSpecificationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;

    private final BienSpecification spec = new BienSpecification();

    private User proprio;
    private User locataire;
    private Bien b1;
    private Bien b2;
    private Bien b3;

    @BeforeEach
    void setUp() {
        proprio   = userRepository.save(User.builder().mail("proprio@test.com").role(Role.ROLE_PROPRIETAIRE).build());
        locataire = userRepository.save(User.builder().mail("loc@test.com").role(Role.ROLE_LOCATAIRE).build());

        b1 = bienRepository.save(Bien.builder()
                .titre("Appartement Paris centre")
                .ville("Paris").pays("France").codePostal("75001")
                .loyerMensuel(800.0).surfaceHabitable(45.0).nombrePieces(2)
                .meuble(true).colocation(false)
                .typeBien(TypeBien.APPARTEMENT)
                .proprietaire(proprio)
                .build());

        b2 = bienRepository.save(Bien.builder()
                .titre("Maison Lyon quartier nord")
                .ville("Lyon").pays("France").codePostal("69001")
                .loyerMensuel(1200.0).surfaceHabitable(80.0).nombrePieces(4)
                .meuble(false).colocation(false)
                .typeBien(TypeBien.MAISON)
                .proprietaire(proprio)
                .build());

        // b3 est occupé — doit être exclu de toutes les recherches via build()
        b3 = bienRepository.save(Bien.builder()
                .titre("Studio Nantes loué")
                .ville("Nantes").pays("France")
                .loyerMensuel(600.0).meuble(true)
                .typeBien(TypeBien.STUDIO)
                .proprietaire(proprio).locataire(locataire)
                .build());
    }

    private BienSearchDTO emptyDto() {
        return new BienSearchDTO(
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );
    }

    @Test
    @DisplayName("sansLocataire — exclut les biens déjà occupés même sans autre filtre")
    void build_sansFiltre_exclutBiensAvecLocataire() {
        List<Bien> result = bienRepository.findAll(spec.build(emptyDto()));

        assertThat(result).extracting(Bien::getId)
                .containsExactlyInAnyOrder(b1.getId(), b2.getId())
                .doesNotContain(b3.getId());
    }

    @Test
    @DisplayName("parVilles — retourne uniquement les biens de Paris")
    void build_parVille_filtreCorrectement() {
        BienSearchDTO dto = new BienSearchDTO(
                List.of("Paris"), null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVille()).isEqualTo("Paris");
    }

    @Test
    @DisplayName("loyerMin — exclut les biens en dessous du loyer minimum")
    void build_loyerMin_filtreParLoyerMinimum() {
        BienSearchDTO dto = new BienSearchDTO(
                null, null, null, null, null,
                900.0, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoyerMensuel()).isGreaterThanOrEqualTo(900.0);
    }

    @Test
    @DisplayName("loyerMax — exclut les biens au-dessus du loyer maximum")
    void build_loyerMax_filtreParLoyerMaximum() {
        BienSearchDTO dto = new BienSearchDTO(
                null, null, null, null, null,
                null, 900.0, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoyerMensuel()).isLessThanOrEqualTo(900.0);
    }

    @Test
    @DisplayName("parTypesBien — retourne uniquement les APPARTEMENT")
    void build_parType_filtreParTypeBien() {
        BienSearchDTO dto = new BienSearchDTO(
                null, null, null, List.of(TypeBien.APPARTEMENT), null,
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeBien()).isEqualTo(TypeBien.APPARTEMENT);
    }

    @Test
    @DisplayName("parTitre — recherche partielle insensible à la casse")
    void build_parTitre_filtrePartiellement() {
        BienSearchDTO dto = new BienSearchDTO(
                null, null, null, null, "APPART",
                null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).containsIgnoringCase("appart");
    }

    @Test
    @DisplayName("parMeuble — retourne uniquement les biens meublés disponibles")
    void build_parMeuble_filtreParMeuble() {
        BienSearchDTO dto = new BienSearchDTO(
                null, null, null, null, null,
                null, null, true, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                0, 10, null, Sort.Direction.ASC
        );

        // b1 est meublé et sans locataire ; b3 est meublé mais a un locataire → exclu par sansLocataire
        List<Bien> result = bienRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMeuble()).isTrue();
    }
}
