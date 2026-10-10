package com.kupanga.api.backoffice.service;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.notification.entity.Notification;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.repository.NotificationRepository;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;

/**
 * B12 sur base réelle (contraintes de clés étrangères, requêtes de mise à jour en masse) :
 * un compte sans donnée liée est supprimé avec ses conversations ; un compte qui a un bail est anonymisé,
 * ses biens archivés, et aucun document n'est perdu.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("Tests d'intégration — suppression et anonymisation des comptes (B12)")
class SuppressionCompteIntegrationTest {

    @Autowired private UserAdminService userAdminService;
    @Autowired private BienAdminService bienAdminService;
    @Autowired private UserRepository userRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private ContratRepository contratRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EntityManager entityManager;

    private User proprio;
    private User locataire;
    private User candidat;
    private Bien bienLoue;
    private Bien bienLibre;
    private Contrat contrat;
    private Conversation conversationCandidat;
    private Conversation conversationLocataire;

    @BeforeEach
    void setUp() {
        proprio = userRepository.save(User.builder().mail("proprio@b12.test").firstName("Paul").lastName("Proprio")
                .password("hash").role(Role.ROLE_PROPRIETAIRE).emailVerifie(true).build());
        locataire = userRepository.save(User.builder().mail("locataire@b12.test").firstName("Lea").lastName("Loc")
                .password("hash").role(Role.ROLE_LOCATAIRE).emailVerifie(true).build());
        candidat = userRepository.save(User.builder().mail("candidat@b12.test").firstName("Carl").lastName("Cand")
                .password("hash").role(Role.ROLE_LOCATAIRE).emailVerifie(true).build());

        bienLoue = bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("Loué").typeBien(TypeBien.APPARTEMENT)
                .proprietaire(proprio).locataire(locataire).build());
        bienLibre = bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("Libre").typeBien(TypeBien.MAISON)
                .proprietaire(proprio).build());

        contrat = contratRepository.save(Contrat.builder().pays(Pays.FR).devise(Devise.EUR).modeleVersion("fr-v1")
                .bien(bienLoue).proprietaire(proprio).locataire(locataire)
                .dateDebut(LocalDate.now()).dureeBailMois(12).loyerMensuel(new BigDecimal("800.0"))
                .statut(StatutContrat.SIGNE)
                .build());

        conversationCandidat = conversation(bienLibre, candidat, proprio);
        message(candidat, proprio, conversationCandidat);
        message(proprio, candidat, conversationCandidat);

        conversationLocataire = conversation(bienLoue, locataire, proprio);
        message(locataire, proprio, conversationLocataire);

        notificationRepository.save(Notification.builder().destinataire(candidat)
                .type(NotificationType.BIEN_ASSIGNE).titre("t").message("m").build());
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Compte sans donnée liée → supprimé avec ses conversations et messages ; rien d'autre ne bouge")
    void compteSansDonnees_supprime() {
        assertThat(userAdminService.supprimer(candidat.getId())).isEqualTo(ResultatSuppressionCompte.SUPPRIME);
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(candidat.getId())).isEmpty();
        assertThat(conversationRepository.findById(conversationCandidat.getId())).isEmpty();
        assertThat(messageRepository.count()).as("seul le message du locataire reste").isEqualTo(1);
        assertThat(notificationRepository.count()).isZero();
        assertThat(bienRepository.findById(bienLibre.getId()).orElseThrow().isArchive()).isFalse();
        assertThat(conversationRepository.findById(conversationLocataire.getId())).isPresent();
    }

    @Test
    @DisplayName("Propriétaire avec un bail → anonymisé ; biens archivés ; contrat, conversations et messages conservés")
    void proprietaireAvecBail_anonymise() {
        assertThat(userAdminService.supprimer(proprio.getId())).isEqualTo(ResultatSuppressionCompte.ANONYMISE);
        entityManager.flush();
        entityManager.clear();

        User anonyme = userRepository.findById(proprio.getId()).orElseThrow();
        String mailAnonyme = anonyme.getMail();
        assertThat(anonyme.isAnonymise()).isTrue();
        assertThat(mailAnonyme).endsWith("@anonyme.invalid");
        assertThat(anonyme.getFirstName()).isEqualTo("Utilisateur");
        assertThat(anonyme.getPassword()).isNull();
        assertThat(userRepository.findByMail("proprio@b12.test")).isEmpty();

        assertThat(bienRepository.findById(bienLoue.getId()).orElseThrow().isArchive()).isTrue();
        assertThat(bienRepository.findById(bienLibre.getId()).orElseThrow().isArchive()).isTrue();
        assertThat(bienRepository.findById(bienLoue.getId()).orElseThrow().getDateArchivage()).isNotNull();

        Contrat relu = contratRepository.findById(contrat.getId()).orElseThrow();
        assertThat(relu.getProprietaire().getId()).isEqualTo(proprio.getId());
        assertThat(relu.getLocataire().getId()).isEqualTo(locataire.getId());

        assertThat(messageRepository.count()).isEqualTo(3);
        Conversation conv = conversationRepository.findById(conversationCandidat.getId()).orElseThrow();
        assertThat(conv.getEmailDestinataire()).isEqualTo(mailAnonyme);
        assertThat(conv.getEmailExpediteur()).isEqualTo("candidat@b12.test");

        assertThat(bienAdminService.desarchiver(bienLibre.getId()))
                .as("pas de remise en ligne sans propriétaire").isFalse();
    }

    @Test
    @DisplayName("Locataire avec un bail → anonymisé ; le bien est libéré, le contrat garde le locataire")
    void locataireAvecBail_anonymise() {
        assertThat(userAdminService.supprimer(locataire.getId())).isEqualTo(ResultatSuppressionCompte.ANONYMISE);
        entityManager.flush();
        entityManager.clear();

        Bien bien = bienRepository.findById(bienLoue.getId()).orElseThrow();
        assertThat(bien.getLocataire()).isNull();
        assertThat(bien.isArchive()).as("le bien du propriétaire reste en ligne").isFalse();
        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getLocataire().getId())
                .isEqualTo(locataire.getId());
        assertThat(conversationRepository.findById(conversationLocataire.getId()).orElseThrow().getEmailExpediteur())
                .isEqualTo(userRepository.findById(locataire.getId()).orElseThrow().getMail());
    }

    @Test
    @DisplayName("Contrat en attente de signature → passé en EXPIRE, lien effacé, à l'anonymisation et à l'archivage")
    void signaturesEnCours_lienInvalide() {
        Contrat enAttente = contratRepository.save(Contrat.builder().pays(Pays.FR).devise(Devise.EUR).modeleVersion("fr-v1")
                .bien(bienLoue).proprietaire(proprio).locataire(locataire)
                .dateDebut(LocalDate.now()).dureeBailMois(12).loyerMensuel(new BigDecimal("800.0"))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE).tokenSignature("lien-b12")
                .build());
        Contrat aSigner = contratRepository.save(Contrat.builder().pays(Pays.FR).devise(Devise.EUR).modeleVersion("fr-v1")
                .bien(bienLibre).proprietaire(proprio).locataire(candidat)
                .dateDebut(LocalDate.now()).dureeBailMois(12).loyerMensuel(new BigDecimal("500.0"))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE).tokenSignature("lien-b12-archive")
                .build());
        entityManager.flush();
        entityManager.clear();

        userAdminService.supprimer(locataire.getId());
        bienAdminService.archiver(bienLibre.getId());
        entityManager.flush();
        entityManager.clear();

        for (Long id : new Long[]{enAttente.getId(), aSigner.getId()}) {
            Contrat relu = contratRepository.findById(id).orElseThrow();
            assertThat(relu.getStatut()).isEqualTo(StatutContrat.EXPIRE);
            assertThat(relu.getTokenSignature()).isNull();
        }
        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getStatut())
                .as("un bail signé n'est jamais touché").isEqualTo(StatutContrat.SIGNE);
        assertThat(contratRepository.findByTokenSignature("lien-b12")).isEmpty();
    }

    @Test
    @DisplayName("Bien archivé depuis le back-office → toujours en base avec son contrat")
    void archiverBien_conserveDocuments() {
        assertThat(bienAdminService.archiver(bienLoue.getId())).isTrue();
        entityManager.flush();
        entityManager.clear();

        assertThat(bienRepository.findById(bienLoue.getId()).orElseThrow().isArchive()).isTrue();
        assertThat(contratRepository.findById(contrat.getId())).isPresent();
        assertThat(conversationRepository.findById(conversationLocataire.getId())).isPresent();
    }

    private Conversation conversation(Bien bien, User a, User b) {
        return conversationRepository.save(Conversation.builder()
                .bien(bien).emailExpediteur(a.getMail()).emailDestinataire(b.getMail()).build());
    }

    private void message(User expediteur, User destinataire, Conversation conversation) {
        messageRepository.save(Message.builder().contenu("Bonjour").expediteur(expediteur)
                .destinataire(destinataire).conversation(conversation).lu(false).build());
    }
}
