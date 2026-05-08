package com.kupanga.api.chat.research.specification;

import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.immobilier.entity.Bien;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ConversationSpecification {

    /**
     * Construit une Specification JPA a partir des criteres de recherche fournis.
     *
     * @param email email de l'utilisateur connecte
     * @param dto   criteres de recherche dynamiques
     * @return la specification combinee
     */
    public Specification<Conversation> build(String email, ConversationSearchDTO dto) {
        return Specification
                .where(pourUtilisateur(email))
                .and(parNomDuBien(dto.nomDuBien()))
                .and(parStatutLecture(email, dto.lu()));
    }

    /**
     * Filtre les conversations dans lesquelles l'utilisateur connecte participe.
     */
    private Specification<Conversation> pourUtilisateur(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) return null;

            query.distinct(true);

            return cb.or(
                    cb.equal(root.get("emailExpediteur"), email),
                    cb.equal(root.get("emailDestinataire"), email)
            );
        };
    }

    /**
     * Filtre par nom/titre du bien lie a la conversation.
     */
    private Specification<Conversation> parNomDuBien(String nomDuBien) {
        return (root, query, cb) -> {
            if (nomDuBien == null || nomDuBien.isBlank()) return null;

            Join<Conversation, Bien> bien = root.join("bien", JoinType.INNER);

            return cb.like(
                    cb.lower(bien.get("titre")),
                    "%" + nomDuBien.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filtre les conversations selon le statut lu/non lu des messages recus
     * par l'utilisateur connecte.
     */
    private Specification<Conversation> parStatutLecture(String email, Boolean lu) {
        return (root, query, cb) -> {
            if (lu == null) return null;

            query.distinct(true);

            Join<Conversation, Message> message = root.join("messages", JoinType.INNER);

            return cb.and(
                    cb.equal(message.get("destinataire").get("mail"), email),
                    cb.equal(message.get("lu"), lu)
            );
        };
    }
}
