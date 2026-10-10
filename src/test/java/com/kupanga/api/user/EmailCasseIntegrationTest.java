package com.kupanga.api.user;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A10 : un e-mail désigne le même compte quelle que soit sa casse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — e-mails insensibles à la casse (A10)")
class EmailCasseIntegrationTest {

    private static final String MOT_DE_PASSE = "MotDePasse123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.saveAndFlush(User.builder()
                .firstName("Alice").lastName("Martin").mail("  Alice.Martin@Casse.TEST ")
                .password(passwordEncoder.encode(MOT_DE_PASSE))
                .role(Role.ROLE_LOCATAIRE).hasCompleteProfil(true).emailVerifie(true)
                .build());
    }

    @Test
    @DisplayName("L'e-mail est stocké en minuscules, sans espaces")
    void emailStockeEnMinuscules() {
        assertThat(userRepository.findById(alice.getId()).orElseThrow().getMail()).isEqualTo("alice.martin@casse.test");
    }

    @Test
    @DisplayName("Recherche et contrôle d'existence ignorent la casse (pas de second compte « Alice.Martin@… »)")
    void rechercheEtExistence_insensiblesALaCasse() {
        assertThat(userService.getUserByEmail("ALICE.MARTIN@casse.test").getId()).isEqualTo(alice.getId());
        assertThatThrownBy(() -> userService.verifyIfUserExistWithEmail("Alice.Martin@Casse.Test"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Connexion avec une autre casse → 200")
    void login_autreCasse() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"ALICE.MARTIN@CASSE.TEST\", \"password\": \"" + MOT_DE_PASSE + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Les e-mails des conversations sont stockés en minuscules")
    void conversation_emailsEnMinuscules() {
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.builder()
                .emailExpediteur("Bob@Casse.TEST").emailDestinataire(" ALICE.martin@casse.test")
                .build());

        Conversation relue = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertThat(relue.getEmailExpediteur()).isEqualTo("bob@casse.test");
        assertThat(relue.getEmailDestinataire()).isEqualTo("alice.martin@casse.test");
    }
}
