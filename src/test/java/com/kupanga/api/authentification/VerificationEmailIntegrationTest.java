package com.kupanga.api.authentification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.authentification.entity.JetonVerificationEmail;
import com.kupanga.api.authentification.repository.JetonVerificationEmailRepository;
import com.kupanga.api.authentification.service.VerificationEmailService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.kupanga.api.authentification.constant.AuthConstant.COMPTE_CREE_VERIFIER_EMAIL;
import static com.kupanga.api.authentification.constant.AuthConstant.EMAIL_NON_VERIFIE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A14 sur le contexte complet (vraie chaîne de sécurité, vraie base) : un compte non confirmé ne peut pas
 * se connecter ; le lien le débloque, une seule fois.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — confirmation de l'adresse e-mail (A14)")
class VerificationEmailIntegrationTest {

    private static final String MOT_DE_PASSE = "MotDePasse123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JetonVerificationEmailRepository jetonRepository;
    @Autowired private VerificationEmailService verificationEmailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private ResultActions inscrire(String mail, String motDePasse, String ip) throws Exception {
        return inscrire(mail, motDePasse, ip, null);
    }

    private ResultActions inscrire(String mail, String motDePasse, String ip, MockMultipartFile image) throws Exception {
        byte[] formulaire = objectMapper.writeValueAsBytes(UserFormDTO.builder()
                .firstName("Nina").lastName("Test").mail(mail)
                .password(motDePasse).role(Role.ROLE_LOCATAIRE).build());
        var requete = multipart("/auth/register")
                .file(new MockMultipartFile("userFormDTO", "", "application/json", formulaire));
        if (image != null) {
            requete.file(image);
        }
        return mockMvc.perform(requete.with(r -> { r.setRemoteAddr(ip); return r; }));
    }

    private User creerNonVerifie() {
        return userRepository.saveAndFlush(User.builder()
                .firstName("Nina").lastName("Test").mail("nouveau@a14.test")
                .password(passwordEncoder.encode(MOT_DE_PASSE))
                .role(Role.ROLE_LOCATAIRE).hasCompleteProfil(true)
                .build());
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"nouveau@a14.test\", \"password\": \"" + MOT_DE_PASSE + "\"}"));
    }

    private ResultActions verifier(String token) throws Exception {
        return mockMvc.perform(post("/auth/verifier-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\"}"));
    }

    @Test
    @DisplayName("Non confirmé → login 403 ; lien ouvert → login 200 ; lien réutilisé → 400")
    void parcoursComplet() throws Exception {
        User nouveau = creerNonVerifie();
        assertThat(nouveau.isEmailVerifie()).isFalse();

        login().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(EMAIL_NON_VERIFIE));

        verificationEmailService.envoyerLien(nouveau);
        String token = jetonRepository.findAll().get(0).getToken();

        verifier(token).andExpect(status().isOk());
        login().andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());

        verifier(token).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Inscription : 201 sans connexion ; même adresse (autre casse) → même 201, aucun second compte, mot de passe inchangé")
    void inscription_puisAdresseDejaInscrite_memeReponse() throws Exception {
        inscrire("inscrit@a14.test", "Password1", "10.14.0.1")
                .andExpect(status().isCreated())
                .andExpect(content().string(COMPTE_CREE_VERIFIER_EMAIL))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        User cree = userRepository.findByMail("inscrit@a14.test").orElseThrow();
        assertThat(cree.isEmailVerifie()).isFalse();
        assertThat(jetonRepository.findAll()).extracting(j -> j.getUser().getId()).containsExactly(cree.getId());
        String jetonInitial = jetonRepository.findAll().get(0).getToken();
        String hashInitial = cree.getPassword();

        inscrire("Inscrit@A14.test", "AutreMotDePasse2", "10.14.0.2")
                .andExpect(status().isCreated())
                .andExpect(content().string(COMPTE_CREE_VERIFIER_EMAIL))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByMail("inscrit@a14.test").orElseThrow().getPassword()).isEqualTo(hashInitial);
        // pas de nouveau lien : le premier reste le seul valable
        assertThat(jetonRepository.findAll()).extracting(JetonVerificationEmail::getToken).containsExactly(jetonInitial);
    }

    @Test
    @DisplayName("Inscription avec une fausse image : 415 que l'adresse soit libre ou déjà inscrite (pas d'énumération)")
    void inscription_imageInvalide_memeStatut() throws Exception {
        MockMultipartFile fausseImage = new MockMultipartFile("imageProfil", "photo.jpg", "image/jpeg",
                "<html>pas une image</html>".getBytes());
        creerNonVerifie();

        inscrire("libre@a14.test", "Password1", "10.14.1.1", fausseImage)
                .andExpect(status().isUnsupportedMediaType());
        inscrire("nouveau@a14.test", "Password1", "10.14.1.2", fausseImage)
                .andExpect(status().isUnsupportedMediaType());
        assertThat(userRepository.findByMail("libre@a14.test")).isEmpty();
    }

    @Test
    @DisplayName("Renvoi du lien : l'ancien lien ne marche plus (400), le nouveau oui")
    void renvoi_invalideLAncienLien() throws Exception {
        User nouveau = creerNonVerifie();
        verificationEmailService.envoyerLien(nouveau);
        String ancien = jetonRepository.findAll().get(0).getToken();

        verificationEmailService.renvoyer("nouveau@a14.test");
        String nouveauJeton = jetonRepository.findAll().get(0).getToken();
        assertThat(nouveauJeton).isNotEqualTo(ancien);

        verifier(ancien).andExpect(status().isBadRequest());
        verifier(nouveauJeton).andExpect(status().isOk());
        login().andExpect(status().isOk());
    }

    @Test
    @DisplayName("Lien expiré → 400, compte toujours non confirmé")
    void jetonExpire() throws Exception {
        User nouveau = creerNonVerifie();
        jetonRepository.saveAndFlush(JetonVerificationEmail.builder()
                .token("11111111-1111-1111-1111-111111111111").user(nouveau)
                .expiration(LocalDateTime.now().minusMinutes(1)).build());

        verifier("11111111-1111-1111-1111-111111111111").andExpect(status().isBadRequest());
        login().andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Jeton inconnu → 400, sans jeton d'accès")
    void jetonInconnu() throws Exception {
        verifier("00000000-0000-0000-0000-000000000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}
