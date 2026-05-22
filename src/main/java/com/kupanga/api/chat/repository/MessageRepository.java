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
     * Marque tous les messages non lus d'une conversation comme lus.
     */
    @Modifying
    @Query("""
            UPDATE Message m SET m.lu = true
            WHERE m.destinataire.mail = :emailDestinataire
              AND m.expediteur.mail   = :emailExpediteur
              AND (m.lu = false OR m.lu IS NULL)
            """)
    void marquerConversationLue(@Param("emailDestinataire") String emailDestinataire,
                                @Param("emailExpediteur")   String emailExpediteur);

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
}
