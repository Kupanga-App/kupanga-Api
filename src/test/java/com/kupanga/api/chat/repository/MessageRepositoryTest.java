package com.kupanga.api.chat.repository;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W6 : « marquer comme lu » ne touche qu'une conversation, et seulement les messages reçus par l'utilisateur.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DisplayName("Tests d'intégration — MessageRepository (W6)")
class MessageRepositoryTest {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private TestEntityManager entityManager;

    private User alice;
    private User bob;
    private Conversation conversationBienA;
    private Message deBobSurA;
    private Message deBobSurB;
    private Message dAliceSurA;

    @BeforeEach
    void setUp() {
        bob = userRepository.save(User.builder().mail("bob@w6.test").role(Role.ROLE_PROPRIETAIRE).build());
        alice = userRepository.save(User.builder().mail("alice@w6.test").role(Role.ROLE_LOCATAIRE).build());
        Bien bienA = bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("A").typeBien(TypeBien.APPARTEMENT).proprietaire(bob).build());
        Bien bienB = bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("B").typeBien(TypeBien.MAISON).proprietaire(bob).build());

        conversationBienA = conversation(bienA);
        Conversation conversationBienB = conversation(bienB);

        // Même expéditeur (Bob) sur deux biens : seul A doit passer en lu
        deBobSurA = message(bob, alice, conversationBienA);
        deBobSurB = message(bob, alice, conversationBienB);
        // Message envoyé par Alice dans la même conversation : reçu par Bob, ne doit pas changer
        dAliceSurA = message(alice, bob, conversationBienA);
        entityManager.flush();
    }

    @Test
    @DisplayName("Seuls les messages reçus dans la conversation du bien A passent en lu")
    void marquerConversationLue_uneSeuleConversation() {
        int modifies = messageRepository.marquerConversationLue(conversationBienA.getId(), alice.getMail());
        entityManager.clear();

        assertThat(modifies).isEqualTo(1);
        assertThat(lu(deBobSurA)).isTrue();
        assertThat(lu(deBobSurB)).as("même expéditeur, autre bien").isFalse();
        assertThat(lu(dAliceSurA)).as("message envoyé par l'utilisateur connecté").isFalse();
    }

    private Conversation conversation(Bien bien) {
        return conversationRepository.save(Conversation.builder()
                .bien(bien).emailExpediteur(alice.getMail()).emailDestinataire(bob.getMail()).build());
    }

    private Message message(User expediteur, User destinataire, Conversation conversation) {
        return messageRepository.save(Message.builder()
                .contenu("Bonjour").expediteur(expediteur).destinataire(destinataire)
                .conversation(conversation).lu(false).build());
    }

    private boolean lu(Message message) {
        return Boolean.TRUE.equals(messageRepository.findById(message.getId()).orElseThrow().getLu());
    }
}
