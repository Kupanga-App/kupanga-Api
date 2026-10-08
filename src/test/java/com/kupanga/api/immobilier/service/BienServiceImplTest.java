package com.kupanga.api.immobilier.service;

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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    @Mock private Authentication      auth;

    @Mock
    private DocumentPdfUrlMapper documentPdfUrlMapper;

    @InjectMocks
    private BienServiceImpl bienService;

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
                .pays("France")
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
        MultipartFile file = mock(MultipartFile.class);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(geocodingService.geocode(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(point);
        when(bienRepository.save(any(Bien.class))).thenReturn(bien);
        doNothing().when(bienPoiService).calculerEtSauvegarderPoi(any(Bien.class));
        doNothing().when(bienImageService).uploadImagesImo(anyList(), anyString(), any(Bien.class));

        assertDoesNotThrow(() -> bienService.createBien(auth, dto, List.of(file)));

        verify(bienRepository).save(any(Bien.class));
        verify(bienPoiService).calculerEtSauvegarderPoi(any(Bien.class));
        verify(bienImageService).uploadImagesImo(anyList(), anyString(), any(Bien.class));
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
        MultipartFile file = mock(MultipartFile.class);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        doNothing().when(userService).verifyIfUserIsOwner(proprietaire.getRole());
        when(geocodingService.geocode(anyString(), anyString(), anyString(), anyString()))
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

    // ══════════════════════════════════════════════════════════════
    // updateBien
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateBien() — succès : champs mis à jour, bien sauvegardé")
    void updateBien_success_updatesAndSaves() {
        BienUpdateDTO dto = new BienUpdateDTO();
        dto.setTitre("Nouveau titre");
        dto.setTypeBien(TypeBien.STUDIO);
        dto.setLoyerMensuel(900.0);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        when(bienRepository.save(bien)).thenReturn(bien);

        BienDTO result = bienService.updateBien(auth, 1L, dto);

        assertThat(result.titre()).isEqualTo("Nouveau titre");
        assertThat(bien.getTitre()).isEqualTo("Nouveau titre");
        assertThat(bien.getTypeBien()).isEqualTo(TypeBien.STUDIO);
        assertThat(bien.getLoyerMensuel()).isEqualTo(900.0);
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
                .pays("France")
                .surfaceHabitable(65.0)
                .nombrePieces(3)
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .depotGarantie(1700.0)
                .meuble(false)
                .colocation(false)
                .disponibleDe(LocalDate.now().plusDays(10))
                .build();
    }
}
