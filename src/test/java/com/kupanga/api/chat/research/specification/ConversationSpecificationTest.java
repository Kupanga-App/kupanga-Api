package com.kupanga.api.chat.research.specification;

import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.immobilier.entity.Bien;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — ConversationSpecification")
class ConversationSpecificationTest {

    @Autowired private UserRepository         userRepository;
    @Autowired private BienRepository         bienRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository      messageRepository;

    private final ConversationSpecification spec = new ConversationSpecification();

    private User alice;
    private User bob;
    private Bien bien;
    private Conversation conv;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(User.builder().mail("alice@test.com").role(Role.ROLE_LOCATAIRE).build());
        bob   = userRepository.save(User.builder().mail("bob@test.com").role(Role.ROLE_PROPRIETAIRE).build());

        bien = bienRepository.save(Bien.builder().titre("Appart Centre-Ville").proprietaire(bob).build());

        conv = conversationRepository.save(Conversation.builder()
                .bien(bien)
                .emailExpediteur(alice.getMail())
                .emailDestinataire(bob.getMail())
                .lastMessage("Bonjour")
                .build());

        // Conversation sans rapport — entre deux autres utilisateurs
        User c = userRepository.save(User.builder().mail("charlie@test.com").role(Role.ROLE_LOCATAIRE).build());
        conversationRepository.save(Conversation.builder()
                .bien(bien)
                .emailExpediteur(c.getMail())
                .emailDestinataire(bob.getMail())
                .lastMessage("Salut")
                .build());
    }

    private ConversationSearchDTO emptyDto() {
        return new ConversationSearchDTO(null, null, 0, 10, null, Sort.Direction.DESC);
    }

    @Test
    @DisplayName("pourUtilisateur — retourne uniquement les conversations auxquelles alice participe")
    void build_pourUtilisateur_retourneConversationsParticipant() {
        List<Conversation> result = conversationRepository.findAll(spec.build(alice.getMail(), emptyDto()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmailExpediteur()).isEqualTo(alice.getMail());
    }

    @Test
    @DisplayName("parNomDuBien — filtre partiel sur le titre du bien lié")
    void build_parNomDuBien_filtreTitreBien() {
        ConversationSearchDTO dto = new ConversationSearchDTO("centre", null, 0, 10, null, Sort.Direction.DESC);

        List<Conversation> result = conversationRepository.findAll(spec.build(alice.getMail(), dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBien().getTitre()).containsIgnoringCase("centre");
    }

    @Test
    @DisplayName("parStatutLecture lu=false — retourne les conversations avec messages non lus")
    void build_parStatutLecture_filtreMsgNonLus() {
        // Ajouter un message non lu reçu par alice
        messageRepository.save(Message.builder()
                .contenu("Bonjour Alice")
                .expediteur(bob)
                .destinataire(alice)
                .conversation(conv)
                .lu(false)
                .build());

        ConversationSearchDTO dto = new ConversationSearchDTO(null, false, 0, 10, null, Sort.Direction.DESC);

        List<Conversation> result = conversationRepository.findAll(spec.build(alice.getMail(), dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(conv.getId());
    }
}
