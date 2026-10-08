package com.kupanga.api.securite;

import com.kupanga.api.authentification.utils.JwtUtils;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.notification.entity.Notification;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * TESTS-SECU : tests d'intégration de sécurité sur le contexte complet (vraie chaîne de filtres,
 * vrais services, base de test).
 * <ol>
 *   <li>Toute route REST non publique renvoie le 401 de la sécurité sans token (routes découvertes
 *       automatiquement : une nouvelle route oubliée dans {@code SecurityConfig} fait échouer le test).</li>
 *   <li>Un propriétaire ou un locataire qui vise la ressource d'un autre reçoit le statut exact attendu
 *       (403, ou 404 quand le service ne doit pas révéler l'existence) et rien n'est modifié.</li>
 *   <li>Témoins : le propriétaire et le locataire légitimes accèdent à leurs ressources.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Schéma généré par Hibernate, comme les @DataJpaTest du projet : indépendant de l'état de la base
// (les @DataJpaTest en create-drop suppriment les tables en fin de run sans toucher à l'historique Flyway).
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — sécurité de l'API (401 / 403)")
class SecuriteApiIntegrationTest {

    /**
     * Routes publiques (méthode + motif). Doit rester alignée sur la liste {@code permitAll} de
     * {@code SecurityConfig} : chacune est vérifiée accessible sans le 401 de la sécurité.
     */
    private static final Set<String> ROUTES_PUBLIQUES = Set.of(
            "POST /auth/login", "POST /auth/register", "POST /auth/google", "POST /auth/refresh",
            "POST /auth/forgot-password", "POST /auth/reset-password", "POST /auth/logout",
            "GET /biens/{bienId}", "POST /biens/search",
            "GET /contrats/signer/{token}", "POST /contrats/signer/{token}",
            "GET /etats-des-lieux/signer/{token}", "POST /etats-des-lieux/signer/{token}",
            "GET /health"
    );

    private static final String SIGNATURE = "{\"signatureBase64\": \"data:image/png;base64," + "A".repeat(120) + "\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping handlerMapping;
    @Autowired private JwtUtils jwtUtils;
    @Value("${jwt.secret-key}") private String cleJwt;

    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private ContratRepository contratRepository;
    @Autowired private QuittanceRepository quittanceRepository;
    @Autowired private EtatDesLieuxRepository etatDesLieuxRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ConversationRepository conversationRepository;

    // A et son locataire sont les victimes ; B et l'autre locataire sont les « intrus »
    private User proprietaireA;
    private User proprietaireB;
    private User locataireA;
    private User autreLocataire;
    private Bien bienA;
    private Bien bienB;
    private Bien autreBienA;
    private Contrat contratA;
    private Contrat contratAutreBienA;
    private Quittance quittanceA;
    private EtatDesLieux edlA;
    private Notification notificationA;

    @BeforeEach
    void setUp() {
        proprietaireA = utilisateur("proprio-a@secu.test", Role.ROLE_PROPRIETAIRE);
        proprietaireB = utilisateur("proprio-b@secu.test", Role.ROLE_PROPRIETAIRE);
        locataireA = utilisateur("locataire-a@secu.test", Role.ROLE_LOCATAIRE);
        autreLocataire = utilisateur("locataire-b@secu.test", Role.ROLE_LOCATAIRE);

        // Volontairement sans photo : un bien sans image doit rester consultable (régression findWithAllProperties)
        bienA = bien("Appartement A", proprietaireA, locataireA);
        autreBienA = bien("Studio A", proprietaireA, locataireA);
        bienB = bien("Maison B", proprietaireB, null);

        contratA = contrat(bienA);
        contratAutreBienA = contrat(autreBienA);

        quittanceA = quittanceRepository.save(Quittance.builder()
                .bien(bienA).proprietaire(proprietaireA).locataire(locataireA).contrat(contratA)
                .mois("JANVIER").annee(2026).loyerMensuel(800.0).chargesMensuelles(50.0).montantTotal(850.0)
                .statut(StatutQuittance.EN_ATTENTE)
                .build());

        edlA = etatDesLieuxRepository.save(EtatDesLieux.builder()
                .bien(bienA).proprietaire(proprietaireA).locataire(locataireA)
                .type(TypeEtat.ENTREE).dateRealisation(LocalDate.now())
                .statut(StatutEdl.EN_ATTENTE_SIGNATURE_PROPRIO)
                .build());

        notificationA = notificationRepository.save(Notification.builder()
                .destinataire(proprietaireA).type(NotificationType.CONTRAT_SIGNE)
                .titre("Contrat signé").message("Le contrat est signé")
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. 401 de la sécurité sans token sur toute route non publique
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Toute route REST non publique renvoie le 401 de la sécurité sans token (découverte automatique)")
    void routesProtegees_sansToken_401() throws Exception {
        List<String> routesProtegees = new ArrayList<>();
        List<String> echecs = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entree : routesKupanga().entrySet()) {
            Set<RequestMethod> methodes = entree.getKey().getMethodsCondition().getMethods();
            for (String motif : entree.getKey().getPatternValues()) {
                if (methodes.isEmpty()) {
                    // @RequestMapping sans méthode : accepte tout, à déclarer explicitement
                    echecs.add("Route sans méthode HTTP explicite : " + motif);
                    continue;
                }
                for (RequestMethod methode : methodes) {
                    String route = methode.name() + " " + motif;
                    if (ROUTES_PUBLIQUES.contains(route)) continue;
                    routesProtegees.add(route);

                    MockHttpServletResponse reponse = appeler(methode, motif);
                    // 401 de la sécurité = corps vide (un 401 métier porterait un ApiErrorResponse)
                    if (reponse.getStatus() != 401 || !reponse.getContentAsString().isEmpty()) {
                        echecs.add(route + " → " + reponse.getStatus() + " " + reponse.getContentAsString());
                    }
                }
            }
        }

        // Garde-fou : la découverte a bien trouvé les routes (sinon le test ne prouverait rien)
        assertThat(routesProtegees).hasSizeGreaterThan(20).contains("GET /auth/me", "POST /contrats", "GET /notifications");
        assertThat(echecs).as("Routes protégées accessibles sans token").isEmpty();
    }

    @Test
    @DisplayName("Chaque route publique existe et n'est pas bloquée par la sécurité")
    void routesPubliques_existentEtNeSontPasBloquees() throws Exception {
        Set<String> routesExistantes = new java.util.HashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entree : routesKupanga().entrySet()) {
            for (RequestMethod methode : entree.getKey().getMethodsCondition().getMethods()) {
                for (String motif : entree.getKey().getPatternValues()) {
                    String route = methode.name() + " " + motif;
                    if (!ROUTES_PUBLIQUES.contains(route)) continue;
                    routesExistantes.add(route);
                    MockHttpServletResponse reponse = appeler(methode, motif);
                    // Un éventuel 401 doit venir du métier (corps JSON), pas de la sécurité (corps vide)
                    assertThat(reponse.getStatus() == 401 && reponse.getContentAsString().isEmpty())
                            .as(route + " bloquée par la sécurité").isFalse();
                    assertThat(reponse.getStatus()).as(route).isNotEqualTo(403);
                }
            }
        }
        assertThat(routesExistantes).as("Routes publiques déclarées mais inexistantes").isEqualTo(ROUTES_PUBLIQUES);
    }

    @Test
    @DisplayName("Bien sans photo : consultable en public (200), sans e-mail du propriétaire ni locataire (P0-6)")
    void vuePubliqueBien_sansPhoto_sansDonneePersonnelle() throws Exception {
        assertThat(statut(get("/biens/" + bienA.getId()))).isEqualTo(200);
        mockMvc.perform(get("/biens/" + bienA.getId()))
                .andExpect(jsonPath("$.proprietaire.mail").doesNotExist())
                .andExpect(jsonPath("$.locataire").doesNotExist())
                .andExpect(jsonPath("$.contrats").doesNotExist())
                .andExpect(jsonPath("$.quittances").doesNotExist());
        // Token de signature inconnu : refus métier (corps JSON), pas le 401 vide de la sécurité
        mockMvc.perform(get("/contrats/signer/token-inconnu")).andExpect(jsonPath("$.message").value("Token Invalide"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Vrai JWT (JwtFilter)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JWT réel : valide → 200 ; mal signé, expiré ou illisible → 401")
    void jwtReel() throws Exception {
        String valide = jwtUtils.generateAccessToken(proprietaireA.getMail(), Role.ROLE_PROPRIETAIRE.name());
        String malSigne = Jwts.builder().setSubject(proprietaireA.getMail()).claim("role", "ROLE_PROPRIETAIRE")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.secretKeyFor(SignatureAlgorithm.HS256)).compact();

        assertThat(statut(get("/users/biens/" + bienA.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + valide)))
                .isEqualTo(200);
        assertThat(statut(get("/users/biens/" + bienA.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + malSigne)))
                .isEqualTo(401);
        assertThat(statut(get("/users/biens/" + bienA.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer pas-un-jwt")))
                .isEqualTo(401);

        // Bien signé avec la vraie clé, mais expiré
        String expire = Jwts.builder().setSubject(proprietaireA.getMail()).claim("role", "ROLE_PROPRIETAIRE")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(cleJwt)), SignatureAlgorithm.HS256).compact();
        assertThat(statut(get("/users/biens/" + bienA.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + expire)))
                .isEqualTo(401);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Ressources d'un autre utilisateur : statut exact, rien n'est modifié
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Propriétaire B sur les ressources du propriétaire A : 403 (404 pour la notification), rien n'est modifié")
    void autreProprietaire_ressourcesDeA_refuse() throws Exception {
        RequestPostProcessor b = connecte(proprietaireB);

        assertStatut(403, patch("/biens/" + bienA.getId()).with(b)
                .contentType(MediaType.APPLICATION_JSON).content("{\"typeBien\": \"STUDIO\", \"titre\": \"Piraté\"}"));
        assertStatut(403, post("/biens/" + bienA.getId() + "/assigne-locataire/" + autreLocataire.getId()).with(b));
        assertStatut(403, get("/users/biens/" + bienA.getId()).with(b));
        assertStatut(403, post("/users/" + bienA.getId() + "/recherche-locataire").with(b)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
        assertStatut(403, post("/contrats/" + contratA.getId() + "/signer-proprio").with(b)
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertStatut(403, post("/etats-des-lieux/" + edlA.getId() + "/signer-proprietaire").with(b)
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertStatut(403, post("/quittances/" + quittanceA.getId() + "/marquer-payee").with(b)
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertStatut(403, get("/quittances/bien/" + bienA.getId()).with(b));
        assertStatut(403, get("/quittances/" + quittanceA.getId()).with(b));
        // Notification d'autrui : 404, le service ne révèle pas qu'elle existe
        assertStatut(404, patch("/notifications/" + notificationA.getId() + "/lire").with(b));

        // Rien n'a bougé côté A
        Bien bienApres = bienRepository.findById(bienA.getId()).orElseThrow();
        assertThat(bienApres.getTitre()).isEqualTo("Appartement A");
        assertThat(bienApres.getLocataire().getId()).isEqualTo(locataireA.getId());
        assertThat(contratRepository.findById(contratA.getId())).get()
                .extracting(Contrat::getStatut).isEqualTo(StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO);
        assertThat(etatDesLieuxRepository.findById(edlA.getId())).get()
                .extracting(EtatDesLieux::getStatut).isEqualTo(StatutEdl.EN_ATTENTE_SIGNATURE_PROPRIO);
        assertThat(quittanceRepository.findById(quittanceA.getId())).get()
                .extracting(Quittance::getStatut).isEqualTo(StatutQuittance.EN_ATTENTE);
        assertThat(notificationRepository.findById(notificationA.getId())).get()
                .extracting(Notification::isLue).isEqualTo(false);
    }

    @Test
    @DisplayName("Propriétaire B : création de contrat, EDL ou quittance sur le bien de A → 403, aucune création (P0-5)")
    void autreProprietaire_creationSurBienDeA_refuse() throws Exception {
        RequestPostProcessor b = connecte(proprietaireB);
        long contrats = contratRepository.count();
        long edls = etatDesLieuxRepository.count();
        long quittances = quittanceRepository.count();

        assertStatut(403, post("/contrats").with(b).contentType(MediaType.APPLICATION_JSON).content(contratJson(bienA)));
        assertStatut(403, post("/etats-des-lieux").with(b).contentType(MediaType.APPLICATION_JSON).content(edlJson(bienA)));
        assertStatut(403, post("/quittances").with(b).contentType(MediaType.APPLICATION_JSON)
                .content(quittanceJson(bienA, contratA.getId())));

        assertThat(contratRepository.count()).isEqualTo(contrats);
        assertThat(etatDesLieuxRepository.count()).isEqualTo(edls);
        assertThat(quittanceRepository.count()).isEqualTo(quittances);
    }

    @Test
    @DisplayName("Propriétaire A : quittance rattachée au contrat d'un autre bien → 400, aucune création (P0-5)")
    void quittanceAvecContratDunAutreBien_refusee() throws Exception {
        long quittances = quittanceRepository.count();

        assertStatut(400, post("/quittances").with(connecte(proprietaireA)).contentType(MediaType.APPLICATION_JSON)
                .content(quittanceJson(bienA, contratAutreBienA.getId())));

        assertThat(quittanceRepository.count()).isEqualTo(quittances);
    }

    @Test
    @DisplayName("Propriétaire B : assigner à SON bien un locataire sans conversation avec lui → 404, rien d'assigné")
    void assignation_locataireNonCandidat_refusee() throws Exception {
        RequestPostProcessor b = connecte(proprietaireB);

        assertStatut(404, post("/biens/" + bienB.getId() + "/assigne-locataire/" + locataireA.getId()).with(b));
        // Conversation avec B sur un AUTRE bien que celui visé : ne rend pas candidat (filtre bienId)
        Bien autreBienB = bien("Garage B", proprietaireB, null);
        conversationRepository.save(Conversation.builder()
                .bien(autreBienB).emailExpediteur(locataireA.getMail()).emailDestinataire(proprietaireB.getMail())
                .build());
        assertStatut(404, post("/biens/" + bienB.getId() + "/assigne-locataire/" + locataireA.getId()).with(b));
        // Id inconnu : même réponse (pas d'énumération des comptes)
        assertStatut(404, post("/biens/" + bienB.getId() + "/assigne-locataire/999999").with(b));

        assertThat(bienRepository.findById(bienB.getId()).orElseThrow().getLocataire()).isNull();
        // L'e-mail du locataire A n'est pas apparu dans le bien de B
        mockMvc.perform(get("/users/biens/" + bienB.getId()).with(b))
                .andExpect(jsonPath("$.locataire").doesNotExist());
    }

    @Test
    @DisplayName("Témoin : un candidat (conversation sur le bien) peut être assigné")
    void assignation_candidat_acceptee() throws Exception {
        conversationRepository.save(Conversation.builder()
                .bien(bienB).emailExpediteur(autreLocataire.getMail()).emailDestinataire(proprietaireB.getMail())
                .build());

        assertStatut(204, post("/biens/" + bienB.getId() + "/assigne-locataire/" + autreLocataire.getId())
                .with(connecte(proprietaireB)));

        assertThat(bienRepository.findById(bienB.getId()).orElseThrow().getLocataire().getId())
                .isEqualTo(autreLocataire.getId());
    }

    @Test
    @DisplayName("Recherches de B et de l'autre locataire filtrées sur le bien de A : aucun résultat")
    void recherches_filtreesSurUtilisateur() throws Exception {
        for (User intrus : List.of(proprietaireB, autreLocataire)) {
            for (String route : List.of("/contrats/search", "/quittances/search", "/etats-des-lieux/search")) {
                mockMvc.perform(post(route).with(connecte(intrus)).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"bienId\": " + bienA.getId() + "}"))
                        .andExpect(jsonPath("$.totalElements").value(0));
            }
        }
    }

    @Test
    @DisplayName("Autre locataire sur le bien de A : dashboard, bien privé et quittance → 403")
    void autreLocataire_ressourcesDeA_refuse() throws Exception {
        RequestPostProcessor intrus = connecte(autreLocataire);

        assertStatut(403, get("/locataire/dashboard/" + bienA.getId()).with(intrus));
        assertStatut(403, get("/users/biens/" + bienA.getId()).with(intrus));
        assertStatut(403, get("/quittances/" + quittanceA.getId()).with(intrus));
    }

    @Test
    @DisplayName("Rôles (@PreAuthorize) : propriétaire sur les routes locataire et inversement → 403")
    void roles_croises_refuses() throws Exception {
        RequestPostProcessor a = connecte(proprietaireA);
        RequestPostProcessor locataire = connecte(locataireA);

        assertStatut(403, get("/locataire/dashboard/" + bienA.getId()).with(a));
        assertStatut(403, get("/quittances/mes-quittances").with(a));
        assertStatut(403, post("/quittances/" + quittanceA.getId() + "/marquer-payee").with(locataire)
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertStatut(403, post("/contrats/" + contratA.getId() + "/signer-proprio").with(locataire)
                .contentType(MediaType.APPLICATION_JSON).content(SIGNATURE));
        assertStatut(403, patch("/biens/" + bienA.getId()).with(locataire)
                .contentType(MediaType.APPLICATION_JSON).content("{\"typeBien\": \"STUDIO\", \"titre\": \"Piraté\"}"));
    }

    @Test
    @DisplayName("Témoin : le propriétaire A et son locataire accèdent à leurs ressources (bien sans photo compris)")
    void proprietaireEtLocataireLegitimes_accedent() throws Exception {
        RequestPostProcessor a = connecte(proprietaireA);

        assertStatut(200, get("/users/biens/" + bienA.getId()).with(a));
        assertStatut(200, get("/quittances/" + quittanceA.getId()).with(a));
        assertStatut(200, get("/quittances/bien/" + bienA.getId()).with(a));
        assertStatut(200, patch("/biens/" + bienA.getId()).with(a)
                .contentType(MediaType.APPLICATION_JSON).content("{\"typeBien\": \"APPARTEMENT\", \"titre\": \"Appartement A rénové\"}"));
        assertStatut(200, get("/quittances/" + quittanceA.getId()).with(connecte(locataireA)));
        assertStatut(200, get("/users/biens/" + bienA.getId()).with(connecte(locataireA)));
        assertStatut(200, get("/locataire/dashboard/" + bienA.getId()).with(connecte(locataireA)));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Map<RequestMappingInfo, HandlerMethod> routesKupanga() {
        Map<RequestMappingInfo, HandlerMethod> routes = new java.util.HashMap<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            Class<?> controleur = handler.getBeanType();
            if (controleur.getPackageName().startsWith("com.kupanga.api")
                    && controleur.isAnnotationPresent(RestController.class)) {
                routes.put(info, handler);
            }
        });
        return routes;
    }

    private MockHttpServletResponse appeler(RequestMethod methode, String motif) throws Exception {
        String url = motif.replaceAll("\\{[^}]+}", "1");
        return mockMvc.perform(request(HttpMethod.valueOf(methode.name()), url)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse();
    }

    private User utilisateur(String mail, Role role) {
        return userRepository.save(User.builder()
                .firstName("Test").lastName("Secu").mail(mail).password("{noop}inutilise")
                .role(role).hasCompleteProfil(true)
                .build());
    }

    private Bien bien(String titre, User proprietaire, User locataire) {
        return bienRepository.save(Bien.builder()
                .titre(titre).adresse("1 rue Test").ville("Paris").codePostal("75001").pays("France")
                .typeBien(TypeBien.APPARTEMENT).loyerMensuel(800.0).chargesMensuelles(50.0)
                .proprietaire(proprietaire).locataire(locataire)
                .build());
    }

    private Contrat contrat(Bien bien) {
        return contratRepository.save(Contrat.builder()
                .bien(bien).proprietaire(bien.getProprietaire()).locataire(bien.getLocataire())
                .dateDebut(LocalDate.now()).dateFin(LocalDate.now().plusYears(1)).dureeBailMois(12)
                .loyerMensuel(800.0).chargesMensuelles(50.0).depotGarantie(800.0).adresseBien("1 rue Test")
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO)
                .build());
    }

    private String contratJson(Bien bien) {
        return """
                {"bienId": %d, "emailLocataire": "%s", "dateDebut": "%s", "dateFin": "%s",
                 "dureeBailMois": 12, "loyerMensuel": 800.0, "chargesMensuelles": 50.0, "depotGarantie": 800.0}
                """.formatted(bien.getId(), locataireA.getMail(), LocalDate.now().plusDays(1), LocalDate.now().plusYears(1));
    }

    private String edlJson(Bien bien) {
        return """
                {"bienId": %d, "emailLocataire": "%s", "type": "ENTREE", "dateRealisation": "%s"}
                """.formatted(bien.getId(), locataireA.getMail(), LocalDate.now());
    }

    private String quittanceJson(Bien bien, Long contratId) {
        return """
                {"bienId": %d, "emailLocataire": "%s", "contratId": %d, "mois": "FEVRIER", "annee": 2026,
                 "loyerMensuel": 800.0, "chargesMensuelles": 50.0, "dateEcheance": "%s"}
                """.formatted(bien.getId(), locataireA.getMail(), contratId, LocalDate.now().plusDays(5));
    }

    private RequestPostProcessor connecte(User user) {
        return SecurityMockMvcRequestPostProcessors.user(user.getMail())
                .authorities(new SimpleGrantedAuthority(user.getRole().name()));
    }

    private int statut(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andReturn().getResponse().getStatus();
    }

    private void assertStatut(int attendu, MockHttpServletRequestBuilder requete) throws Exception {
        var resultat = mockMvc.perform(requete).andReturn();
        assertThat(resultat.getResponse().getStatus())
                .as(resultat.getRequest().getMethod() + " " + resultat.getRequest().getRequestURI()
                        + " → " + resultat.getResponse().getContentAsString())
                .isEqualTo(attendu);
    }
}
