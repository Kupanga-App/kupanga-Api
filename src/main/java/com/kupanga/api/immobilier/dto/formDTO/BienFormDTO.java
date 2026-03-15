package com.kupanga.api.immobilier.dto.formDTO;

import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.validation.NoUrl;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BienFormDTO {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 150, message = "Entre 3 et 150 caractères")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'\"\\-()]+$", message = "Caractères non autorisés")
    @NoUrl
    private String titre;

    @NotNull(message = "Le type de bien est obligatoire")
    private TypeBien typeBien;

    @Size(max = 1000, message = "1000 caractères maximum")
    @NoUrl
    private String description;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(min = 5, max = 200, message = "Entre 5 et 200 caractères")
    @Pattern(regexp = "^[\\p{L}0-9 ,.'\\-]+$", message = "Caractères non autorisés")
    @NoUrl
    private String adresse;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 100, message = "Entre 2 et 100 caractères")
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "Lettres, espaces, tirets ou apostrophes uniquement")
    @NoUrl
    private String ville;

    @NotBlank(message = "Le code postal est obligatoire")
    @Pattern(regexp = "^[0-9A-Z\\- ]{3,10}$", message = "Code postal invalide (ex: 44000)")
    private String codePostal;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[\\p{L} \\-']+$", message = "Caractères non autorisés")
    @NoUrl
    private String pays;
}