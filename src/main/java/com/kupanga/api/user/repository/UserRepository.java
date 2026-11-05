package com.kupanga.api.user.repository;

import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {

    boolean existsByMail(String mail);
    Optional<User> findByMail(String mail);
    Optional<User> findByGoogleId(String googleId);

    long countByRole(Role role);

    /** Back-office (lot 3) : comptes actifs qui ne possèdent aucun bien et ne louent aucun logement. */
    @org.springframework.data.jpa.repository.Query("""
            select count(u) from User u
            where u.anonymise = false
              and not exists (select b.id from com.kupanga.api.immobilier.entity.Bien b where b.proprietaire = u)
              and not exists (select b2.id from com.kupanga.api.immobilier.entity.Bien b2 where b2.locataire = u)
            """)
    long countSansBienNiLogement();

    /** B12 : la photo de profil est-elle aussi celle d'un autre compte ? */
    boolean existsByClePhotoProfilAndIdNot(String clePhotoProfil, Long id);
}
