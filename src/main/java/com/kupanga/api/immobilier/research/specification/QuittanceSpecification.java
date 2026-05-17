package com.kupanga.api.immobilier.research.specification;

import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutQuittance;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.user.entity.Role;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class QuittanceSpecification {

    public Specification<Quittance> build(QuittanceSearchDTO dto, Long userId, Role role) {
        return Specification
                .where(parUtilisateur(userId, role))
                .and(parAnnee(dto.annee()))
                .and(parMois(dto.mois()))
                .and(parStatut(dto.statut()))
                .and(parBien(dto.bienId()));
    }

    private Specification<Quittance> parUtilisateur(Long userId, Role role) {
        return (root, query, cb) -> {
            if (role == Role.ROLE_PROPRIETAIRE) {
                return cb.or(
                        cb.equal(root.get("proprietaire").get("id"), userId),
                        cb.equal(root.get("locataire").get("id"), userId)
                );
            }
            return cb.equal(root.get("locataire").get("id"), userId);
        };
    }

    private Specification<Quittance> parAnnee(Integer annee) {
        return (root, query, cb) -> {
            if (annee == null) return null;
            return cb.equal(root.get("annee"), annee);
        };
    }

    // mois est stocké en String dans l'entité — on compare via String.valueOf()
    private Specification<Quittance> parMois(String mois) {
        return (root, query, cb) -> {
            if (mois == null) return null;
            return cb.equal(root.get("mois"), mois);
        };
    }

    private Specification<Quittance> parStatut(StatutQuittance statut) {
        return (root, query, cb) -> {
            if (statut == null) return null;
            return cb.equal(root.get("statut"), statut);
        };
    }

    private Specification<Quittance> parBien(Long bienId) {
        return (root, query, cb) -> {
            if (bienId == null) return null;
            return cb.equal(root.get("bien").get("id"), bienId);
        };
    }
}
