package com.kupanga.api.immobilier.repository;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;

/**
 * Sur base réelle, sans transaction de test (chaque appel au dépôt est validé, comme en production) :
 * <ul>
 *   <li>B7 : {@code marquerExpire} enregistre {@code EXPIRE} dans sa propre transaction, seulement si le document
 *       attend la signature du locataire ;</li>
 *   <li>B6 : le verrou optimiste refuse d'écrire une version périmée (relance pendant la signature du locataire).</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Tests d'intégration — expiration et verrou optimiste des signatures (B6, B7)")
class SignatureContratRepositoryTest {

    @Autowired private ContratRepository contratRepository;
    @Autowired private EtatDesLieuxRepository edlRepository;
    @Autowired private BienRepository bienRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private Contrat contrat;
    private EtatDesLieux edl;

    @BeforeEach
    void setUp() {
        User proprio = userRepository.save(User.builder().mail("proprio@b7.test").role(Role.ROLE_PROPRIETAIRE).build());
        User locataire = userRepository.save(User.builder().mail("locataire@b7.test").role(Role.ROLE_LOCATAIRE).build());
        Bien bien = bienRepository.save(Bien.builder().pays(Pays.FR).devise(Devise.EUR).titre("B7").typeBien(TypeBien.APPARTEMENT)
                .proprietaire(proprio).locataire(locataire).build());

        contrat = contratRepository.save(Contrat.builder().pays(Pays.FR).devise(Devise.EUR).modeleVersion("fr-v1")
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .dateDebut(LocalDate.now()).dureeBailMois(12).loyerMensuel(new BigDecimal("800.0"))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE)
                .build());
        edl = edlRepository.save(EtatDesLieux.builder().pays(Pays.FR).modeleVersion("fr-v1")
                .bien(bien).proprietaire(proprio).locataire(locataire)
                .type(TypeEtat.ENTREE).dateRealisation(LocalDate.now())
                .statut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE)
                .build());
    }

    @AfterEach
    void nettoyer() {
        edlRepository.deleteAll();
        contratRepository.deleteAll();
        bienRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("B7 : marquerExpire enregistre EXPIRE (contrat et EDL) et incrémente la version")
    void marquerExpire_enregistre() {
        assertThat(contratRepository.marquerExpire(contrat.getId(), contrat.getVersion())).isEqualTo(1);
        assertThat(edlRepository.marquerExpire(edl.getId(), edl.getVersion())).isEqualTo(1);

        Contrat relu = contratRepository.findById(contrat.getId()).orElseThrow();
        assertThat(relu.getStatut()).isEqualTo(StatutContrat.EXPIRE);
        assertThat(relu.getVersion()).isEqualTo(contrat.getVersion() + 1);
        assertThat(edlRepository.findById(edl.getId()).orElseThrow().getStatut()).isEqualTo(StatutEdl.EXPIRE);
    }

    @Test
    @DisplayName("B7 : un contrat déjà SIGNE n'est jamais passé en EXPIRE")
    void marquerExpire_contratSigne_inchange() {
        contrat.setStatut(StatutContrat.SIGNE);
        contrat = contratRepository.save(contrat);

        assertThat(contratRepository.marquerExpire(contrat.getId(), contrat.getVersion())).isZero();
        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.SIGNE);
    }

    @Test
    @DisplayName("B7 : ancien lien expiré ouvert pendant une relance du propriétaire (version changée) → nouveau lien conservé")
    void marquerExpire_versionPerimee_inchange() {
        Long versionLueParLeLocataire = contrat.getVersion();
        contrat.setTokenSignature("nouveau-lien");
        contrat = contratRepository.save(contrat); // relance : version + 1

        assertThat(contratRepository.marquerExpire(contrat.getId(), versionLueParLeLocataire)).isZero();
        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getStatut())
                .isEqualTo(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
    }

    @Test
    @DisplayName("B7 : EXPIRE reste enregistré quand la transaction appelante est annulée (refus 410)")
    void marquerExpire_surviAuRollbackDeLAppelant() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(statut -> {
            Contrat lu = contratRepository.findById(contrat.getId()).orElseThrow();
            contratRepository.marquerExpire(lu.getId(), lu.getVersion());
            throw new IllegalStateException("refus 410");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.EXPIRE);
    }

    @Test
    @DisplayName("B6 : signature du locataire puis relance du propriétaire sur une copie périmée → refusée, SIGNE conservé")
    void verrouOptimiste_copiePerimee_refusee() {
        Contrat vuParLeLocataire = contratRepository.findById(contrat.getId()).orElseThrow();
        Contrat vuParLeProprietaire = contratRepository.findById(contrat.getId()).orElseThrow();

        vuParLeLocataire.setStatut(StatutContrat.SIGNE);
        contratRepository.saveAndFlush(vuParLeLocataire);

        vuParLeProprietaire.setSignatureProprietaire("relance");
        assertThatThrownBy(() -> contratRepository.saveAndFlush(vuParLeProprietaire))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(contratRepository.findById(contrat.getId()).orElseThrow().getStatut()).isEqualTo(StatutContrat.SIGNE);
    }
}
