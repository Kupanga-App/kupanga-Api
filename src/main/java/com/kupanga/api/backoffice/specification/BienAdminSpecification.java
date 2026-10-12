package com.kupanga.api.backoffice.specification;

import com.kupanga.api.juridiction.Pays;
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
                .and(parPays(dto.pays()))
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

    /** J1 : liste déroulante sur l'enum {@link Pays}. */
    private Specification<Bien> parPays(Pays pays) {
        return (root, query, cb) -> {
            if (pays == null) return null;
            return cb.equal(root.get("pays"), pays);
        };
    }

    private Specification<Bien> parType(TypeBien typeBien) {
        return (root, query, cb) -> {
            if (typeBien == null) return null;
            return cb.equal(root.get("typeBien"), typeBien);
        };
    }
}
