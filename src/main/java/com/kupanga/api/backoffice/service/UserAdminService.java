package com.kupanga.api.backoffice.service;

import com.kupanga.api.authentification.entity.RefreshToken;
import com.kupanga.api.authentification.repository.JetonVerificationEmailRepository;
import com.kupanga.api.authentification.repository.PasswordResetTokenRepository;
import com.kupanga.api.authentification.repository.RefreshTokenRepository;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.backoffice.dto.UserAdminDTO;
import com.kupanga.api.backoffice.dto.UserAdminPageDTO;
import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.backoffice.specification.UserAdminSpecification;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.config.ApresCommit;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.kupanga.api.minio.constant.MinioConstant.PHOTO_PROFIL_BUCKET;

/**
 * Service d'administration des utilisateurs.
 * Fournit les opérations de recherche paginée, suppression (ou anonymisation) et statistiques pour le back-office.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    /**
     * {@code .invalid} (RFC 2606) : aucune adresse de ce domaine ne reçoit d'e-mail. La partie locale est aléatoire
     * pour qu'un tiers ne puisse pas inscrire l'adresse à l'avance (index unique : l'anonymisation échouerait).
     */
    static final String DOMAINE_ANONYME = "anonyme.invalid";
    static final String PRENOM_ANONYME  = "Utilisateur";
    static final String NOM_ANONYME     = "supprimé";

    private final UserRepository            userRepository;
    private final UserAdminSpecification    userAdminSpecification;
    private final RefreshTokenRepository    refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationRepository   notificationRepository;
    private final JetonVerificationEmailRepository jetonVerificationEmailRepository;
    private final BienRepository            bienRepository;
    private final ContratRepository         contratRepository;
    private final QuittanceRepository       quittanceRepository;
    private final EtatDesLieuxRepository    etatDesLieuxRepository;
    private final MessageRepository         messageRepository;
    private final ConversationRepository    conversationRepository;
    private final MinioService              minioService;

    /**
     * Recherche paginée des utilisateurs selon les critères admin.
     *
     * @param dto critères de recherche, tri et pagination
     * @return page d'utilisateurs correspondant aux critères
     */
    @Transactional(readOnly = true)
    public UserAdminPageDTO rechercher(UserAdminSearchDTO dto) {
        Pagination pagination = dto.toPagination();
        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );
        Page<UserAdminDTO> page = userRepository
                .findAll(userAdminSpecification.build(dto), pageable)
                .map(UserAdminDTO::from);
        return UserAdminPageDTO.from(page);
    }

    /**
     * B12 : supprime un compte, ou l'anonymise s'il a des données liées.
     * <ul>
     *   <li>sans bien, contrat, quittance ni EDL : le compte est supprimé avec ses conversations et ses messages ;</li>
     *   <li>sinon : nom, e-mail, mot de passe, Google et photo sont effacés (connexion impossible), ses biens sont
     *       archivés, les biens qu'il loue sont libérés ; les baux, quittances, EDL et messages sont conservés.</li>
     * </ul>
     * Dans les deux cas, ses jetons (refresh, reset, vérification) et ses notifications sont supprimés.
     *
     * @param id identifiant de l'utilisateur
     * @return ce qui a été fait
     */
    @Transactional
    public ResultatSuppressionCompte supprimer(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResultatSuppressionCompte.INTROUVABLE;
        }
        if (user.isAnonymise()) {
            return ResultatSuppressionCompte.DEJA_ANONYMISE;
        }
        String mail = user.getMail();
        String photo = user.getUrlProfile();

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(id);
        if (refreshToken != null) {
            refreshTokenRepository.delete(refreshToken);
        }
        passwordResetTokenRepository.deleteByUserId(id);
        jetonVerificationEmailRepository.deleteByUserId(id);
        notificationRepository.deleteByDestinataireId(id);

        if (!aDesDonneesLiees(id)) {
            messageRepository.supprimerParUtilisateur(id);
            conversationRepository.supprimerParEmail(mail);
            userRepository.deleteById(id);
            supprimerPhotoApresCommit(photo, id);
            return ResultatSuppressionCompte.SUPPRIME;
        }

        LocalDateTime maintenant = LocalDateTime.now();
        String mailAnonyme = "supprime-" + UUID.randomUUID() + "@" + DOMAINE_ANONYME;
        // Liens de signature en cours invalidés (la personne supprimée ne signe plus, rien ne lui est envoyé)
        contratRepository.expirerNonSignesDeUtilisateur(id, StatutContrat.SIGNE, StatutContrat.EXPIRE);
        etatDesLieuxRepository.expirerNonSignesDeUtilisateur(id, StatutEdl.SIGNE, StatutEdl.EXPIRE);
        bienRepository.archiverParProprietaire(id, maintenant);
        bienRepository.retirerLocataire(id);
        conversationRepository.remplacerEmail(mail, mailAnonyme);

        // Relu après les mises à jour en masse (elles vident le contexte de persistance)
        User anonyme = userRepository.findById(id).orElseThrow();
        anonyme.setFirstName(PRENOM_ANONYME);
        anonyme.setLastName(NOM_ANONYME);
        anonyme.setMail(mailAnonyme);
        anonyme.setPassword(null);
        anonyme.setGoogleId(null);
        anonyme.setUrlProfile(null);
        anonyme.setEmailVerifie(false);
        anonyme.setAnonymise(true);
        anonyme.setDateAnonymisation(maintenant);
        userRepository.save(anonyme);
        supprimerPhotoApresCommit(photo, id);
        return ResultatSuppressionCompte.ANONYMISE;
    }

    /**
     * La photo de profil est dans un bucket public : supprimée une fois la transaction validée,
     * sauf si un autre compte pointe vers le même objet (avatar choisi par URL).
     */
    private void supprimerPhotoApresCommit(String photo, Long id) {
        if (photo == null || userRepository.existsByUrlProfileAndIdNot(photo, id)) {
            return;
        }
        ApresCommit.executer(() -> minioService.supprimerParUrl(photo, PHOTO_PROFIL_BUCKET));
    }

    private boolean aDesDonneesLiees(Long id) {
        return bienRepository.existsByProprietaire_IdOrLocataire_Id(id, id)
                || contratRepository.existsByProprietaire_IdOrLocataire_Id(id, id)
                || quittanceRepository.existsByProprietaire_IdOrLocataire_Id(id, id)
                || etatDesLieuxRepository.existsByProprietaire_IdOrLocataire_Id(id, id);
    }

    /**
     * Retourne le nombre total d'utilisateurs enregistrés.
     *
     * @return nombre total d'utilisateurs
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return userRepository.count();
    }

    /**
     * Retourne le nombre d'utilisateurs ayant un rôle donné.
     *
     * @param role le rôle à compter
     * @return nombre d'utilisateurs pour ce rôle
     */
    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
