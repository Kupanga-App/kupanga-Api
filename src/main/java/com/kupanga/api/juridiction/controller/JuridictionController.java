package com.kupanga.api.juridiction.controller;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.dto.JuridictionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * J6 : configuration publique des juridictions pour le formulaire de bien (lecture seule, aucune donnée personnelle).
 */
@RestController
@RequestMapping("/juridictions")
@RequiredArgsConstructor
@Tag(name = "Juridictions", description = "Configuration du formulaire de bien selon le pays (public)")
public class JuridictionController {

    /** La configuration ne change qu'au redéploiement : le navigateur peut la garder une heure. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final JuridictionRegistry juridictionRegistry;

    @Operation(summary = "Pays pris en charge",
            description = "Pays dans lesquels un bien peut être créé, avec leur nom (liste déroulante du formulaire).")
    @GetMapping
    public ResponseEntity<List<JuridictionDTO.PaysDTO>> paysPrisEnCharge() {
        List<JuridictionDTO.PaysDTO> pays = juridictionRegistry.paysPrisEnCharge().stream()
                .map(p -> new JuridictionDTO.PaysDTO(p, p.getLibelle()))
                .toList();
        return ResponseEntity.ok().cacheControl(CACHE).body(pays);
    }

    @Operation(summary = "Configuration du formulaire de bien pour un pays",
            description = """
                    Champs obligatoires et masqués, devises (et devise par défaut), types de bien proposés.
                    Le formulaire s'adapte au pays choisi ; l'API revalide toujours à la création et à la modification
                    (un champ masqué envoyé est refusé en 400).
                    """)
    @ApiResponse(responseCode = "200", description = "Configuration du pays")
    @ApiResponse(responseCode = "400", description = "Code pays inconnu (autre que FR, BE, CD, CG)")
    @ApiResponse(responseCode = "404", description = "Pays pas encore pris en charge")
    @GetMapping("/{pays}")
    public ResponseEntity<JuridictionDTO> configuration(@PathVariable Pays pays) {
        if (!juridictionRegistry.estPrisEnCharge(pays)) {
            throw new KupangaBusinessException(
                    "Les biens situés en " + pays.getLibelle() + " ne sont pas encore pris en charge", HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok().cacheControl(CACHE).body(JuridictionDTO.de(pays, juridictionRegistry.profil(pays)));
    }
}
