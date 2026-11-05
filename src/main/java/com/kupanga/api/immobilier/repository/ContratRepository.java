package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long>, JpaSpecificationExecutor<Contrat> {

    Optional<Contrat> findByTokenSignature(String token);

    /** Fin de bail : dernier bail signé du locataire sur le bien (dépôt de garantie à restituer). */
    Optional<Contrat> findFirstByBien_IdAndLocataire_IdAndStatutOrderByCreatedAtDesc(
            Long bienId, Long locataireId, StatutContrat statut);

    /**
     * B7 : passe le contrat en {@code EXPIRE} dans sa propre transaction : le refus (410) qui suit annule
     * la transaction de l'appelant, et avec elle toute modification faite sur l'entité.
     * Filtré sur la version lue : une relance du propriétaire entre-temps (nouveau lien) n'est pas expirée.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query("""
            UPDATE Contrat c SET c.statut = com.kupanga.api.immobilier.entity.StatutContrat.EXPIRE,
                                 c.version = c.version + 1
            WHERE c.id = :id
              AND c.version = :version
              AND c.statut = com.kupanga.api.immobilier.entity.StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE
            """)
    int marquerExpire(@Param("id") Long id, @Param("version") Long version);

    @Query("SELECT c FROM Contrat c LEFT JOIN FETCH c.locataire WHERE c.bien.id = :bienId ORDER BY c.createdAt DESC")
    List<Contrat> findByBienId(@Param("bienId") Long bienId);

    long countByBienId(Long bienId);

    @Query("SELECT c.bien.id, COUNT(c) FROM Contrat c GROUP BY c.bien.id")
    List<Object[]> countParBien();

    /** B12 : le compte est-il partie (propriétaire ou locataire) d'au moins un document ? */
    boolean existsByProprietaire_IdOrLocataire_Id(Long proprietaireId, Long locataireId);

    /**
     * B12 : passe en {@code EXPIRE} (lien de signature effacé) les documents non signés d'un compte supprimé :
     * le lien reçu par e-mail ne permet plus de signer. Version incrémentée (verrou optimiste, B6).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Contrat c SET c.statut = :expire, c.tokenSignature = null, c.version = c.version + 1
            WHERE (c.proprietaire.id = :userId OR c.locataire.id = :userId)
              AND c.statut <> :signe AND c.statut <> :expire
            """)
    int expirerNonSignesDeUtilisateur(@Param("userId") Long userId,
                                      @Param("signe") StatutContrat signe, @Param("expire") StatutContrat expire);

    /** B12 : idem pour les documents non signés d'un bien archivé. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Contrat c SET c.statut = :expire, c.tokenSignature = null, c.version = c.version + 1
            WHERE c.bien.id = :bienId
              AND c.statut <> :signe AND c.statut <> :expire
            """)
    int expirerNonSignesDuBien(@Param("bienId") Long bienId,
                               @Param("signe") StatutContrat signe, @Param("expire") StatutContrat expire);

    /**
     * Réservation désistée, retirée ou expirée (décision du 2026-11-05) : les baux non signés du candidat sur ce
     * bien passent en {@code ANNULE} et leur lien de signature est effacé. Version incrémentée : une signature en
     * cours sur l'entité échoue au verrou optimiste (409). Contexte vidé ensuite : un bail déjà chargé dans la
     * transaction n'est jamais relu avec son ancien statut.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Contrat c SET c.statut = com.kupanga.api.immobilier.entity.StatutContrat.ANNULE,
                                 c.tokenSignature = null, c.tokenExpiration = null, c.version = c.version + 1
            WHERE c.bien.id = :bienId AND c.locataire.id = :locataireId
              AND c.statut <> com.kupanga.api.immobilier.entity.StatutContrat.SIGNE
              AND c.statut <> com.kupanga.api.immobilier.entity.StatutContrat.ANNULE
            """)
    int annulerNonSignesDuCandidat(@Param("bienId") Long bienId, @Param("locataireId") Long locataireId);
}
