package com.kupanga.api.immobilier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.juridiction.Pays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * J4 : dans le vrai contexte Spring, {@code @ValideSelonJuridiction} reçoit le registre des juridictions et
 * {@code POST /biens} répond 400 avec une erreur par champ fautif, avant tout traitement (aucun bien créé).
 * Sans ce test, une contrainte privée de registre ne bloquerait rien en silence (seul le service rattraperait).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
@DisplayName("J4 : POST /biens validé selon le pays (contexte Spring complet)")
class BienJuridictionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BienRepository bienRepository;

    private static BienFormDTO kinshasa() {
        return BienFormDTO.builder()
                .titre("Appartement Matonge").typeBien(TypeBien.APPARTEMENT)
                .adresse("N° 12, Av. Kasa-Vubu").ville("Kinshasa").pays(Pays.CD)
                .commune("Kalamu").quartier("Matonge")
                .surfaceHabitable(65.0).nombrePieces(3)
                .loyerMensuel(new BigDecimal("500")).chargesMensuelles(new BigDecimal("20"))
                .depotGarantie(new BigDecimal("1000"))
                .meuble(false).colocation(false).disponibleDe(LocalDate.now().plusDays(10))
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions creer(Consumer<BienFormDTO> modification) throws Exception {
        BienFormDTO dto = kinshasa();
        modification.accept(dto);
        MockMultipartFile bien = new MockMultipartFile("bienFormDTO", "", "application/json",
                objectMapper.writeValueAsBytes(dto));
        return mockMvc.perform(multipart("/biens").file(bien));
    }

    @Test
    @WithMockUser(username = "proprio-j4@test.com", roles = "PROPRIETAIRE")
    @DisplayName("RDC : code postal (masqué) et quartier manquant → 400, une erreur par champ")
    void rdc_codePostalEtQuartier_400() throws Exception {
        long avant = bienRepository.count();

        creer(dto -> {
            dto.setCodePostal("12345");
            dto.setQuartier(null);
        })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.codePostal").value(
                        "Ce champ ne s'applique pas à un bien situé en République démocratique du Congo"))
                .andExpect(jsonPath("$.validationErrors.quartier").value(
                        "Ce champ est obligatoire pour un bien situé en République démocratique du Congo"));

        assertThat(bienRepository.count()).isEqualTo(avant);
    }

    @Test
    @WithMockUser(username = "proprio-j4@test.com", roles = "PROPRIETAIRE")
    @DisplayName("France : commune (masquée) et code postal manquant → 400 sur ces deux champs")
    void france_communeEtCodePostal_400() throws Exception {
        creer(dto -> {
            dto.setPays(Pays.FR);
            dto.setVille("Nantes");
            dto.setQuartier(null);
        })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.commune").exists())
                .andExpect(jsonPath("$.validationErrors.codePostal").exists())
                .andExpect(jsonPath("$.validationErrors.quartier").doesNotExist());
    }

    @Test
    @WithMockUser(username = "proprio-j4@test.com", roles = "PROPRIETAIRE")
    @DisplayName("Champs facultatifs envoyés vides (\"\") : acceptés par la validation, aucune erreur sur ces champs")
    void champsFacultatifsVides_acceptes() throws Exception {
        creer(dto -> {
            dto.setAvenue("");
            dto.setNumeroParcelle("");
            dto.setCodePostal("");
            dto.setTitre(null); // seule erreur attendue : la requête ne va pas plus loin que la validation
        })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.titre").exists())
                .andExpect(jsonPath("$.validationErrors.avenue").doesNotExist())
                .andExpect(jsonPath("$.validationErrors.numeroParcelle").doesNotExist())
                .andExpect(jsonPath("$.validationErrors.codePostal").doesNotExist());
    }
}
