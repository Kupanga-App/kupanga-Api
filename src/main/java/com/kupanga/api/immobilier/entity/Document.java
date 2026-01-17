package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String url;
    private LocalDate dateAjout;

    @ManyToOne
    @JoinColumn(name = "bien_id")
    private Bien bien;
}
