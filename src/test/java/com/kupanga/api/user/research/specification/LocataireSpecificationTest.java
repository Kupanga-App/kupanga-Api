package com.kupanga.api.user.research.specification;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.research.dto.LocataireSearchDTO;
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
@DisplayName("Tests d'intégration — LocataireSpecification")
class LocataireSpecificationTest {

    @Autowired private UserRepository         userRepository;
    @Autowired private BienRepository         bienRepository;
    @Autowired private ConversationRepository conversationRepository;

    private final LocataireSpecification spec = new LocataireSpecification();

    private User proprio;
    private User alice;
    private User bob;
    private Bien bien;

    @BeforeEach
    void setUp() {
        proprio = userRepository.save(User.builder().mail("proprio@test.com").role(Role.ROLE_PROPRIETAIRE).build());

        alice = userRepository.save(User.builder()
                .firstName("Alice").lastName("Dupont")
                .mail("alice@test.com").role(Role.ROLE_LOCATAIRE).build());

        bob = userRepository.save(User.builder()
                .firstName("Bob").lastName("Martin")
                .mail("bob@test.com").role(Role.ROLE_LOCATAIRE).build());

        bien = bienRepository.save(Bien.builder().titre("Appart Test").proprietaire(proprio).build());

        // Alice et Bob ont chacun une conversation liée au bien
        conversationRepository.save(Conversation.builder()
                .bien(bien)
                .emailExpediteur(alice.getMail())
                .emailDestinataire(proprio.getMail())
                .build());

        conversationRepository.save(Conversation.builder()
                .bien(bien)
                .emailExpediteur(bob.getMail())
                .emailDestinataire(proprio.getMail())
                .build());
    }

    private LocataireSearchDTO emptyDto() {
        return new LocataireSearchDTO(null, null, null, 0, 10, null, Sort.Direction.ASC);
    }

    @Test
    @DisplayName("pourBien — retourne les utilisateurs ayant une conversation liée au bien")
    void build_pourBien_retourneLocatairesViaConversation() {
        List<User> result = userRepository.findAll(spec.build(bien.getId(), emptyDto()));

        assertThat(result).extracting(User::getMail)
                .containsExactlyInAnyOrder(alice.getMail(), bob.getMail());
    }

    @Test
    @DisplayName("parPrenom — filtre partiel insensible à la casse sur le prénom")
    void build_parPrenom_filtreParPrenom() {
        LocataireSearchDTO dto = new LocataireSearchDTO("ali", null, null, 0, 10, null, Sort.Direction.ASC);

        List<User> result = userRepository.findAll(spec.build(bien.getId(), dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualToIgnoringCase("Alice");
    }

    @Test
    @DisplayName("parMail — filtre partiel sur l'email du locataire")
    void build_parMail_filtreParEmail() {
        LocataireSearchDTO dto = new LocataireSearchDTO(null, null, "bob", 0, 10, null, Sort.Direction.ASC);

        List<User> result = userRepository.findAll(spec.build(bien.getId(), dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMail()).contains("bob");
    }
}
