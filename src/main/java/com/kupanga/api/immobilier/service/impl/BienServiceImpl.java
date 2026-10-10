package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.config.ApresCommit;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.mapper.DocumentPdfUrlMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.service.BienImageService;
import com.kupanga.api.immobilier.service.BienPoiService;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.GeocodingService;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.minio.image.ValidationImage;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.service.UserService;
import com.kupanga.api.user.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

import static com.kupanga.api.minio.constant.MinioConstant.PHOTO_IMO_BUCKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class BienServiceImpl implements BienService {

    private final UserService        userService;
    private final BienImageService   bienImageService;
    private final GeocodingService   geocodingService;
    private final BienRepository     bienRepository;
    private final BienMapper         bienMapper;
    private final BienPoiService     bienPoiService;
    private final NotificationService notificationService;
    private final DocumentPdfUrlMapper documentPdfUrlMapper;
    private final ConversationRepository conversationRepository;
    private final JuridictionRegistry juridictionRegistry;

    public void createBien(Authentication auth, BienFormDTO dto, List<MultipartFile> files) {

        User user = userService.getUserByEmail(auth.getName());
        userService.verifyIfUserIsOwner(user.getRole());

        if (files == null || files.isEmpty()) {
            throw new KupangaBusinessException(
                    "Les photos pour le bien publié sont obligatoires sur notre site",
                    HttpStatus.BAD_REQUEST
            );
        }
        // B5 : toutes les photos contrôlées avant tout enregistrement (ni bien créé, ni envoi MinIO partiel)
        ValidationImage.verifierPhotosBien(files);
        // J2 : seuls les pays dotés d'un profil de juridiction sont pris en charge (400 sinon)
        // J3 : devise acceptée dans ce pays (défaut du profil sinon) et montants sous les plafonds de la devise
        Devise devise = juridictionRegistry.devisePour(dto.getPays(), dto.getDevise());
        juridictionRegistry.verifierMontants(devise, dto.getLoyerMensuel(), dto.getChargesMensuelles(),
                dto.getDepotGarantie());
        // J4 (C1) : champs obligatoires / masqués et types de bien du pays (déjà contrôlés par
        // @ValideSelonJuridiction ; refait ici pour tout appel qui ne passerait pas par le contrôleur)
        juridictionRegistry.verifierBien(dto.getPays(), dto, true);

        Bien bien = Bien.builder()
                // ─── Informations générales ───────────────────────────────────
                .titre(dto.getTitre())
                .typeBien(dto.getTypeBien())
                .description(dto.getDescription())
                .proprietaire(user)

                // ─── Adresse ──────────────────────────────────────────────────
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .codePostal(texte(dto.getCodePostal()))
                .pays(dto.getPays())
                // J4 : adresse congolaise (vide hors RDC : champs masqués)
                .commune(texte(dto.getCommune()))
                .quartier(texte(dto.getQuartier()))
                .avenue(texte(dto.getAvenue()))
                .numeroParcelle(texte(dto.getNumeroParcelle()))
                .pointDeRepere(texte(dto.getPointDeRepere()))

                // ─── Caractéristiques physiques ───────────────────────────────
                .surfaceHabitable(dto.getSurfaceHabitable())
                .nombrePieces(dto.getNombrePieces())
                .nombreChambres(dto.getNombreChambres())
                .etage(dto.getEtage())
                .ascenseur(dto.getAscenseur())
                .anneeConstruction(dto.getAnneeConstruction())
                .modeChauffage(dto.getModeChauffage())

                // ─── Diagnostic énergétique ───────────────────────────────────
                .classeEnergie(dto.getClasseEnergie())
                .classeGes(dto.getClasseGes())

                // ─── Conditions de location ───────────────────────────────────
                .loyerMensuel(dto.getLoyerMensuel())
                .chargesMensuelles(dto.getChargesMensuelles())
                .depotGarantie(dto.getDepotGarantie())
                .devise(devise)
                .meuble(dto.getMeuble())
                .colocation(dto.getColocation())
                .disponibleDe(dto.getDisponibleDe())

                .build();

        Point point = geocodingService.geocode(dto.getAdresse(), dto.getVille(),
                bien.getCodePostal(), dto.getPays());

        if (point == null) {
            throw new KupangaBusinessException(
                    "Nous n'avons pas pu géolocaliser votre bien",
                    HttpStatus.NOT_FOUND
            );
        }

        bien.setLocalisation(point);
        log.info("Géocodage réussi -> {}", point);

        bienRepository.save(bien);

        calculerPoiApresCommit(bien.getId());

        bienImageService.uploadImagesImo(files, PHOTO_IMO_BUCKET, bien);
    }

    /**
     * B9 : le calcul asynchrone des POI relit le bien par son id ; il ne part qu'une fois le bien validé en base
     * (sinon le thread asynchrone pourrait ne pas le trouver).
     */
    private void calculerPoiApresCommit(Long bienId) {
        ApresCommit.executer(() -> lancerCalculPoi(bienId));
    }

    /** Les POI sont facultatifs : un exécuteur asynchrone saturé ne doit pas faire échouer la création du bien. */
    private void lancerCalculPoi(Long bienId) {
        try {
            bienPoiService.calculerEtSauvegarderPoi(bienId);
        } catch (TaskRejectedException e) {
            log.warn("Calcul des POI du bien {} refusé (exécuteur asynchrone saturé)", bienId);
        }
    }

    @Override
    public BienPublicDTO getBienInfos(Long id){

        // B12 : un bien archivé n'est plus public (même réponse qu'un id inconnu)
        Bien bien = bienRepository.findWithAllProperties(id)
                .filter(b -> !b.isArchive())
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );

        return bienMapper.toPublicDTO(bien);

    }

    @Override
    @Transactional
    public BienDTO getBienPrive(Long bienId, String email) {

        User user = userService.getUserByEmail(email);
        Bien bien = findWithAllProperties(bienId);

        if (!estProprietaire(bien, user) && !concerne(bien.getLocataire(), user)) {
            throw new KupangaBusinessException(
                    "Accès refusé : vous n'êtes ni le propriétaire ni le locataire de ce bien", HttpStatus.FORBIDDEN);
        }
        return toPriveDTO(bien, user);
    }

    @Override
    public Bien findWithAllProperties(Long id){

        return bienRepository.findWithAllProperties(id)
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );
    }

    @Override
    @Transactional
    public List<BienDTO> findAllPropertiesAssociateToUser(String email){

        User user = userService.getUserByEmail(email);

        return bienRepository.findAllPropertiesAssociateToUser(user.getId())
                .stream()
                .map(bien -> toPriveDTO(bien, user))
                .toList();
    }

    @Override
    public Bien findById(Long bienId){

        return bienRepository.findById(bienId).orElseThrow(() ->
                new KupangaBusinessException("Aucun bien trouvé pour cet id" , HttpStatus.NOT_FOUND)
        );
    }

    @Override
    public boolean existsByIdAndProprietaireId(Long bienId, Long proprietaireId) {
        return bienRepository.existsByIdAndProprietaireId(bienId, proprietaireId);
    }

    @Override
    public BienDTO updateBien(Authentication auth, Long bienId, BienUpdateDTO dto) {

        User proprietaire = userService.getUserByEmail(auth.getName());
        Bien bien = findById(bienId);

        if (!bien.getProprietaire().getId().equals(proprietaire.getId())) {
            throw new KupangaBusinessException(
                    "Accès refusé : vous n'êtes pas le propriétaire de ce bien",
                    HttpStatus.FORBIDDEN
            );
        }
        verifierBienActif(bien);
        // J4 : champs masqués (ex. DPE en RDC) et type de bien selon le pays du bien, avant toute modification
        juridictionRegistry.verifierBien(bien.getPays(), dto, false);

        // ─── Informations générales ───────────────────────────────────────────
        if (dto.getTitre()       != null) bien.setTitre(dto.getTitre());
        if (dto.getTypeBien()    != null) bien.setTypeBien(dto.getTypeBien());

        // "" = effacement explicite · null = inchangé
        if (dto.getDescription() != null) {
            bien.setDescription(dto.getDescription().isEmpty() ? null : dto.getDescription());
        }

        // ─── Caractéristiques physiques ───────────────────────────────────────
        if (dto.getSurfaceHabitable()  != null) bien.setSurfaceHabitable(dto.getSurfaceHabitable());
        if (dto.getNombrePieces()      != null) bien.setNombrePieces(dto.getNombrePieces());
        if (dto.getNombreChambres()    != null) bien.setNombreChambres(dto.getNombreChambres());
        if (dto.getEtage()             != null) bien.setEtage(dto.getEtage());
        if (dto.getAscenseur()         != null) bien.setAscenseur(dto.getAscenseur());
        if (dto.getAnneeConstruction() != null) bien.setAnneeConstruction(dto.getAnneeConstruction());
        if (dto.getModeChauffage()     != null) bien.setModeChauffage(dto.getModeChauffage());

        // ─── Diagnostic énergétique ───────────────────────────────────────────
        if (dto.getClasseEnergie() != null) bien.setClasseEnergie(dto.getClasseEnergie());
        if (dto.getClasseGes()     != null) bien.setClasseGes(dto.getClasseGes());

        // ─── Conditions de location ───────────────────────────────────────────
        // J3 : nouvelle devise acceptée dans le pays ; montants (nouveaux ou existants) sous les plafonds
        Devise devise = dto.getDevise() != null
                ? juridictionRegistry.devisePour(bien.getPays(), dto.getDevise())
                : bien.getDevise();
        juridictionRegistry.verifierMontants(devise,
                dto.getLoyerMensuel()      != null ? dto.getLoyerMensuel()      : bien.getLoyerMensuel(),
                dto.getChargesMensuelles() != null ? dto.getChargesMensuelles() : bien.getChargesMensuelles(),
                dto.getDepotGarantie()     != null ? dto.getDepotGarantie()     : bien.getDepotGarantie());
        bien.setDevise(devise);
        if (dto.getLoyerMensuel()      != null) bien.setLoyerMensuel(dto.getLoyerMensuel());
        if (dto.getChargesMensuelles() != null) bien.setChargesMensuelles(dto.getChargesMensuelles());
        if (dto.getDepotGarantie()     != null) bien.setDepotGarantie(dto.getDepotGarantie());
        if (dto.getMeuble()            != null) bien.setMeuble(dto.getMeuble());
        if (dto.getColocation()        != null) bien.setColocation(dto.getColocation());
        if (dto.getDisponibleDe()      != null) bien.setDisponibleDe(dto.getDisponibleDe());

        bienRepository.save(bien);

        return toPriveDTO(bien, proprietaire);
    }

    @Override
    public void affectLocataire( Authentication auth, Long bienId , Long userId) {

        User proprietaire = userService.getUserByEmail(auth.getName());
        userService.verifyIfUserIsOwner(proprietaire.getRole());
        Bien bien = verifierProprietaire(bienId, proprietaire.getMail());
        verifierBienActif(bien);
        // Seuls les candidats du bien (conversation avec le propriétaire sur ce bien) peuvent être assignés :
        // sinon un propriétaire pourrait assigner n'importe quel compte et lire son e-mail (revue TESTS-SECU).
        // Id inconnu et non-candidat donnent la même réponse : rien n'indique si le compte existe.
        User locataire;
        try {
            locataire = userService.findById(userId);
        } catch (KupangaBusinessException e) {
            throw locataireIntrouvable();
        }
        // B12 : un compte anonymisé ne peut plus être assigné
        if (locataire.isAnonymise() || conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                bienId, proprietaire.getMail(), locataire.getMail()).isEmpty()) {
            throw locataireIntrouvable();
        }
        if (locataire.getRole() != Role.ROLE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "L'utilisateur à assigner doit être un locataire", HttpStatus.BAD_REQUEST);
        }
        bien.setLocataire(locataire);
        bienRepository.save(bien);

        // Notification au locataire
        notificationService.saveAndSend(
                locataire,
                NotificationType.BIEN_ASSIGNE,
                "Un bien vous a été assigné",
                "Le propriétaire " + proprietaire.getFirstName() + " "
                        + proprietaire.getLastName()
                        + " vous a assigné le bien : "
                        + bien.getAdresse() + ", " + bien.getVille(),
                null,
                bien.getId()
        );

        // Confirmation au propriétaire
        notificationService.saveAndSend(
                proprietaire,
                NotificationType.BIEN_ASSIGNATION_CONFIRMEE,
                "Locataire assigné avec succès",
                locataire.getFirstName() + " " + locataire.getLastName()
                        + " a été assigné au bien "
                        + bien.getAdresse() + ", " + bien.getVille() + ".",
                null,
                bien.getId()
        );
    }

    private KupangaBusinessException locataireIntrouvable() {
        return new KupangaBusinessException("Locataire introuvable pour ce bien", HttpStatus.NOT_FOUND);
    }

    @Override
    public Bien verifierProprietaire(Long bienId, String emailProprietaire) {

        Bien bien = findWithAllProperties(bienId);

        if (bien.getProprietaire() == null || !bien.getProprietaire().getMail().equals(emailProprietaire)) {
            throw new KupangaBusinessException(
                    "Accès refusé : vous n'êtes pas le propriétaire de ce bien", HttpStatus.FORBIDDEN);
        }
        return bien;
    }

    @Override
    public User verifierLocataireDuBien(Bien bien, String emailLocataire) {

        User locataire = bien.getLocataire();

        if (locataire == null || emailLocataire == null || !locataire.getMail().equals(EmailUtils.normaliser(emailLocataire))) {
            throw new KupangaBusinessException(
                    "Ce locataire n'est pas le locataire assigné à ce bien", HttpStatus.BAD_REQUEST);
        }
        return locataire;
    }

    @Override
    public void verifierBienActif(Bien bien) {
        if (bien.isArchive()) {
            throw new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT);
        }
    }

    @Override
    public void verifierDocumentModifiable(Bien bien, User proprietaire, User locataire) {
        verifierBienActif(bien);
        if ((proprietaire != null && proprietaire.isAnonymise()) || (locataire != null && locataire.isAnonymise())) {
            throw new KupangaBusinessException(
                    "Une des parties a supprimé son compte : ce document ne peut plus être modifié", HttpStatus.CONFLICT);
        }
    }

    private static boolean estProprietaire(Bien bien, User user) {
        return bien.getProprietaire() != null && bien.getProprietaire().getId().equals(user.getId());
    }

    private static boolean concerne(User partie, User user) {
        return partie != null && partie.getId().equals(user.getId());
    }

    /**
     * Vue privée d'un bien (propriétaire ou locataire connecté) : parties avec e-mail,
     * documents filtrés selon l'utilisateur.
     */
    private BienDTO toPriveDTO(Bien bien, User user) {
        return BienDTO.builder()
                // ─── Informations générales ───────────────────────────────
                .id(bien.getId())
                .titre(bien.getTitre())
                .typeBien(bien.getTypeBien())
                .description(bien.getDescription())

                // ─── Adresse ──────────────────────────────────────────────
                .adresse(bien.getAdresse())
                .ville(bien.getVille())
                .codePostal(bien.getCodePostal())
                .pays(bien.getPays())
                .commune(bien.getCommune())
                .quartier(bien.getQuartier())
                .avenue(bien.getAvenue())
                .numeroParcelle(bien.getNumeroParcelle())
                .pointDeRepere(bien.getPointDeRepere())
                .latitude(bien.getLocalisation() != null
                        ? bien.getLocalisation().getY()
                        : null)
                .longitude(bien.getLocalisation() != null
                        ? bien.getLocalisation().getX()
                        : null)

                // ─── Caractéristiques physiques ───────────────────────────
                .surfaceHabitable(bien.getSurfaceHabitable())
                .nombrePieces(bien.getNombrePieces())
                .nombreChambres(bien.getNombreChambres())
                .etage(bien.getEtage())
                .ascenseur(bien.getAscenseur())
                .anneeConstruction(bien.getAnneeConstruction())
                .modeChauffage(bien.getModeChauffage())

                // ─── Diagnostic énergétique ───────────────────────────────
                .classeEnergie(bien.getClasseEnergie())
                .classeGes(bien.getClasseGes())

                // ─── Conditions de location ───────────────────────────────
                .loyerMensuel(bien.getLoyerMensuel())
                .chargesMensuelles(bien.getChargesMensuelles())
                .depotGarantie(bien.getDepotGarantie())
                .devise(bien.getDevise())
                .meuble(bien.getMeuble())
                .colocation(bien.getColocation())
                .disponibleDe(bien.getDisponibleDe())
                .archive(bien.isArchive())

                // ─── Parties ──────────────────────────────────────────────
                .proprietaire(bien.getProprietaire() != null
                        ? UserDTO.builder()
                        .id(bien.getProprietaire().getId())
                        .firstName(bien.getProprietaire().getFirstName())
                        .lastName(bien.getProprietaire().getLastName())
                        .mail(bien.getProprietaire().getMail())
                        .build()
                        : null)
                .locataire(bien.getLocataire() != null
                        ? UserDTO.builder()
                        .id(bien.getLocataire().getId())
                        .firstName(bien.getLocataire().getFirstName())
                        .lastName(bien.getLocataire().getLastName())
                        .mail(bien.getLocataire().getMail())
                        .build()
                        : null)

                // ─── Documents & médias ───────────────────────────────────
                // Le propriétaire voit tous les documents du bien ; un locataire ne voit que les siens
                // (pas ceux des locataires précédents)
                .contrats(bien.getContrats() != null
                        ? bien.getContrats().stream()
                        .filter(c -> estProprietaire(bien, user) || concerne(c.getLocataire(), user))
                        .map(c -> documentPdfUrlMapper.urlContrat(c.getClePdf()))
                        .filter(Objects::nonNull)
                        .toList()
                        : List.of())
                .quittances(bien.getQuittances() != null
                        ? bien.getQuittances().stream()
                        .filter(q -> estProprietaire(bien, user) || concerne(q.getLocataire(), user))
                        .map(q -> documentPdfUrlMapper.urlQuittance(q.getClePdf()))
                        .filter(Objects::nonNull)
                        .toList()
                        : List.of())
                .documents(bien.getDocuments() != null
                        ? bien.getDocuments().stream()
                        .map(Document::getUrl)
                        .filter(Objects::nonNull)
                        .toList()
                        : List.of())
                .images(bien.getImages() != null
                        ? bien.getImages().stream()
                        .map(BienImage::getUrl)
                        .filter(Objects::nonNull)
                        .toList()
                        : List.of())
                .pois(bien.getPois() != null
                        ? bien.getPois().stream()
                        .map(p -> p.getPoiType().getLabelFr())
                        .filter(Objects::nonNull)
                        .toList()
                        : List.of())

                // ─── Audit ────────────────────────────────────────────────
                .createdAt(bien.getCreatedAt())
                .updatedAt(bien.getUpdatedAt())

                .build();
    }

    /** Texte saisi, ou {@code null} s'il est vide (un champ vide n'est pas enregistré). */
    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
