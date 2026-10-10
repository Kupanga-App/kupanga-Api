package com.kupanga.api.chat.repository;


import com.kupanga.api.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Marque comme lus les messages reçus par {@code emailDestinataire} dans une seule conversation (W6 :
     * avant, le filtre ne portait que sur l'expéditeur et touchait aussi ses conversations sur d'autres biens).
     */
    @Modifying
    @Query("""
            UPDATE Message m SET m.lu = true
            WHERE m.conversation.id    = :conversationId
              AND m.destinataire.mail = :emailDestinataire
              AND (m.lu = false OR m.lu IS NULL)
            """)
    int marquerConversationLue(@Param("conversationId")    Long conversationId,
                               @Param("emailDestinataire") String emailDestinataire);

    /**
     * Compte les messages non lus pour un utilisateur donné.
     */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.destinataire.mail = :email
              AND (m.lu = false OR m.lu IS NULL)
            """)
    Long countMessagesNonLus(@Param("email") String email);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversation.id = :conversationId
              AND m.destinataire.mail = :email
              AND (m.lu = false OR m.lu IS NULL)
            """)
    long countNonLuByConversationAndDestinataire(@Param("conversationId") Long conversationId,
                                                 @Param("email") String email);

    @Query("""
    SELECT m FROM Message m
    WHERE m.conversation.bien.id = :bienId
      AND (
        (m.expediteur.mail = :emailA AND m.destinataire.mail = :emailB)
        OR
        (m.expediteur.mail = :emailB AND m.destinataire.mail = :emailA)
      )
    ORDER BY m.createdAt ASC
    """)
    List<Message> findHistorique(@Param("bienId") Long bienId,
                                 @Param("emailA") String emailA,
                                 @Param("emailB") String emailB);

    /** B12 : supprime les messages envoyés ou reçus par un compte supprimé. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Message m WHERE m.expediteur.id = :userId OR m.destinataire.id = :userId")
    int supprimerParUtilisateur(@Param("userId") Long userId);
}
