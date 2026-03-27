package com.kupanga.api.chat.repository;

import com.kupanga.api.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation , Long> {

    @Query("""
   
            Select c
            from Conversation c
            inner join c.bien b
            where b.id = :bienId
            and c.emailExpediteur = :email
   
    """)
    Optional<Conversation> findConversationWithBienIdAndEmailExpediteur(Long bienId , String email);
}
