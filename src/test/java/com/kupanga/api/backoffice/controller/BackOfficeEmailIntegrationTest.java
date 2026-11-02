package com.kupanga.api.backoffice.controller;

import com.kupanga.api.email.file.EmailEnFile;
import com.kupanga.api.email.file.EmailEnFileRepository;
import com.kupanga.api.email.file.PrioriteEmail;
import com.kupanga.api.email.file.StatutEmail;
import com.kupanga.api.email.file.TypeEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M4 : page « E-mails » du back-office, rendu Thymeleaf réel sur base réelle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — e-mails du back-office (M4)")
class BackOfficeEmailIntegrationTest {

    private static final String ADMIN = "ROLE_BACKOFFICE_ADMIN";

    @Autowired private MockMvc mockMvc;
    @Autowired private EmailEnFileRepository depot;

    private EmailEnFile email(StatutEmail statut, TypeEmail type, Instant expireLe) {
        Instant maintenant = Instant.now();
        return depot.save(EmailEnFile.builder()
                .creeLe(maintenant).type(type).priorite(type.getPriorite())
                .destinataire("jean.dupont@gmail.com").sujet("Bonjour Jean")
                .html("<p>Lien : https://kupanga.test/reset?token=SECRET</p>").texte("token=SECRET")
                .statut(statut).tentatives(statut == StatutEmail.ECHEC_DEFINITIF ? 9 : 0)
                .prochainEssai(maintenant).expireLe(expireLe)
                .derniereErreur(statut == StatutEmail.ECHEC_DEFINITIF ? "Refus SMTP 550" : null)
                .build());
    }

    @Test
    @DisplayName("Sans session admin : la page et le renvoi sont refusés (redirection vers la connexion)")
    void sansSession_refuse() throws Exception {
        mockMvc.perform(get("/backoffice/emails"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/backoffice/login"));
        mockMvc.perform(post("/backoffice/emails/1/renvoyer").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Liste : adresse masquée, aucun sujet ni contenu (jetons), erreur et bouton Renvoyer pour un échec")
    void liste_masquee() throws Exception {
        email(StatutEmail.ECHEC_DEFINITIF, TypeEmail.BIENVENUE, null);

        mockMvc.perform(get("/backoffice/emails"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("j***@gmail.com")))
                .andExpect(content().string(containsString("Refus SMTP 550")))
                .andExpect(content().string(containsString("Renvoyer")))
                .andExpect(content().string(not(containsString("jean.dupont@gmail.com"))))
                .andExpect(content().string(not(containsString("SECRET"))))
                .andExpect(content().string(not(containsString("Bonjour Jean"))));
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Renvoi sans jeton CSRF → refusé")
    void renvoi_sansCsrf() throws Exception {
        EmailEnFile echec = email(StatutEmail.ECHEC_DEFINITIF, TypeEmail.BIENVENUE, null);

        mockMvc.perform(post("/backoffice/emails/" + echec.getId() + "/renvoyer"))
                .andExpect(status().isForbidden());
        assertThat(depot.findById(echec.getId()).orElseThrow().getStatut()).isEqualTo(StatutEmail.ECHEC_DEFINITIF);
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Renvoi d'un échec → remis en file (A_ENVOYER, essais remis à zéro)")
    void renvoi_echec() throws Exception {
        EmailEnFile echec = email(StatutEmail.ECHEC_DEFINITIF, TypeEmail.QUITTANCE, null);

        mockMvc.perform(post("/backoffice/emails/" + echec.getId() + "/renvoyer").with(csrf()))
                .andExpect(redirectedUrl("/backoffice/emails"))
                .andExpect(flash().attribute("message", "E-mail remis en file : il part dans les secondes qui suivent."));

        EmailEnFile apres = depot.findById(echec.getId()).orElseThrow();
        assertThat(apres.getStatut()).isEqualTo(StatutEmail.A_ENVOYER);
        assertThat(apres.getTentatives()).isZero();
        assertThat(apres.getDerniereErreur()).isNull();
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Renvoi refusé : lien expiré, e-mail déjà envoyé, inconnu")
    void renvoi_refuse() throws Exception {
        EmailEnFile expire = email(StatutEmail.ECHEC_DEFINITIF, TypeEmail.RESET_MOT_DE_PASSE,
                Instant.now().minus(Duration.ofMinutes(1)));
        EmailEnFile envoye = email(StatutEmail.ENVOYE, TypeEmail.BIENVENUE, null);

        mockMvc.perform(post("/backoffice/emails/" + expire.getId() + "/renvoyer").with(csrf()))
                .andExpect(flash().attribute("message", "Le lien de cet e-mail a expiré : la personne doit en redemander un."));
        mockMvc.perform(post("/backoffice/emails/" + envoye.getId() + "/renvoyer").with(csrf()))
                .andExpect(flash().attribute("message", "Seul un e-mail en échec peut être renvoyé."));
        mockMvc.perform(post("/backoffice/emails/999999/renvoyer").with(csrf()))
                .andExpect(flash().attribute("message", "E-mail introuvable."));
        assertThat(depot.findById(expire.getId()).orElseThrow().getStatut()).isEqualTo(StatutEmail.ECHEC_DEFINITIF);
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Tableau de bord : compteur des e-mails en échec ; filtre par statut")
    void tableauDeBord_etFiltre() throws Exception {
        email(StatutEmail.ECHEC_DEFINITIF, TypeEmail.CONTRAT_SIGNE, null);
        email(StatutEmail.A_ENVOYER, TypeEmail.BIENVENUE, null);

        mockMvc.perform(get("/backoffice/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("E-mails en échec")));
        mockMvc.perform(get("/backoffice/emails").param("statut", "A_ENVOYER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BIENVENUE")))
                .andExpect(content().string(not(containsString("CONTRAT_SIGNE</span>"))));
        assertThat(PrioriteEmail.CRITIQUE).isEqualTo(TypeEmail.CONTRAT_SIGNE.getPriorite());
    }
}
