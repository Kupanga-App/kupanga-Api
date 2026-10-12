package com.kupanga.api.immobilier.repository;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.juridiction.Pays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BienRepository extends JpaRepository<Bien, Long>, JpaSpecificationExecutor<Bien> {

    @Query(
            """
            select b
            from Bien b
            inner join fetch b.proprietaire
            left join fetch b.locataire
            left join fetch b.contrats
            left join fetch b.quittances
            left join fetch b.etatsDesLieux
            left join fetch b.documents
            left join fetch b.images
            where b.id = :id
            """
    )
    Optional<Bien> findWithAllProperties(Long id);

    @Query(
            """
            select b
            from Bien b
            inner join b.proprietaire p
            left join b.locataire l
            where p.id = :userId or l.id = :userId
            """
    )
    List<Bien> findAllPropertiesAssociateToUser(Long userId);

    boolean existsByIdAndProprietaireId(Long id, Long proprietaireId);

    @Query("SELECT COUNT(DISTINCT b.ville) FROM Bien b WHERE b.ville IS NOT NULL")
    long countDistinctVilles();

    @Query("SELECT b.ville, COUNT(b) FROM Bien b WHERE b.ville IS NOT NULL GROUP BY b.ville ORDER BY COUNT(b) DESC")
    List<Object[]> countParVille();

    @Query("SELECT b.typeBien, COUNT(b) FROM Bien b GROUP BY b.typeBien")
    List<Object[]> countParType();

    /** J2 : pays des biens enregistrés (contrôle des profils de juridiction au démarrage). */
    @Query("SELECT DISTINCT b.pays FROM Bien b")
    List<Pays> findPaysUtilises();

    /** B12 : le compte possède ou loue-t-il au moins un bien ? */
    boolean existsByProprietaire_IdOrLocataire_Id(Long proprietaireId, Long locataireId);

    /** B12 : archive tous les biens encore actifs d'un propriétaire (compte anonymisé). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Bien b SET b.archive = true, b.dateArchivage = :date WHERE b.proprietaire.id = :userId AND b.archive = false")
    int archiverParProprietaire(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    /** B12 : libère les biens loués par un compte anonymisé. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Bien b SET b.locataire = null WHERE b.locataire.id = :userId")
    int retirerLocataire(@Param("userId") Long userId);
}
