package com.kupanga.api.immobilier;

import com.jayway.jsonpath.JsonPath;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.LocationRepository;
import com.kupanga.api.immobilier.service.LocationService;
import com.kupanga.api.immobilier.pdf.ContratPdfService;
import com.kupanga.api.immobilier.validation.SignaturesDeTest;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Parcours réel de la réservation (décision du 2026-11-05), en HTTP sur base PostgreSQL : demande acceptée →
 * bien réservé (hors recherche) → bail du candidat → signature → locataire ; désistement et retrait avant
 * signature ; expiration après 14 jours. Seule la génération du PDF est simulée (pas de stockage S3 en test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Intégration — réservation d'un bien jusqu'à la signature du bail")
class LocationIntegrationTest {

    private static final String SIGNATURE =
            "{\"signatureBase64\": \"" + SignaturesDeTest.signatureValide() + "\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private ContratRepository contratRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private LocationService locationService;

    @MockBean private ContratPdfService contratPdfService;

    private User proprietaire;
    private User candidat;
    private User autre;
    private Bien bien;

    @BeforeEach
    void setUp() {
        when(contratPdfService.genererEtUploaderPdf(any())).thenReturn("contrats/test.pdf");
        proprietaire = utilisateur("proprio@location.test", Role.ROLE_PROPRIETAIRE);
        // Lot 3 : le candidat a choisi « mettre un bien en location » à l'inscription ; il peut quand même louer
        candidat = utilisateur("candidat@location.test", Role.ROLE_PROPRIETAIRE);
        autre = utilisateur("autre@location.test", Role.ROLE_LOCATAIRE);
        bien = bienRepository.save(Bien.builder()
                .titre("T2 Location").adresse("3 rue Test").ville("Lyon").codePostal("69001").pays(Pays.FR)
                .devise(Devise.EUR).typeBien(TypeBien.APPARTEMENT).loyerMensuel(new BigDecimal("700.00"))
                .chargesMensuelles(new BigDecimal("40.00")).depotGarantie(new BigDecimal("700.00"))
                .proprietaire(proprietaire).build());
        for (User u : new User[]{candidat, autre}) {
            conversationRepository.save(Conversation.builder().bien(bien)
                    .emailExpediteur(u.getMail()).emailDestinataire(proprietaire.getMail()).build());
        }
    }

    @Test
    @DisplayName("Acceptée → réservée et hors recherche ; bail du candidat signé → locataire, location ACTIVE")
    void reservationPuisSignature() throws Exception {
        long locationId = reserver(candidat);

        // Pas encore locataire ; annonce retirée de la recherche
        assertThat(bienRepository.findById(bien.getId()).orElseThrow().getLocataire()).isNull();
        assertThat(dansLaRecherche()).isFalse();
        // Le propriétaire voit la réservation ; un autre compte non
        assertThat(json(get("/biens/" + bien.getId() + "/location").with(connecte(proprietaire)), 200, "$.statut"))
                .isEqualTo("RESERVEE");
        assertStatut(403, get("/biens/" + bien.getId() + "/location").with(connecte(autre)));
        assertThat(json(get("/users/biens/" + bien.getId()).with(connecte(proprietaire)), 200,
                "$.reservation.locataire.mail")).isEqualTo(candidat.getMail());
        // Le candidat voit sa réservation ; aucune nouvelle demande sur le bien
        assertThat(json(get("/locations/reservations").with(connecte(candidat)), 200, "$[0].id").toString())
                .isEqualTo(String.valueOf(locationId));
        assertStatut(409, post("/biens/" + bien.getId() + "/demandes-location/" + autre.getId()).with(connecte(proprietaire)));
        // Bail : seulement pour le candidat qui a réservé
        assertStatut(400, post("/contrats").with(connecte(proprietaire)).contentType(MediaType.APPLICATION_JSON)
                .content(bailJson(autre)));
        Contrat bail = creerEtSignerParLeProprietaire();

        assertStatut(204, post("/contrats/signer/" + bail.getTokenSignature()).contentType(MediaType.APPLICATION_JSON)
                .content(SIGNATURE));

        assertThat(bienRepository.findById(bien.getId()).orElseThrow().getLocataire().getId()).isEqualTo(candidat.getId());
        Location location = locationRepository.findById(locationId).orElseThrow();
        assertThat(location.getStatut()).isEqualTo(StatutLocation.ACTIVE);
        assertThat(location.getDateDebut()).isNotNull();
        // Devenu locataire : tableau de bord de son logement
        assertStatut(200, get("/locataire/dashboard/" + bien.getId()).with(connecte(candidat)));
        // Bail signé : plus de désistement ni de retrait
        assertStatut(409, post("/locations/" + locationId + "/desister").with(connecte(candidat)));
        assertStatut(409, post("/locations/" + locationId + "/annuler").with(connecte(proprietaire)));
    }

    @Test
    @DisplayName("Fin de bail : congé du locataire, fin enregistrée avec retenue motivée, bien libéré, documents gardés")
    void finDeBail() throws Exception {
        long locationId = reserver(candidat);
        Contrat bail = creerEtSignerParLeProprietaire();
        assertStatut(204, post("/contrats/signer/" + bail.getTokenSignature()).contentType(MediaType.APPLICATION_JSON)
                .content(SIGNATURE));

        // Vue privée : location en cours visible du locataire, avec le dépôt du bail
        assertThat(json(get("/users/biens/" + bien.getId()).with(connecte(candidat)), 200, "$.location.id").toString())
                .isEqualTo(String.valueOf(locationId));
        // Fin impossible sans congé ; congé d'un autre compte → 404
        assertStatut(409, post("/locations/" + locationId + "/terminer").with(connecte(proprietaire))
                .contentType(MediaType.APPLICATION_JSON).content("{\"depotRestitue\": 700}"));
        assertStatut(404, post("/locations/" + locationId + "/conge").with(connecte(autre))
                .contentType(MediaType.APPLICATION_JSON).content("{\"dateFin\": \"" + LocalDate.now() + "\"}"));
        // Le locataire donne congé pour aujourd'hui
        assertThat(json(post("/locations/" + locationId + "/conge").with(connecte(candidat))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"dateFin\": \"" + LocalDate.now() + "\"}"),
                200, "$.congeDonnePar")).isEqualTo("LOCATAIRE");
        // Le locataire ne termine pas lui-même ; le propriétaire ne retire pas le congé du locataire
        assertStatut(404, post("/locations/" + locationId + "/terminer").with(connecte(candidat))
                .contentType(MediaType.APPLICATION_JSON).content("{\"depotRestitue\": 700}"));
        assertStatut(409, delete("/locations/" + locationId + "/conge").with(connecte(proprietaire)));
        // Retenue sans motif, puis montant supérieur au dépôt → 400
        assertStatut(400, post("/locations/" + locationId + "/terminer").with(connecte(proprietaire))
                .contentType(MediaType.APPLICATION_JSON).content("{\"depotRestitue\": 500}"));
        assertStatut(400, post("/locations/" + locationId + "/terminer").with(connecte(proprietaire))
                .contentType(MediaType.APPLICATION_JSON).content("{\"depotRestitue\": 701}"));

        assertThat(json(post("/locations/" + locationId + "/terminer").with(connecte(proprietaire))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depotRestitue\": 500, \"motifRetenue\": \"Peinture de la chambre\"}"),
                200, "$.statut")).isEqualTo("TERMINEE");

        assertThat(bienRepository.findById(bien.getId()).orElseThrow().getLocataire()).isNull();
        assertThat(dansLaRecherche()).isTrue();
        // L'ancien locataire n'a plus la vue privée ni le tableau de bord, mais garde son bail signé
        assertStatut(403, get("/users/biens/" + bien.getId()).with(connecte(candidat)));
        assertStatut(403, get("/locataire/dashboard/" + bien.getId()).with(connecte(candidat)));
        assertThat(json(post("/contrats/search").with(connecte(candidat)).contentType(MediaType.APPLICATION_JSON)
                .content("{}"), 200, "$.contenu[0].statut")).isEqualTo("SIGNE");
        // Le bien peut être proposé à quelqu'un d'autre
        reserver(autre);
    }

    @Test
    @DisplayName("Désistement avant signature : bail annulé et lien mort, bien de nouveau en ligne et proposable")
    void desistement() throws Exception {
        long locationId = reserver(candidat);
        Contrat bail = creerEtSignerParLeProprietaire();
        String lien = bail.getTokenSignature();

        // Seul le candidat se désiste ; seul le propriétaire retire
        assertStatut(404, post("/locations/" + locationId + "/desister").with(connecte(autre)));
        assertStatut(404, post("/locations/" + locationId + "/annuler").with(connecte(autre)));
        assertStatut(404, post("/locations/999999/annuler").with(connecte(proprietaire)));
        assertThat(json(get("/locations/reservations").with(connecte(autre)), 200, "$.length()").toString())
                .isEqualTo("0");
        assertThat(json(post("/locations/" + locationId + "/desister").with(connecte(candidat)), 200, "$.statut"))
                .isEqualTo("DESISTEE");

        assertThat(contratRepository.findById(bail.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.ANNULE);
        assertStatut(404, post("/contrats/signer/" + lien).contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertThat(bienRepository.findById(bien.getId()).orElseThrow().getLocataire()).isNull();
        assertThat(dansLaRecherche()).isTrue();
        assertStatut(204, get("/biens/" + bien.getId() + "/location").with(connecte(proprietaire)));
        // Le propriétaire peut proposer le bien à quelqu'un d'autre
        reserver(autre);
    }

    @Test
    @DisplayName("Retrait par le propriétaire avant signature : réservation ANNULEE, bail annulé")
    void retraitParLeProprietaire() throws Exception {
        long locationId = reserver(candidat);
        Contrat bail = creerEtSignerParLeProprietaire();

        assertThat(json(post("/locations/" + locationId + "/annuler").with(connecte(proprietaire)), 200, "$.statut"))
                .isEqualTo("ANNULEE");

        assertThat(contratRepository.findById(bail.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.ANNULE);
        assertThat(dansLaRecherche()).isTrue();
    }

    @Test
    @DisplayName("14 jours sans bail signé : la réservation expire, le bail ne se signe plus, le bien revient en ligne")
    void expiration() throws Exception {
        long locationId = reserver(candidat);
        Contrat bail = creerEtSignerParLeProprietaire();
        String lien = bail.getTokenSignature();
        // Échéance dépassée (le lien de signature, lui, est encore valable)
        Location location = locationRepository.findById(locationId).orElseThrow();
        Location echue = Location.builder().bien(location.getBien()).proprietaire(location.getProprietaire())
                .locataire(location.getLocataire()).statut(StatutLocation.RESERVEE)
                .dateReservation(LocalDateTime.now().minusDays(15)).dateExpiration(LocalDateTime.now().minusMinutes(1))
                .build();
        locationRepository.delete(location);
        locationRepository.flush();
        echue = locationRepository.saveAndFlush(echue);

        // Avant la tâche planifiée : statut affiché déjà juste, signature refusée, bien de nouveau en ligne
        assertStatut(409, post("/contrats/signer/" + lien).contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertThat(dansLaRecherche()).isTrue();

        assertThat(locationService.expirerReservationsEchues()).isEqualTo(1);

        assertThat(locationRepository.findById(echue.getId()).orElseThrow().getStatut()).isEqualTo(StatutLocation.EXPIREE);
        assertThat(contratRepository.findById(bail.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.ANNULE);
        assertThat(bienRepository.findById(bien.getId()).orElseThrow().getLocataire()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Demande envoyée par le propriétaire puis acceptée par le compte ; renvoie l'id de la réservation. */
    private long reserver(User qui) throws Exception {
        Object demandeId = json(post("/biens/" + bien.getId() + "/demandes-location/" + qui.getId())
                .with(connecte(proprietaire)), 201, "$.id");
        assertStatut(200, post("/demandes-location/" + demandeId + "/accepter").with(connecte(qui)));
        return locationRepository.findReservationValable(bien.getId(), LocalDateTime.now()).orElseThrow().getId();
    }

    private Contrat creerEtSignerParLeProprietaire() throws Exception {
        var resultat = mockMvc.perform(post("/contrats").with(connecte(proprietaire))
                .contentType(MediaType.APPLICATION_JSON).content(bailJson(candidat))).andReturn();
        assertThat(resultat.getResponse().getStatus()).as(resultat.getResponse().getContentAsString()).isEqualTo(204);
        Contrat bail = contratRepository.findByBienId(bien.getId()).get(0);
        assertStatut(204, post("/contrats/" + bail.getId() + "/signer-proprio").with(connecte(proprietaire))
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        return contratRepository.findById(bail.getId()).orElseThrow();
    }

    private boolean dansLaRecherche() throws Exception {
        String corps = mockMvc.perform(post("/biens/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getContentAsString();
        return !JsonPath.parse(corps).read("$.contenu[?(@.id == " + bien.getId() + ")]", java.util.List.class).isEmpty();
    }

    private String bailJson(User locataire) {
        return """
                {"bienId": %d, "emailLocataire": "%s", "dateDebut": "%s", "dateFin": "%s",
                 "dureeBailMois": 12, "loyerMensuel": 700.0, "chargesMensuelles": 40.0, "depotGarantie": 700.0}
                """.formatted(bien.getId(), locataire.getMail(), LocalDate.now().plusDays(1), LocalDate.now().plusYears(1));
    }

    private User utilisateur(String mail, Role role) {
        return userRepository.save(User.builder().firstName("Test").lastName("Location").mail(mail)
                .password("{noop}inutilise").role(role).hasCompleteProfil(true).emailVerifie(true).build());
    }

    private RequestPostProcessor connecte(User user) {
        return SecurityMockMvcRequestPostProcessors.user(user.getMail())
                .authorities(new SimpleGrantedAuthority(user.getRole().name()));
    }

    private Object json(MockHttpServletRequestBuilder requete, int attendu, String chemin) throws Exception {
        var resultat = mockMvc.perform(requete).andReturn();
        String corps = resultat.getResponse().getContentAsString();
        assertThat(resultat.getResponse().getStatus()).as(resultat.getRequest().getRequestURI() + " → " + corps)
                .isEqualTo(attendu);
        return JsonPath.parse(corps).read(chemin);
    }

    private void assertStatut(int attendu, MockHttpServletRequestBuilder requete) throws Exception {
        var resultat = mockMvc.perform(requete).andReturn();
        assertThat(resultat.getResponse().getStatus())
                .as(resultat.getRequest().getMethod() + " " + resultat.getRequest().getRequestURI()
                        + " → " + resultat.getResponse().getContentAsString())
                .isEqualTo(attendu);
    }
}
