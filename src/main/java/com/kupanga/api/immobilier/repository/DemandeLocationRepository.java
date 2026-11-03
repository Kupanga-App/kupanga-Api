package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.StatutDemandeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DemandeLocationRepository extends JpaRepository<DemandeLocation, Long> {

    @Query("""
            select d from DemandeLocation d
            join fetch d.bien join fetch d.proprietaire join fetch d.locataire
            where d.id = :id
            """)
    Optional<DemandeLocation> findAvecParties(@Param("id") Long id);

    /** Demandes reçues par un locataire, encore en attente et non expirées, les plus récentes d'abord. */
    @Query("""
            select d from DemandeLocation d
            join fetch d.bien join fetch d.proprietaire join fetch d.locataire
            where d.locataire.id = :locataireId and d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.EN_ATTENTE and d.dateExpiration > :maintenant
            order by d.dateCreation desc
            """)
    List<DemandeLocation> findRecuesEnAttente(@Param("locataireId") Long locataireId,
                                              @Param("maintenant") LocalDateTime maintenant);

    /** Historique des demandes d'un bien, les plus récentes d'abord. */
    @Query("""
            select d from DemandeLocation d
            join fetch d.bien join fetch d.proprietaire join fetch d.locataire
            where d.bien.id = :bienId
            order by d.dateCreation desc
            """)
    List<DemandeLocation> findDuBien(@Param("bienId") Long bienId);

    boolean existsByBien_IdAndStatut(Long bienId, StatutDemandeLocation statut);

    /** Passe en EXPIREE les demandes en attente d'un bien dont le délai est dépassé (libère l'index unique). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DemandeLocation d set d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.EXPIREE, d.version = d.version + 1
            where d.bien.id = :bienId and d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.EN_ATTENTE and d.dateExpiration <= :maintenant
            """)
    int expirerEchuesDuBien(@Param("bienId") Long bienId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : bien archivé, demande en attente annulée. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DemandeLocation d set d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.ANNULEE, d.dateReponse = :maintenant, d.version = d.version + 1
            where d.bien.id = :bienId and d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.EN_ATTENTE
            """)
    int annulerEnAttenteDuBien(@Param("bienId") Long bienId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : compte anonymisé, ses demandes en attente (envoyées ou reçues) sont annulées. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update DemandeLocation d set d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.ANNULEE, d.dateReponse = :maintenant, d.version = d.version + 1
            where (d.proprietaire.id = :userId or d.locataire.id = :userId) and d.statut = com.kupanga.api.immobilier.entity.StatutDemandeLocation.EN_ATTENTE
            """)
    int annulerEnAttenteDeUtilisateur(@Param("userId") Long userId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : compte supprimé, ses demandes sont supprimées (clés étrangères). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DemandeLocation d where d.proprietaire.id = :userId or d.locataire.id = :userId")
    int supprimerParUtilisateur(@Param("userId") Long userId);
}
