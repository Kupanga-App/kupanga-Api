package com.kupanga.api.backoffice.specification;

import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class BienAdminSpecification {

    public Specification<Bien> build(BienAdminSearchDTO dto) {
        return Specification
                .where(parTitre(dto.titre()))
                .and(parVille(dto.ville()))
                .and(parType(dto.typeBien()));
    }

    private Specification<Bien> parTitre(String titre) {
        return (root, query, cb) -> {
            if (titre == null || titre.isBlank()) return null;
            return cb.like(cb.lower(root.get("titre")), "%" + titre.toLowerCase() + "%");
        };
    }

    private Specification<Bien> parVille(String ville) {
        return (root, query, cb) -> {
            if (ville == null || ville.isBlank()) return null;
            return cb.like(cb.lower(root.get("ville")), "%" + ville.toLowerCase() + "%");
        };
    }

    private Specification<Bien> parType(TypeBien typeBien) {
        return (root, query, cb) -> {
            if (typeBien == null) return null;
            return cb.equal(root.get("typeBien"), typeBien);
        };
    }
}
