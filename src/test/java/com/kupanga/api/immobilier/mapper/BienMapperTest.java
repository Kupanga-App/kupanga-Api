package com.kupanga.api.immobilier.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kupanga.api.immobilier.dto.readDTO.BienPublicDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class BienMapperTest {

    private final BienMapper bienMapper = Mappers.getMapper(BienMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("toPublicDTO() : propriétaire réduit à prénom + initiale + photo, aucune info locataire (P0-6)")
    void toPublicDTO_neverExposesPersonalData() throws Exception {
        User proprietaire = User.builder()
                .id(1L).firstName("Jean").lastName("dupont")
                .mail("jean.dupont@mail.com").role(Role.ROLE_PROPRIETAIRE)
                .urlProfile("photo.jpg")
                .build();
        User locataire = User.builder()
                .id(2L).firstName("Alice").lastName("Martin")
                .mail("alice.martin@mail.com").role(Role.ROLE_LOCATAIRE)
                .build();
        Bien bien = Bien.builder()
                .id(10L).titre("T3").typeBien(TypeBien.APPARTEMENT)
                .proprietaire(proprietaire).locataire(locataire)
                .build();

        BienPublicDTO dto = bienMapper.toPublicDTO(bien);

        assertThat(dto.proprietaire().firstName()).isEqualTo("Jean");
        assertThat(dto.proprietaire().initialeNom()).isEqualTo("D.");
        assertThat(dto.proprietaire().urlProfile()).isEqualTo("photo.jpg");

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json)
                .doesNotContain("jean.dupont@mail.com")
                .doesNotContain("dupont")
                .doesNotContain("Alice")
                .doesNotContain("alice.martin@mail.com")
                .doesNotContain("\"mail\"")
                .doesNotContain("\"role\"")
                .doesNotContain("\"locataire\"")
                .doesNotContain("\"contrats\"")
                .doesNotContain("\"quittances\"");
    }

    @Test
    @DisplayName("toPublicDTO() : bien sans propriétaire ni nom → pas d'erreur")
    void toPublicDTO_handlesMissingNames() {
        Bien bien = Bien.builder().id(11L)
                .proprietaire(User.builder().id(3L).firstName("Solo").build())
                .build();

        BienPublicDTO dto = bienMapper.toPublicDTO(bien);

        assertThat(dto.proprietaire().initialeNom()).isNull();
        assertThat(bienMapper.toPublicDTO(Bien.builder().id(12L).build()).proprietaire()).isNull();
    }
}
