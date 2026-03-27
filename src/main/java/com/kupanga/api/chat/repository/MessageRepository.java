package com.kupanga.api.chat.repository;


import com.kupanga.api.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Récupère la conversation complète entre deux utilisateurs,
     * triée par date croissante.
     */
    @Query("""
            SELECT m FROM Message m
            LEFT JOIN FETCH m.expediteur
            LEFT JOIN FETCH m.destinataire
            LEFT JOIN FETCH m.bien
            WHERE (m.expediteur.mail = :emailA AND m.destinataire.mail = :emailB)
               OR (m.expediteur.mail = :emailB AND m.destinataire.mail = :emailA)
            ORDER BY m.createdAt ASC
            """)
    List<Message> findConversation(@Param("emailA") String emailA,
                                   @Param("emailB") String emailB);

    /**
     * Récupère la conversation entre deux utilisateurs dans le contexte d'un bien.
     */
    @Query("""
            SELECT m FROM Message m
            LEFT JOIN FETCH m.expediteur
            LEFT JOIN FETCH m.destinataire
            LEFT JOIN FETCH m.bien
            WHERE m.bien.id = :bienId
              AND ((m.expediteur.mail = :emailA AND m.destinataire.mail = :emailB)
                OR (m.expediteur.mail = :emailB AND m.destinataire.mail = :emailA))
            ORDER BY m.createdAt ASC
            """)
    List<Message> findConversationParBien(@Param("emailA") String emailA,
                                          @Param("emailB") String emailB,
                                          @Param("bienId") Long bienId);

    /**
     * Marque tous les messages non lus d'une conversation comme lus.
     */
    @Modifying
    @Query("""
            UPDATE Message m SET m.lu = true
            WHERE m.destinataire.mail = :emailDestinataire
              AND m.expediteur.mail   = :emailExpediteur
              AND m.lu = false
            """)
    void marquerConversationLue(@Param("emailDestinataire") String emailDestinataire,
                                @Param("emailExpediteur")   String emailExpediteur);

    /**
     * Compte les messages non lus pour un utilisateur donné.
     */
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.destinataire.mail = :email
              AND m.lu = false
            """)
    Long countMessagesNonLus(@Param("email") String email);
}
