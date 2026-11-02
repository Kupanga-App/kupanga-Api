package com.kupanga.api.email.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface EmailEnFileRepository extends JpaRepository<EmailEnFile, Long> {

    /**
     * M2 : réserve un lot d'e-mails dus (en attente, ou réservés par un envoyeur dont le verrou a expiré) et les
     * passe {@code EN_COURS} jusqu'à {@code verrou}. {@code FOR UPDATE SKIP LOCKED} : deux envoyeurs (deux threads,
     * deux instances) ne prennent jamais la même ligne. Les e-mails critiques passent en premier.
     * <p>À appeler dans une transaction (validée avant l'envoi).
     *
     * @return les identifiants réservés
     */
    @Query(value = """
            UPDATE emails_sortants SET statut = 'EN_COURS', verrou_jusqua = :verrou
            WHERE id IN (
                SELECT id FROM emails_sortants
                WHERE statut IN ('A_ENVOYER', 'EN_COURS')
                  AND prochain_essai <= :maintenant
                  AND (verrou_jusqua IS NULL OR verrou_jusqua < :maintenant)
                ORDER BY CASE priorite WHEN 'CRITIQUE' THEN 0 ELSE 1 END, prochain_essai, id
                LIMIT :lot
                FOR UPDATE SKIP LOCKED)
            RETURNING id
            """, nativeQuery = true)
    List<Long> reserver(@Param("maintenant") Instant maintenant, @Param("verrou") Instant verrou, @Param("lot") int lot);

    /** RGPD : contenu des e-mails non envoyés (échec, périmé) effacé après la durée de conservation. */
    @Modifying
    @Query("""
            UPDATE EmailEnFile e SET e.html = null, e.texte = null, e.contenuEfface = true
            WHERE e.creeLe < :avant AND e.contenuEfface = false AND e.statut IN :statuts
            """)
    int effacerContenu(@Param("avant") Instant avant, @Param("statuts") Collection<StatutEmail> statuts);

    /** Revue M2 : contenu (liens avec jeton) des e-mails non envoyés effacé dès l'expiration de leur lien. */
    @Modifying
    @Query("""
            UPDATE EmailEnFile e SET e.html = null, e.texte = null, e.contenuEfface = true
            WHERE e.expireLe < :maintenant AND e.contenuEfface = false AND e.statut IN :statuts
            """)
    int effacerContenuExpire(@Param("maintenant") Instant maintenant, @Param("statuts") Collection<StatutEmail> statuts);

    /** B12 / revue M2 : e-mails (en attente ou passés) d'une adresse, supprimés avec le compte. */
    List<EmailEnFile> findByDestinataire(String destinataire);

    // ─── M4 : back-office ─────────────────────────────────────────────────────
    Page<EmailEnFile> findByStatut(StatutEmail statut, Pageable pageable);

    Page<EmailEnFile> findByType(TypeEmail type, Pageable pageable);

    Page<EmailEnFile> findByStatutAndType(StatutEmail statut, TypeEmail type, Pageable pageable);

    long countByStatut(StatutEmail statut);

    /** RGPD : identifiants des e-mails terminés plus anciens que la durée de conservation. */
    @Query("SELECT e.id FROM EmailEnFile e WHERE e.creeLe < :avant AND e.statut IN :statuts")
    List<Long> terminesAvant(@Param("avant") Instant avant, @Param("statuts") Collection<StatutEmail> statuts);
}
