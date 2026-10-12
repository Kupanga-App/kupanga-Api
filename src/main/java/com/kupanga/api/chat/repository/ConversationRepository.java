package com.kupanga.api.chat.repository;

import com.kupanga.api.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation , Long> , JpaSpecificationExecutor<Conversation> {

    @Query("""
   
            Select c
            from Conversation c
            inner join c.bien b
            where b.id = :bienId
            and (
            (c.emailExpediteur = :emailA and c.emailDestinataire = :emailB )
            or (c.emailExpediteur = :emailB and c.emailDestinataire = :emailA )
            )
   
    """)
    Optional<Conversation> findConversationWithBienIdAndEmailExpediteur(Long bienId , String emailA , String emailB);

    /** B12 : supprime les conversations d'un compte supprimé (ses messages doivent l'être avant). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Conversation c WHERE c.emailExpediteur = :email OR c.emailDestinataire = :email")
    int supprimerParEmail(@Param("email") String email);

    /** B12 : remplace l'adresse d'un compte anonymisé dans ses conversations. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Conversation c SET
                c.emailExpediteur   = CASE WHEN c.emailExpediteur   = :ancien THEN :nouveau ELSE c.emailExpediteur END,
                c.emailDestinataire = CASE WHEN c.emailDestinataire = :ancien THEN :nouveau ELSE c.emailDestinataire END
            WHERE c.emailExpediteur = :ancien OR c.emailDestinataire = :ancien
            """)
    int remplacerEmail(@Param("ancien") String ancien, @Param("nouveau") String nouveau);
}
