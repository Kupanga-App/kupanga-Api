package com.kupanga.api.user.repository;

import com.kupanga.api.user.entity.AvatarProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvatarProfilRepository extends JpaRepository<AvatarProfil, Long> , JpaSpecificationExecutor<AvatarProfil> {

    Optional<AvatarProfil> findByUrl(String url);
}
