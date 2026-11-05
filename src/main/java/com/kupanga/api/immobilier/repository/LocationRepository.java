package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("""
            select l from Location l
            join fetch l.bien join fetch l.proprietaire join fetch l.locataire
            where l.id = :id
            """)
    Optional<Location> findAvecParties(@Param("id") Long id);

    /** Réservation encore valable d'un bien (au plus une, index V54). */
    @Query("""
            select l from Location l
            join fetch l.bien join fetch l.proprietaire join fetch l.locataire
            where l.bien.id = :bienId
              and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
              and l.dateExpiration > :maintenant
            """)
    Optional<Location> findReservationValable(@Param("bienId") Long bienId,
                                              @Param("maintenant") LocalDateTime maintenant);

    /** Location active d'un bien avec ses parties (fin de bail). */
    @Query("""
            select l from Location l
            join fetch l.bien join fetch l.proprietaire join fetch l.locataire
            where l.bien.id = :bienId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ACTIVE
            """)
    Optional<Location> findActiveAvecParties(@Param("bienId") Long bienId);

    /** Location active d'un bien (au plus une, index V54). */
    @Query("""
            select l from Location l
            where l.bien.id = :bienId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ACTIVE
            """)
    Optional<Location> findActive(@Param("bienId") Long bienId);

    /** Réservations encore valables d'un candidat, les plus récentes d'abord. */
    @Query("""
            select l from Location l
            join fetch l.bien join fetch l.proprietaire join fetch l.locataire
            where l.locataire.mail = :email
              and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
              and l.dateExpiration > :maintenant
            order by l.dateReservation desc
            """)
    List<Location> findReservationsDuLocataire(@Param("email") String email,
                                               @Param("maintenant") LocalDateTime maintenant);

    /** Réservations dont l'échéance est passée, à faire expirer (tâche planifiée). */
    @Query("""
            select l.id from Location l
            where l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE and l.dateExpiration <= :maintenant
            order by l.dateExpiration
            """)
    List<Long> findIdsReservationsEchues(@Param("maintenant") LocalDateTime maintenant);

    /** Réservations échues d'un bien, encore enregistrées RESERVEE (à faire expirer avant une nouvelle réservation). */
    @Query("""
            select l.id from Location l
            where l.bien.id = :bienId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
              and l.dateExpiration <= :maintenant
            """)
    List<Long> findIdsReservationsEchuesDuBien(@Param("bienId") Long bienId, @Param("maintenant") LocalDateTime maintenant);

    /**
     * Passe en EXPIREE les réservations échues d'un bien (libère l'index unique avant une nouvelle réservation).
     * Sans vidage du contexte : les entités de l'appelant (bien, demande) restent attachées.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Location l set l.statut = com.kupanga.api.immobilier.entity.StatutLocation.EXPIREE,
                                  l.dateFin = l.dateExpiration, l.version = l.version + 1
            where l.bien.id = :bienId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
              and l.dateExpiration <= :maintenant
            """)
    int expirerEchuesDuBien(@Param("bienId") Long bienId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : bien archivé, réservation annulée. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Location l set l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ANNULEE,
                                  l.dateFin = :maintenant, l.version = l.version + 1
            where l.bien.id = :bienId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
            """)
    int annulerReservationDuBien(@Param("bienId") Long bienId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : compte anonymisé, ses réservations (en tant que candidat ou propriétaire) sont annulées. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Location l set l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ANNULEE,
                                  l.dateFin = :maintenant, l.version = l.version + 1
            where (l.proprietaire.id = :userId or l.locataire.id = :userId)
              and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.RESERVEE
            """)
    int annulerReservationsDeUtilisateur(@Param("userId") Long userId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : locataire anonymisé, ses locations en cours sont terminées (le bien est libéré). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Location l set l.statut = com.kupanga.api.immobilier.entity.StatutLocation.TERMINEE,
                                  l.dateFin = :maintenant, l.version = l.version + 1
            where l.locataire.id = :userId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ACTIVE
            """)
    int terminerActivesDuLocataire(@Param("userId") Long userId, @Param("maintenant") LocalDateTime maintenant);

    /**
     * B12 / fin de bail : propriétaire anonymisé, ses locations en cours sont terminées (personne ne pourrait plus
     * enregistrer la fin du bail ; le locataire garde ses documents).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Location l set l.statut = com.kupanga.api.immobilier.entity.StatutLocation.TERMINEE,
                                  l.dateFin = :maintenant, l.version = l.version + 1
            where l.proprietaire.id = :userId and l.statut = com.kupanga.api.immobilier.entity.StatutLocation.ACTIVE
            """)
    int terminerActivesDuProprietaire(@Param("userId") Long userId, @Param("maintenant") LocalDateTime maintenant);

    /** B12 : compte supprimé, ses locations sont supprimées (clés étrangères). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Location l where l.proprietaire.id = :userId or l.locataire.id = :userId")
    int supprimerParUtilisateur(@Param("userId") Long userId);
}
