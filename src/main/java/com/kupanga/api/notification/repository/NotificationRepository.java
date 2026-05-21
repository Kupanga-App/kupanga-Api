package com.kupanga.api.notification.repository;

import com.kupanga.api.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.destinataire.mail = :email AND n.lue = false ORDER BY n.createdAt DESC")
    List<Notification> findNonLuesByEmail(@Param("email") String email);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.destinataire.mail = :email")
    Optional<Notification> findByIdAndDestinataireMail(@Param("id") Long id, @Param("email") String email);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.destinataire.mail = :email AND n.lue = false")
    void marquerToutesLues(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.destinataire.id = :userId")
    void deleteByDestinataireId(@Param("userId") Long userId);
}
