package com.kupanga.api.user.repository;

import com.kupanga.api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByMail(String mail);
    Optional<User> findByMail(String mail);

}
