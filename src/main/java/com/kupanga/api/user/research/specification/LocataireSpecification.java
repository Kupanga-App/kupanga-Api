package com.kupanga.api.user.research.specification;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.research.dto.LocataireSearchDTO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class LocataireSpecification {

    /**
     * Construit une {@code Specification} JPA à partir des critères de recherche fournis.
     * <p>
     * Équivaut à :
     * <pre>
     *   SELECT u.* FROM utilisateurs u
     *   INNER JOIN conversations c ON c.email_expediteur = u.email
     *   INNER JOIN biens b         ON b.id = c.bien_id
     *   WHERE b.id = :bienId
     *     AND (filtres dynamiques…)
     * </pre>
     *
     * @param bienId l'identifiant du bien dont on veut les locataires
     * @param dto    les critères de filtrage dynamiques
     * @return la spécification combinée
     */
    public Specification<User> build(Long bienId, LocataireSearchDTO dto) {
        return Specification
                .where(pourBien(bienId))
                .and(parPrenom(dto.firstName()))
                .and(parNom(dto.lastName()))
                .and(parMail(dto.mail()));
    }

    /**
     * Joint utilisateurs → conversations → biens et filtre sur l'id du bien.
     * <p>
     * La jointure se fait via le champ {@code emailExpediteur} de {@link Conversation}
     * mis en regard du champ {@code mail} de {@link User}.
     * C'est la traduction exacte du JOIN SQL :
     * {@code INNER JOIN conversations c ON c.email_expediteur = u.email}.
     */
    private Specification<User> pourBien(Long bienId) {
        return (root, query, cb) -> {
            if (bienId == null) return null;

            var subquery = query.subquery(String.class);
            var c = subquery.from(Conversation.class);
            Join<Conversation, Bien> b = c.join("bien", JoinType.INNER);

            subquery
                    .select(c.get("emailExpediteur"))
                    .where(cb.equal(b.get("id"), bienId));

            query.distinct(true);

            return root.get("mail").in(subquery);
        };
    }

    /**
     * Filtre par prénom (recherche partielle, insensible à la casse).
     */
    private Specification<User> parPrenom(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get("firstName")),
                    "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filtre par nom (recherche partielle, insensible à la casse).
     */
    private Specification<User> parNom(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get("lastName")),
                    "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filtre par email (recherche partielle, insensible à la casse).
     */
    private Specification<User> parMail(String mail) {
        return (root, query, cb) -> {
            if (mail == null || mail.isBlank()) return null;
            return cb.like(
                    cb.lower(root.get("mail")),
                    "%" + mail.toLowerCase() + "%"
            );
        };
    }
}