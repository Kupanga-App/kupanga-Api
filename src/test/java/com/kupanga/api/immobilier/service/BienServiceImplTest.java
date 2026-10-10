package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.mapper.DocumentPdfUrlMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.service.impl.BienServiceImpl;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

@DisplayName("Tests unitaires — BienServiceImpl")
class BienServiceImplTest {

    @Mock private UserService         userService;
    @Mock private BienImageService    bienImageService;
    @Mock private GeocodingService    geocodingService;
    @Mock private BienRepository      bienRepository;
    @Mock private BienMapper          bienMapper;
    @Mock private BienPoiService      bienPoiService;
    @Mock private NotificationService notificationService;
    @Mock private ConversationRepository conversationRepository;
    /** J3 : vrai registre (profils et plafonds de application.yml). */
    @Spy  private JuridictionRegistry juridictionRegistry = JuridictionsDeTest.registre();
    @Mock private Authentication      auth;

    @Mock
    private DocumentPdfUrlMapper documentPdfUrlMapper;

    @InjectMocks
    private BienServiceImpl bienService;

    /** En-tête JPEG réel : les photos sont reconnues au contenu (B5). */
    private static final byte[] PHOTO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private User proprietaire;
    private User locataire;
    private Bien bien;
    private Point point;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .role(Role.ROLE_PROPRIETAIRE)
                .build();

        locataire = User.builder()
                .id(2L)
                .mail("locataire@test.com")
                .role(Role.ROLE_LOCATAIRE)
                .build();

        point = new GeometryFactory().createPoint(new Coordinate(-1.553621, 47.218371));

        bien = Bien.builder()
                .id(1L)
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .codePostal("44000")
                .pays(Pays.FR).devise(Devise.EUR)
                .localisation(point)
                .proprietaire(proprietaire)
                .build();

        when(auth.getName()).thenReturn(proprietaire.getMail());
    }

    // ══════════════════════════════════════════════════════════════
    // createBien
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createBien() — succès : bien créé, POI calculés, images uploadées")
    void createBien_success() {
        BienFormDTO dto = buildValidFormDTO();
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(geocodingService.geocode(anyString(), anyString(), anyString(), any(Pays.class)))
                .thenReturn(point);
        when(bienRepository.save(any(Bien.class))).thenReturn(bien);
        doNothing().when(bienPoiService).calculerEtSauvegarderPoi(any());
        doNothing().when(bienImageService).uploadImagesImo(anyList(), anyString(), any(Bien.class));

        assertDoesNotThrow(() -> bienService.createBien(auth, dto, List.of(file)));

        verify(bienRepository).save(any(Bien.class));
        verify(bienPoiService).calculerEtSauvegarderPoi(any()); // id du bien (B9), sans transaction : appel direct
        verify(bienImageService).uploadImagesImo(anyList(), anyString(), any(Bien.class));
    }

    @Test
    @DisplayName("J2 : createBien() — pays sans profil de juridiction → 400, ni géocodage, ni bien, ni photo")
    void createBien_paysNonPrisEnCharge_refuse() {
        BienFormDTO dto = buildValidFormDTO();
        dto.setPays(Pays.BE);
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, List.of(file)));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(geocodingService, bienImageService);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("J3 : createBien() — sans devise : défaut du pays (USD en RDC) ; CDF demandé : CDF enregistré")
    void createBien_devise() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(geocodingService.geocode(anyString(), anyString(), any(), any(Pays.class))).thenReturn(point);
        when(bienRepository.save(any(Bien.class))).thenReturn(bien);
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        BienFormDTO enDollars = buildValidFormDTOKinshasa();
        bienService.createBien(auth, enDollars, List.of(file));

        BienFormDTO enFrancs = buildValidFormDTOKinshasa();
        enFrancs.setDevise(Devise.CDF);
        enFrancs.setLoyerMensuel(new BigDecimal("250000000"));
        bienService.createBien(auth, enFrancs, List.of(file));

        ArgumentCaptor<Bien> captor = ArgumentCaptor.forClass(Bien.class);
        verify(bienRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getDevise()).isEqualTo(Devise.USD);
        assertThat(captor.getAllValues().get(1).getDevise()).isEqualTo(Devise.CDF);
        assertThat(captor.getAllValues().get(1).getLoyerMensuel()).isEqualByComparingTo("250000000");
    }

    @Test
    @DisplayName("J4 : createBien() à Kinshasa — adresse congolaise enregistrée, textes vides → null, pas de code postal")
    void createBien_adresseCongolaise() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(geocodingService.geocode(anyString(), anyString(), any(), any(Pays.class))).thenReturn(point);
        when(bienRepository.save(any(Bien.class))).thenReturn(bien);
        BienFormDTO dto = buildValidFormDTOKinshasa();
        dto.setCodePostal("  ");
        dto.setAvenue("Kasa-Vubu");
        dto.setNumeroParcelle("");
        dto.setPointDeRepere(" Derrière l'église Saint-Joseph ");

        bienService.createBien(auth, dto, List.of(new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG)));

        ArgumentCaptor<Bien> captor = ArgumentCaptor.forClass(Bien.class);
        verify(bienRepository).save(captor.capture());
        Bien cree = captor.getValue();
        assertThat(cree.getCommune()).isEqualTo("Kalamu");
        assertThat(cree.getQuartier()).isEqualTo("Matonge");
        assertThat(cree.getAvenue()).isEqualTo("Kasa-Vubu");
        assertThat(cree.getNumeroParcelle()).isNull();
        assertThat(cree.getPointDeRepere()).isEqualTo("Derrière l'église Saint-Joseph");
        assertThat(cree.getCodePostal()).isNull();
        verify(geocodingService).geocode("N° 12, Av. Kasa-Vubu", "Kinshasa", null, Pays.CD);
    }

    @Test
    @DisplayName("J4 : createBien() — contrôle du pays refait par le service : quartier manquant (RDC), quartier en France → 400")
    void createBien_champsSelonPays_400() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        BienFormDTO sansQuartier = buildValidFormDTOKinshasa();
        sansQuartier.setQuartier(null);
        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, sansQuartier, List.of(file)));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).startsWith("quartier").contains("obligatoire");

        BienFormDTO franceAvecQuartier = buildValidFormDTO();
        franceAvecQuartier.setQuartier("Centre");
        ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, franceAvecQuartier, List.of(file)));
        assertThat(ex.getMessage()).startsWith("quartier").contains("ne s'applique pas");

        verifyNoInteractions(geocodingService, bienImageService);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("J4 : updateBien() d'un bien en RDC — DPE (masqué) → 400, rien n'est modifié ; en France, DPE accepté")
    void updateBien_champMasque_400() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.USD);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTitre("Nouveau titre");
        dto.setClasseEnergie(ClasseEnergie.B);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.updateBien(auth, 1L, dto));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).startsWith("classeEnergie");
        assertThat(bien.getTitre()).isEqualTo("Appartement T3");
        verify(bienRepository, never()).save(any());

        bien.setPays(Pays.FR);
        bien.setDevise(Devise.EUR);
        when(bienRepository.save(bien)).thenReturn(bien);
        bienService.updateBien(auth, 1L, dto);
        assertThat(bien.getClasseEnergie()).isEqualTo(ClasseEnergie.B);
    }

    @Test
    @DisplayName("J3 : createBien() — devise refusée dans le pays (USD en France) → 400, ni géocodage ni bien")
    void createBien_deviseRefusee_400() {
        BienFormDTO dto = buildValidFormDTO();
        dto.setDevise(Devise.USD);
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, List.of(file)));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(geocodingService, bienImageService);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("C5 : createBien() — loyer en euros au-dessus du plafond → 400, rien d'enregistré")
    void createBien_plafondDepasse_400() {
        BienFormDTO dto = buildValidFormDTO();
        dto.setLoyerMensuel(new BigDecimal("250000"));
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, List.of(file)));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("EUR");
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("J3 : updateBien() — devise changée (USD → CDF en RDC) avec un loyer cohérent ; devise étrangère au pays → 400")
    void updateBien_devise() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.USD);
        bien.setLoyerMensuel(new BigDecimal("500"));
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        when(bienRepository.save(bien)).thenReturn(bien);

        BienUpdateDTO versEuros = new BienUpdateDTO();
        versEuros.setDevise(Devise.EUR);
        assertThatThrownBy(() -> bienService.updateBien(auth, 1L, versEuros))
                .isInstanceOf(KupangaBusinessException.class);
        assertThat(bien.getDevise()).isEqualTo(Devise.USD);

        BienUpdateDTO versFrancs = new BienUpdateDTO();
        versFrancs.setDevise(Devise.CDF);
        versFrancs.setLoyerMensuel(new BigDecimal("1400000"));
        bienService.updateBien(auth, 1L, versFrancs);

        assertThat(bien.getDevise()).isEqualTo(Devise.CDF);
        assertThat(bien.getLoyerMensuel()).isEqualByComparingTo("1400000");
    }

    @Test
    @DisplayName("C5 : updateBien() — loyer existant au-dessus du plafond de la nouvelle devise → 400, bien inchangé")
    void updateBien_plafondNouvelleDevise_400() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.CDF);
        bien.setLoyerMensuel(new BigDecimal("1400000"));
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));

        BienUpdateDTO versDollars = new BienUpdateDTO();
        versDollars.setDevise(Devise.USD);

        assertThatThrownBy(() -> bienService.updateBien(auth, 1L, versDollars))
                .isInstanceOf(KupangaBusinessException.class)
                .hasMessageContaining("USD");
        assertThat(bien.getDevise()).isEqualTo(Devise.CDF);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBien() — exécuteur asynchrone saturé (POI refusés) → bien créé et photos envoyées quand même (B9)")
    void createBien_executeurSature_bienCreeQuandMeme() {
        BienFormDTO dto = buildValidFormDTO();
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(geocodingService.geocode(anyString(), anyString(), anyString(), any(Pays.class))).thenReturn(point);
        when(bienRepository.save(any(Bien.class))).thenReturn(bien);
        doThrow(new TaskRejectedException("file pleine")).when(bienPoiService).calculerEtSauvegarderPoi(any());

        assertDoesNotThrow(() -> bienService.createBien(auth, dto, List.of(file)));

        verify(bienImageService).uploadImagesImo(anyList(), anyString(), any(Bien.class));
    }

    @Test
    @DisplayName("createBien() — photo au contenu non image → 415, aucun bien enregistré ni envoi MinIO (B5)")
    void createBien_photoNonImage_refuseAvantEnregistrement() {
        BienFormDTO dto = buildValidFormDTO();
        MultipartFile piege = new MockMultipartFile("files", "p.jpg", "image/jpeg", "<html>".getBytes());

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, List.of(
                        new MockMultipartFile("files", "ok.jpg", "image/jpeg", PHOTO_JPEG), piege)));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getStatus());
        verify(bienRepository, never()).save(any(Bien.class));
        verify(bienImageService, never()).uploadImagesImo(anyList(), anyString(), any(Bien.class));
    }

    @Test
    @DisplayName("createBien() — 21 photos → 400, aucun bien enregistré (B5)")
    void createBien_tropDePhotos_refuse() {
        BienFormDTO dto = buildValidFormDTO();
        MultipartFile photo = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, java.util.Collections.nCopies(21, photo)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(bienRepository, never()).save(any(Bien.class));
    }

    @Test
    @DisplayName("createBien() — liste de fichiers vide → KupangaBusinessException 400")
    void createBien_emptyFiles_throwsBadRequest() {
        BienFormDTO dto = buildValidFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, Collections.emptyList()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBien() — fichiers null → KupangaBusinessException 400")
    void createBien_nullFiles_throwsBadRequest() {
        BienFormDTO dto = buildValidFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, null));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(geocodingService, never()).geocode(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createBien() — géocodage échoue (null) → KupangaBusinessException 404")
    void createBien_geocodingFails_throwsNotFound() {
        BienFormDTO dto = buildValidFormDTO();
        MultipartFile file = new MockMultipartFile("files", "p.jpg", "image/jpeg", PHOTO_JPEG);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(geocodingService.geocode(anyString(), anyString(), anyString(), any(Pays.class)))
                .thenReturn(null);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.createBien(auth, dto, List.of(file)));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(bienRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // getBienInfos
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getBienInfos() — bien trouvé → BienDTO retourné")
    void getBienInfos_found_returnsDTO() {
        BienPublicDTO dto = BienPublicDTO.builder().id(1L).titre("Appartement T3").build();

        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(bienMapper.toPublicDTO(bien)).thenReturn(dto);

        BienPublicDTO result = bienService.getBienInfos(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.titre()).isEqualTo("Appartement T3");
    }

    @Test
    @DisplayName("getBienInfos() — bien introuvable → KupangaBusinessException 404")
    void getBienInfos_notFound_throwsException() {
        when(bienRepository.findWithAllProperties(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.getBienInfos(99L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // findWithAllProperties
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findWithAllProperties() — bien trouvé → Bien retourné")
    void findWithAllProperties_found_returnsBien() {
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        Bien result = bienService.findWithAllProperties(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findWithAllProperties() — bien introuvable → KupangaBusinessException 404")
    void findWithAllProperties_notFound_throwsException() {
        when(bienRepository.findWithAllProperties(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.findWithAllProperties(99L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // findById
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findById() — bien trouvé → Bien retourné")
    void findById_found_returnsBien() {
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));

        Bien result = bienService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById() — bien introuvable → KupangaBusinessException 404")
    void findById_notFound_throwsException() {
        when(bienRepository.findById(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.findById(99L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // findAllPropertiesAssociateToUser
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findAllPropertiesAssociateToUser() — retourne la liste des biens de l'utilisateur")
    void findAllPropertiesAssociateToUser_returnsList() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findAllPropertiesAssociateToUser(proprietaire.getId()))
                .thenReturn(List.of(bien));

        List<BienDTO> result = bienService.findAllPropertiesAssociateToUser(proprietaire.getMail());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).titre()).isEqualTo("Appartement T3");
    }

    @Test
    @DisplayName("findAllPropertiesAssociateToUser() — aucun bien → liste vide")
    void findAllPropertiesAssociateToUser_emptyList() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findAllPropertiesAssociateToUser(proprietaire.getId()))
                .thenReturn(Collections.emptyList());

        List<BienDTO> result = bienService.findAllPropertiesAssociateToUser(proprietaire.getMail());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllPropertiesAssociateToUser() — un locataire ne voit que ses propres contrats et quittances (P0-5)")
    void findAllPropertiesAssociateToUser_locataire_seesOnlyOwnDocuments() {
        User ancienLocataire = User.builder().id(7L).mail("ancien@test.com").role(Role.ROLE_LOCATAIRE).build();
        bien.setLocataire(locataire);
        bien.setContrats(new java.util.HashSet<>(List.of(
                Contrat.builder().id(1L).locataire(locataire).clePdf("contrat-actuel.pdf").build(),
                Contrat.builder().id(2L).locataire(ancienLocataire).clePdf("contrat-ancien.pdf").build())));
        bien.setQuittances(new java.util.HashSet<>(List.of(
                Quittance.builder().id(1L).locataire(locataire).clePdf("quittance-actuelle.pdf").build(),
                Quittance.builder().id(2L).locataire(ancienLocataire).clePdf("quittance-ancienne.pdf").build())));

        when(documentPdfUrlMapper.urlContrat(anyString())).thenAnswer(i -> "signe:" + i.getArgument(0));
        when(documentPdfUrlMapper.urlQuittance(anyString())).thenAnswer(i -> "signe:" + i.getArgument(0));
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienRepository.findAllPropertiesAssociateToUser(locataire.getId())).thenReturn(List.of(bien));

        BienDTO dto = bienService.findAllPropertiesAssociateToUser(locataire.getMail()).get(0);

        assertThat(dto.contrats()).containsExactly("signe:contrat-actuel.pdf");
        assertThat(dto.quittances()).containsExactly("signe:quittance-actuelle.pdf");
    }

    @Test
    @DisplayName("findAllPropertiesAssociateToUser() — le propriétaire voit tous les documents du bien")
    void findAllPropertiesAssociateToUser_proprietaire_seesAllDocuments() {
        User ancienLocataire = User.builder().id(7L).mail("ancien@test.com").role(Role.ROLE_LOCATAIRE).build();
        bien.setContrats(new java.util.HashSet<>(List.of(
                Contrat.builder().id(1L).locataire(locataire).clePdf("contrat-actuel.pdf").build(),
                Contrat.builder().id(2L).locataire(ancienLocataire).clePdf("contrat-ancien.pdf").build())));

        when(documentPdfUrlMapper.urlContrat(anyString())).thenAnswer(i -> "signe:" + i.getArgument(0));
        when(documentPdfUrlMapper.urlQuittance(anyString())).thenAnswer(i -> "signe:" + i.getArgument(0));
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findAllPropertiesAssociateToUser(proprietaire.getId())).thenReturn(List.of(bien));

        BienDTO dto = bienService.findAllPropertiesAssociateToUser(proprietaire.getMail()).get(0);

        assertThat(dto.contrats()).containsExactlyInAnyOrder("signe:contrat-actuel.pdf", "signe:contrat-ancien.pdf");
    }

    // ══════════════════════════════════════════════════════════════
    // existsByIdAndProprietaireId
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("existsByIdAndProprietaireId() — délègue au repository et retourne le résultat")
    void existsByIdAndProprietaireId_delegatesToRepository() {
        when(bienRepository.existsByIdAndProprietaireId(1L, 1L)).thenReturn(true);
        when(bienRepository.existsByIdAndProprietaireId(1L, 99L)).thenReturn(false);

        assertThat(bienService.existsByIdAndProprietaireId(1L, 1L)).isTrue();
        assertThat(bienService.existsByIdAndProprietaireId(1L, 99L)).isFalse();
    }

    @Test
    @DisplayName("B12 : getBienInfos() — bien archivé → 404, comme un id inconnu")
    void getBienInfos_archive_404() {
        bien.setArchive(true);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.getBienInfos(1L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(bienMapper);
    }

    @Test
    @DisplayName("B12 : getBienPrive() — bien archivé toujours visible par son propriétaire, avec archive = true")
    void getBienPrive_archive_lisible() {
        bien.setArchive(true);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        BienDTO result = bienService.getBienPrive(1L, proprietaire.getMail());

        assertThat(result.archive()).isTrue();
    }

    @Test
    @DisplayName("B12 : updateBien() — bien archivé → 409, rien n'est modifié")
    void updateBien_archive_409() {
        bien.setArchive(true);
        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTitre("Nouveau titre");
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.updateBien(auth, 1L, dto));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bien.getTitre()).isEqualTo("Appartement T3");
        verify(bienRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // updateBien
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateBien() — succès : champs mis à jour, bien sauvegardé")
    void updateBien_success_updatesAndSaves() {
        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTitre("Nouveau titre");
        dto.setTypeBien(TypeBien.STUDIO);
        dto.setLoyerMensuel(new BigDecimal("900.0"));

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        when(bienRepository.save(bien)).thenReturn(bien);

        BienDTO result = bienService.updateBien(auth, 1L, dto);

        assertThat(result.titre()).isEqualTo("Nouveau titre");
        assertThat(bien.getTitre()).isEqualTo("Nouveau titre");
        assertThat(bien.getTypeBien()).isEqualTo(TypeBien.STUDIO);
        assertThat(bien.getLoyerMensuel()).isEqualByComparingTo("900.0");
        verify(bienRepository).save(bien);
    }

    @Test
    @DisplayName("updateBien() — description vide '' → description mise à null")
    void updateBien_emptyDescription_setsNull() {
        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTypeBien(TypeBien.APPARTEMENT);
        dto.setDescription("");

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));

        bienService.updateBien(auth, 1L, dto);

        assertThat(bien.getDescription()).isNull();
    }

    @Test
    @DisplayName("updateBien() — utilisateur non propriétaire → KupangaBusinessException 403")
    void updateBien_notOwner_throwsForbidden() {
        User autreUser = User.builder().id(99L).mail("autre@test.com").build();
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(autreUser);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));

        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTypeBien(TypeBien.APPARTEMENT);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.updateBien(auth, 1L, dto));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateBien() — bien introuvable → KupangaBusinessException 404")
    void updateBien_bienNotFound_throwsException() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.updateBien(auth, 99L, new BienUpdateDTO()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // affectLocataire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("affectLocataire() — succès : locataire assigné au bien")
    void affectLocataire_success_assignsLocataire() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(userService.findById(2L)).thenReturn(locataire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                1L, proprietaire.getMail(), locataire.getMail())).thenReturn(Optional.of(new Conversation()));
        when(bienRepository.save(bien)).thenReturn(bien);

        assertDoesNotThrow(() -> bienService.affectLocataire(auth, 1L, 2L));

        assertThat(bien.getLocataire()).isEqualTo(locataire);
        verify(bienRepository).save(bien);
        verify(notificationService).saveAndSend(
                eq(locataire), eq(NotificationType.BIEN_ASSIGNE),
                anyString(), anyString(), isNull(), eq(bien.getId()));
        verify(notificationService).saveAndSend(
                eq(proprietaire), eq(NotificationType.BIEN_ASSIGNATION_CONFIRMEE),
                anyString(), anyString(), isNull(), eq(bien.getId()));
    }

    @Test
    @DisplayName("B12 : affectLocataire() — bien archivé → 409, rien n'est assigné")
    void affectLocataire_bienArchive_409() {
        bien.setArchive(true);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 2L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("B12 : affectLocataire() — compte anonymisé → 404, comme un non-candidat")
    void affectLocataire_compteAnonymise_404() {
        locataire.setAnonymise(true);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(userService.findById(2L)).thenReturn(locataire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                1L, proprietaire.getMail(), locataire.getMail())).thenReturn(Optional.of(new Conversation()));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 2L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("affectLocataire() — locataire sans conversation sur ce bien → 404, rien n'est assigné (TESTS-SECU)")
    void affectLocataire_pasCandidat_refuse() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(userService.findById(2L)).thenReturn(locataire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                1L, proprietaire.getMail(), locataire.getMail())).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 2L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).doesNotContain(locataire.getMail());
        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("affectLocataire() — id inconnu → même 404 qu'un non-candidat (pas d'énumération des comptes)")
    void affectLocataire_idInconnu_memeReponse() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(userService.findById(999L)).thenThrow(
                new KupangaBusinessException("Aucun utilisateur trouvé pour l'Id : 999", HttpStatus.NOT_FOUND));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 999L));

        assertThat(ex.getMessage()).isEqualTo("Locataire introuvable pour ce bien");
    }

    @Test
    @DisplayName("affectLocataire() — bien introuvable → KupangaBusinessException 404")
    void affectLocataire_bienNotFound_throwsException() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(userService.findById(2L)).thenReturn(locataire);
        when(bienRepository.findWithAllProperties(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 99L, 2L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("affectLocataire() — user non propriétaire → exception lancée par verifyIfUserIsOwner")
    void affectLocataire_notOwner_throwsException() {
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doThrow(new KupangaBusinessException("Accès refusé", HttpStatus.FORBIDDEN))
                .when(userService).verifyIfUserIsOwner(proprietaire.getRole());

        assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 2L));

        verify(bienRepository, never()).save(any());
    }

    @Test
    @DisplayName("affectLocataire() — bien d'un autre propriétaire → 403, rien n'est assigné (P0-5)")
    void affectLocataire_bienDAutrui_throwsForbidden() {
        User autre = User.builder().id(9L).mail("autre@test.com").role(Role.ROLE_PROPRIETAIRE).build();
        when(auth.getName()).thenReturn(autre.getMail());
        when(userService.getUserByEmail(autre.getMail())).thenReturn(autre);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 2L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bien.getLocataire()).isNull();
        verify(bienRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("affectLocataire() — l'utilisateur assigné n'est pas un locataire → 400 (P0-5)")
    void affectLocataire_userNotLocataire_throwsBadRequest() {
        User autreProprio = User.builder().id(3L).mail("p2@test.com").role(Role.ROLE_PROPRIETAIRE).build();
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(userService.findById(3L)).thenReturn(autreProprio);
        when(conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                1L, proprietaire.getMail(), autreProprio.getMail())).thenReturn(Optional.of(new Conversation()));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.affectLocataire(auth, 1L, 3L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(bienRepository, never()).save(any());
    }

    // ══════════════════════════════════════════════════════════════
    // getBienPrive (P0-6)
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getBienPrive() — propriétaire : vue privée avec e-mail du locataire")
    void getBienPrive_proprietaire_returnsPrivateView() {
        bien.setLocataire(locataire);
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        BienDTO dto = bienService.getBienPrive(1L, proprietaire.getMail());

        assertThat(dto.locataire().mail()).isEqualTo(locataire.getMail());
    }

    @Test
    @DisplayName("getBienPrive() — locataire du bien : autorisé")
    void getBienPrive_locataire_allowed() {
        bien.setLocataire(locataire);
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        assertThat(bienService.getBienPrive(1L, locataire.getMail()).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBienPrive() — autre utilisateur : 403")
    void getBienPrive_autreUtilisateur_throwsForbidden() {
        User autre = User.builder().id(9L).mail("autre@test.com").role(Role.ROLE_LOCATAIRE).build();
        bien.setLocataire(locataire);
        when(userService.getUserByEmail(autre.getMail())).thenReturn(autre);
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.getBienPrive(1L, autre.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════
    // verifierProprietaire / verifierLocataireDuBien (P0-5)
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("verifierProprietaire() — propriétaire du bien : renvoie le bien")
    void verifierProprietaire_owner_returnsBien() {
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        assertThat(bienService.verifierProprietaire(1L, proprietaire.getMail())).isEqualTo(bien);
    }

    @Test
    @DisplayName("verifierProprietaire() — autre utilisateur : 403")
    void verifierProprietaire_notOwner_throwsForbidden() {
        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierProprietaire(1L, "autre@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("verifierProprietaire() — bien introuvable : 404")
    void verifierProprietaire_bienNotFound_throwsNotFound() {
        when(bienRepository.findWithAllProperties(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierProprietaire(99L, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("verifierLocataireDuBien() — locataire assigné : renvoie le locataire")
    void verifierLocataireDuBien_assigned_returnsLocataire() {
        bien.setLocataire(locataire);

        assertThat(bienService.verifierLocataireDuBien(bien, "LOCATAIRE@test.com")).isEqualTo(locataire);
    }

    @Test
    @DisplayName("verifierLocataireDuBien() — autre e-mail : 400")
    void verifierLocataireDuBien_otherEmail_throwsBadRequest() {
        bien.setLocataire(locataire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierLocataireDuBien(bien, "victime@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("verifierLocataireDuBien() — aucun locataire assigné : 400")
    void verifierLocataireDuBien_noLocataire_throwsBadRequest() {
        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierLocataireDuBien(bien, locataire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ══════════════════════════════════════════════════════════════
    // Fixture
    // ══════════════════════════════════════════════════════════════

    private BienFormDTO buildValidFormDTO() {
        return BienFormDTO.builder()
                .titre("Appartement T3")
                .typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .codePostal("44000")
                .pays(Pays.FR)
                .surfaceHabitable(65.0)
                .nombrePieces(3)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .depotGarantie(new BigDecimal("1700.0"))
                .meuble(false)
                .colocation(false)
                .disponibleDe(LocalDate.now().plusDays(10))
                .build();
    }

    /** J4 : bien à Kinshasa (commune et quartier obligatoires, pas de code postal). */
    private BienFormDTO buildValidFormDTOKinshasa() {
        BienFormDTO dto = buildValidFormDTO();
        dto.setPays(Pays.CD);
        dto.setAdresse("N° 12, Av. Kasa-Vubu");
        dto.setVille("Kinshasa");
        dto.setCodePostal(null);
        dto.setCommune("Kalamu");
        dto.setQuartier("Matonge");
        return dto;
    }

    @Test
    @DisplayName("B12 : verifierDocumentModifiable() — 409 si bien archivé ou partie anonymisée, sinon rien")
    void verifierDocumentModifiable() {
        assertDoesNotThrow(() -> bienService.verifierDocumentModifiable(bien, proprietaire, locataire));

        locataire.setAnonymise(true);
        assertThat(assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierDocumentModifiable(bien, proprietaire, locataire)).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        locataire.setAnonymise(false);
        bien.setArchive(true);
        assertThat(assertThrows(KupangaBusinessException.class,
                () -> bienService.verifierDocumentModifiable(bien, proprietaire, locataire)).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
