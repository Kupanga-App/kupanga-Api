package com.kupanga.api.user.repository;

import com.kupanga.api.user.entity.AvatarProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AvatarProfilRepository extends JpaRepository<AvatarProfil, Long> , JpaSpecificationExecutor<AvatarProfil> {
}
