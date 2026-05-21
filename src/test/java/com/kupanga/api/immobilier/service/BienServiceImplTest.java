package com.kupanga.api.immobilier.service;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.BienMapper;
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
    @Mock private Authentication      auth;

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
        BienDTO dto = BienDTO.builder().id(1L).titre("Appartement T3").build();

        when(bienRepository.findWithAllProperties(1L)).thenReturn(Optional.of(bien));
        when(bienMapper.toPublicDTO(bien)).thenReturn(dto);

        BienDTO result = bienService.getBienInfos(1L);

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

        BienDTO expectedDTO = BienDTO.builder().id(1L).titre("Nouveau titre").build();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        when(bienRepository.save(bien)).thenReturn(bien);
        when(bienMapper.toPublicDTO(bien)).thenReturn(expectedDTO);

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
        when(bienMapper.toPublicDTO(bien)).thenReturn(BienDTO.builder().build());

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
