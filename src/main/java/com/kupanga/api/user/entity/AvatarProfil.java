package com.kupanga.api.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "avatars_profil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarProfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
}
