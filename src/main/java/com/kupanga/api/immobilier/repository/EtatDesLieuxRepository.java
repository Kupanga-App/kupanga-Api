package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.StatutEdl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EtatDesLieuxRepository extends JpaRepository<EtatDesLieux, Long>, JpaSpecificationExecutor<EtatDesLieux> {

    Optional<EtatDesLieux> findByTokenSignature(String tokenSignature);

    /** B7 : passe l'EDL en {@code EXPIRE} dans sa propre transaction (cf. {@code ContratRepository.marquerExpire}). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query("""
            UPDATE EtatDesLieux e SET e.statut = com.kupanga.api.immobilier.entity.StatutEdl.EXPIRE,
                                      e.version = e.version + 1
            WHERE e.id = :id
              AND e.version = :version
              AND e.statut = com.kupanga.api.immobilier.entity.StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE
            """)
    int marquerExpire(@Param("id") Long id, @Param("version") Long version);

    @Query("SELECT e FROM EtatDesLieux e LEFT JOIN FETCH e.locataire WHERE e.bien.id = :bienId ORDER BY e.createdAt DESC")
    List<EtatDesLieux> findByBienId(@Param("bienId") Long bienId);

    long countByBienId(Long bienId);

    @Query("SELECT e.bien.id, COUNT(e) FROM EtatDesLieux e GROUP BY e.bien.id")
    List<Object[]> countParBien();

    /**
     * Charge l'EDL avec toutes ses relations en une seule requête
     * pour éviter les N+1 lors de la génération PDF.
     */
    @Query("""
            SELECT e FROM EtatDesLieux e
            LEFT JOIN FETCH e.bien
            LEFT JOIN FETCH e.proprietaire
            LEFT JOIN FETCH e.locataire
            LEFT JOIN FETCH e.pieces p
            LEFT JOIN FETCH p.elements
            LEFT JOIN FETCH e.compteurs
            LEFT JOIN FETCH e.cles
            WHERE e.id = :id
            """)
    Optional<EtatDesLieux> findWithAllRelations(@Param("id") Long id);

    /** B12 : le compte est-il partie (propriétaire ou locataire) d'au moins un document ? */
    boolean existsByProprietaire_IdOrLocataire_Id(Long proprietaireId, Long locataireId);

    /**
     * B12 : passe en {@code EXPIRE} (lien de signature effacé) les documents non signés d'un compte supprimé :
     * le lien reçu par e-mail ne permet plus de signer. Version incrémentée (verrou optimiste, B6).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EtatDesLieux e SET e.statut = :expire, e.tokenSignature = null, e.version = e.version + 1
            WHERE (e.proprietaire.id = :userId OR e.locataire.id = :userId)
              AND e.statut <> :signe AND e.statut <> :expire
            """)
    int expirerNonSignesDeUtilisateur(@Param("userId") Long userId,
                                      @Param("signe") StatutEdl signe, @Param("expire") StatutEdl expire);

    /** B12 : idem pour les documents non signés d'un bien archivé. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EtatDesLieux e SET e.statut = :expire, e.tokenSignature = null, e.version = e.version + 1
            WHERE e.bien.id = :bienId
              AND e.statut <> :signe AND e.statut <> :expire
            """)
    int expirerNonSignesDuBien(@Param("bienId") Long bienId,
                               @Param("signe") StatutEdl signe, @Param("expire") StatutEdl expire);
}
