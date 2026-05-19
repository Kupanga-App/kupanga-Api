package com.kupanga.api.backoffice.specification;

import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
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
@DisplayName("Tests d'intégration — UserAdminSpecification")
class UserAdminSpecificationTest {

    @Autowired private UserRepository userRepository;

    private final UserAdminSpecification spec = new UserAdminSpecification();

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder()
                .firstName("Alice").lastName("Dupont")
                .mail("alice@kupanga.com").role(Role.ROLE_PROPRIETAIRE)
                .build());

        bob = userRepository.save(User.builder()
                .firstName("Bob").lastName("Martin")
                .mail("bob@kupanga.com").role(Role.ROLE_LOCATAIRE)
                .build());
    }

    @Test
    @DisplayName("parPrenom — filtre partiel insensible à la casse")
    void build_parPrenom_filtrePrenom() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO("ali", null, null, null, 0, 10);

        List<User> result = userRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualToIgnoringCase("Alice");
    }

    @Test
    @DisplayName("parNom — filtre partiel insensible à la casse")
    void build_parNom_filtreNom() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, "mart", null, null, 0, 10);

        List<User> result = userRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualToIgnoringCase("Martin");
    }

    @Test
    @DisplayName("parMail — filtre partiel sur l'email")
    void build_parMail_filtreMail() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, "alice", null, 0, 10);

        List<User> result = userRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMail()).contains("alice");
    }

    @Test
    @DisplayName("parRole PROPRIETAIRE — retourne uniquement les propriétaires")
    void build_parRole_filtreRole() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, Role.ROLE_PROPRIETAIRE, 0, 10);

        List<User> result = userRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Role.ROLE_PROPRIETAIRE);
    }

    @Test
    @DisplayName("sans filtre — retourne tous les utilisateurs")
    void build_sansFiltre_retourneTousLesUtilisateurs() {
        UserAdminSearchDTO dto = new UserAdminSearchDTO(null, null, null, null, 0, 10);

        List<User> result = userRepository.findAll(spec.build(dto));

        assertThat(result).hasSize(2);
    }
}
