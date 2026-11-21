package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.dto.TableauDeBordDTO;
import com.kupanga.api.backoffice.service.TableauDeBordService;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Refonte du back-office : chaque écran s'affiche avec des données réelles (rendu Thymeleaf complet),
 * les fichiers de style sont servis sans session, et le tableau de bord compte juste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — écrans du back-office")
class BackOfficePagesIntegrationTest {

    private static final String ADMIN = "ROLE_BACKOFFICE_ADMIN";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private ContratRepository contratRepository;
    @Autowired private TableauDeBordService tableauDeBordService;
    @Autowired private EntityManager entityManager;

    private Bien donnees() {
        User proprio = userRepository.save(User.builder().mail("proprio@bo.test").firstName("Paula").lastName("Proprio")
                .password("hash").role(Role.ROLE_PROPRIETAIRE).emailVerifie(true).build());
        User locataire = userRepository.save(User.builder().mail("locataire@bo.test").firstName("Léon").lastName("Loc")
                .password("hash").role(Role.ROLE_LOCATAIRE).emailVerifie(false).build());
        Bien bien = bienRepository.save(Bien.builder().pays(Pays.CD).devise(Devise.USD).titre("Villa <b>Gombe</b>")
                .ville("Kinshasa").typeBien(TypeBien.VILLA).loyerMensuel(new BigDecimal("1200"))
                .proprietaire(proprio).locataire(locataire).build());
        contratRepository.save(Contrat.builder().pays(Pays.CD).devise(Devise.USD).modeleVersion("cd-v1")
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .dateDebut(LocalDate.now()).dureeBailMois(12).loyerMensuel(new BigDecimal("1200"))
                .statut(StatutContrat.SIGNE).dateSignatureLocataire(LocalDateTime.now())
                .build());
        entityManager.flush();
        return bien;
    }

    @Test
    @DisplayName("Feuille de style, script et logo servis sans session ; la connexion les charge")
    void ressources_publiques() throws Exception {
        mockMvc.perform(get("/backoffice/assets/backoffice.css")).andExpect(status().isOk())
                .andExpect(content().string(containsString("--kp-accent")));
        mockMvc.perform(get("/backoffice/assets/backoffice.js")).andExpect(status().isOk());
        mockMvc.perform(get("/backoffice/assets/tableau-de-bord.js")).andExpect(status().isOk());
        mockMvc.perform(get("/backoffice/assets/logo-clair.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/backoffice/assets/polices/figtree-latin-400.woff2")).andExpect(status().isOk());
        mockMvc.perform(get("/backoffice/assets/lucide-0.460.0.min.js")).andExpect(status().isOk());
        // Aucune ressource tierce sur la page de connexion (un script tiers y lirait le mot de passe)
        mockMvc.perform(get("/backoffice/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/backoffice/assets/backoffice.css")))
                .andExpect(content().string(not(containsString("https://"))))
                .andExpect(content().string(not(containsString("bootstrap"))));
    }

    @Test
    @DisplayName("Ressources : seul GET est public ; une ressource absente ne donne pas une page du back-office")
    void ressources_limitees() throws Exception {
        mockMvc.perform(post("/backoffice/assets/backoffice.js")).andExpect(status().is4xxClientError());
        mockMvc.perform(get("/backoffice/assets/inexistant.js")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Fenêtres de confirmation : jeton CSRF présent ; sans jeton, la suppression est refusée")
    void confirmations_csrf() throws Exception {
        donnees();
        mockMvc.perform(get("/backoffice/users")).andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"dialog-suppression\".*name=\"_csrf\".*")));
        mockMvc.perform(get("/backoffice/biens")).andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"dialog-archivage\".*name=\"_csrf\".*")));
        mockMvc.perform(post("/backoffice/users/1/supprimer")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Noms et titres piégés échappés aussi dans les attributs data-nom")
    void attributs_echappes() throws Exception {
        User piege = userRepository.save(User.builder().mail("piege@bo.test").firstName("\"><img src=x onerror=alert(1)>")
                .lastName("X").password("hash").role(Role.ROLE_PROPRIETAIRE).emailVerifie(true).build());
        bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("\"><img src=y onerror=alert(2)>")
                .ville("Lyon").typeBien(TypeBien.MAISON).proprietaire(piege).build());
        entityManager.flush();

        for (String ecran : new String[]{"/backoffice/users", "/backoffice/biens", "/backoffice/dashboard"}) {
            mockMvc.perform(get(ecran)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("<img src=x"))))
                    .andExpect(content().string(not(containsString("<img src=y"))));
        }
    }

    @Test
    @DisplayName("Sans session, les écrans renvoient vers la connexion (les ressources ne donnent rien d'autre)")
    void ecrans_proteges() throws Exception {
        mockMvc.perform(get("/backoffice/dashboard")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/backoffice/analytics")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin@kupanga.test", authorities = ADMIN)
    @DisplayName("Chaque écran s'affiche avec des données, titres échappés")
    void ecrans_avecDonnees() throws Exception {
        Bien bien = donnees();

        mockMvc.perform(get("/backoffice/dashboard")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Inscriptions et biens publiés")))
                .andExpect(content().string(containsString("Villa &lt;b&gt;Gombe&lt;/b&gt;")))
                .andExpect(content().string(not(containsString("<b>Gombe</b>"))));
        mockMvc.perform(get("/backoffice/analytics")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Documents par bien")));
        mockMvc.perform(get("/backoffice/users")).andExpect(status().isOk())
                .andExpect(content().string(containsString("E-mail à confirmer")))
                .andExpect(content().string(containsString("dialog-suppression")));
        mockMvc.perform(get("/backoffice/users").param("page", "3")).andExpect(status().isOk());
        mockMvc.perform(get("/backoffice/biens")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Loué")))
                .andExpect(content().string(containsString("Villa")));
        mockMvc.perform(get("/backoffice/biens/{id}/documents", bien.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Signé")));
        mockMvc.perform(get("/backoffice/emails")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tableau de bord : comptes, biens loués, baux signés et historique du mois en cours")
    void tableauDeBord_compte() {
        TableauDeBordDTO avant = tableauDeBordService.calculer();
        donnees();
        TableauDeBordDTO apres = tableauDeBordService.calculer();

        assertThat(apres.comptes().valeur() - avant.comptes().valeur()).isEqualTo(2);
        assertThat(apres.comptes().nouveaux() - avant.comptes().nouveaux()).isEqualTo(2);
        assertThat(apres.comptesNonConfirmes() - avant.comptesNonConfirmes()).isEqualTo(1);
        assertThat(apres.biens().valeur() - avant.biens().valeur()).isEqualTo(1);
        assertThat(apres.biensLoues() - avant.biensLoues()).isEqualTo(1);
        assertThat(apres.location().bauxSignes30j() - avant.location().bauxSignes30j()).isEqualTo(1);

        assertThat(apres.mois()).hasSize(12);
        TableauDeBordDTO.PointMensuel dernier = apres.mois().get(11);
        assertThat(dernier.cle()).isEqualTo(YearMonth.now().toString());
        assertThat(dernier.inscriptions() - avant.mois().get(11).inscriptions()).isEqualTo(2);
        assertThat(dernier.baux() - avant.mois().get(11).baux()).isEqualTo(1);
        assertThat(apres.parPays()).anySatisfy(p -> assertThat(p.cle()).isEqualTo("CD"));
        assertThat(apres.parType()).anySatisfy(p -> assertThat(p.libelle()).isEqualTo(TypeBien.VILLA.getLibelle()));
    }

    @Test
    @DisplayName("Indicateur : évolution sur 30 jours, rien si la période précédente est vide")
    void indicateur_variation() {
        assertThat(new TableauDeBordDTO.Indicateur(10, 6, 4).variation()).isEqualTo(50);
        assertThat(new TableauDeBordDTO.Indicateur(10, 2, 4).variation()).isEqualTo(-50);
        assertThat(new TableauDeBordDTO.Indicateur(10, 3, 0).variation()).isNull();
        assertThat(new TableauDeBordDTO.Quittances(1, 1, 1).partEnRetard()).isEqualTo(34);
    }
}
