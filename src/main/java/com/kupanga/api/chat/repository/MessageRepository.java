package com.kupanga.api.chat.repository;


import com.kupanga.api.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
