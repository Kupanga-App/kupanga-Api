package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.ModePaiement;
import com.kupanga.api.immobilier.validation.SignaturePng;
import com.kupanga.api.immobilier.validation.SignaturePngValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

/**
 * C8-bis : le propriétaire déclare le loyer reçu hors application et signe la quittance.
 *
 * @param signatureBase64   signature du propriétaire (PNG validé, B6)
 * @param modePaiement      mode proposé dans le pays du logement (profil de juridiction, 400 sinon)
 * @param datePaiement      date de réception, facultative (aujourd'hui dans le fuseau du pays) ; jamais dans le futur
 * @param referencePaiement référence facultative (n° de transaction Mobile Money, de virement…)
 */
@Builder
public record MarquerPayeeDTO(

        @NotBlank(message = "La signature est obligatoire")
        @Size(min = 100, max = SignaturePngValidator.TAILLE_MAX_BASE64, message = "La signature semble invalide")
        @SignaturePng
        String signatureBase64,

        @NotNull(message = "Le mode de paiement est obligatoire")
        ModePaiement modePaiement,

        LocalDate datePaiement,

        @Size(max = 100, message = "La référence ne doit pas dépasser 100 caractères")
        @Pattern(regexp = "^[\\p{L}0-9 .,/#_\\-]*$", message = "Caractères non autorisés dans la référence")
        String referencePaiement
) {}
