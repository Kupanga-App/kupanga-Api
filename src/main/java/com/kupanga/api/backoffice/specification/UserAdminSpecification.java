package com.kupanga.api.backoffice.specification;

import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class UserAdminSpecification {

    public Specification<User> build(UserAdminSearchDTO dto) {
        return Specification
                .where(parPrenom(dto.firstName()))
                .and(parNom(dto.lastName()))
                .and(parMail(dto.mail()))
                .and(parRole(dto.role()));
    }

    private Specification<User> parPrenom(String firstName) {
        return (root, query, cb) -> {
            if (firstName == null || firstName.isBlank()) return null;
            return cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%");
        };
    }

    private Specification<User> parNom(String lastName) {
        return (root, query, cb) -> {
            if (lastName == null || lastName.isBlank()) return null;
            return cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%");
        };
    }

    private Specification<User> parMail(String mail) {
        return (root, query, cb) -> {
            if (mail == null || mail.isBlank()) return null;
            return cb.like(cb.lower(root.get("mail")), "%" + mail.toLowerCase() + "%");
        };
    }

    private Specification<User> parRole(Role role) {
        return (root, query, cb) -> {
            if (role == null) return null;
            return cb.equal(root.get("role"), role);
        };
    }
}
