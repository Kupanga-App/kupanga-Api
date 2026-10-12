package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BienDTO(

        Long          id,

        // ─── Informations générales ───────────────────────────────────────────
        String        titre,
        TypeBien      typeBien,
        String        description,

        // ─── Adresse ──────────────────────────────────────────────────────────
        String        adresse,
        String        ville,
        String        codePostal,
        Pays          pays,
        // J4 : adresse congolaise (null hors RDC)
        String        commune,
        String        quartier,
        String        avenue,
        String        numeroParcelle,
        String        pointDeRepere,
        Double        latitude,
        Double        longitude,
        /** C2 : REPERE ou ADRESSE = position exacte ; QUARTIER, COMMUNE, VILLE = approximative ; AUCUNE = pas sur la carte. */
        PrecisionLocalisation precisionLocalisation,

        // ─── Caractéristiques physiques ───────────────────────────────────────
        Double        surfaceHabitable,
        Integer       nombrePieces,
        Integer       nombreChambres,
        Integer       etage,
        Boolean       ascenseur,
        Integer       anneeConstruction,
        ModeChauffage modeChauffage,

        // ─── Diagnostic énergétique ───────────────────────────────────────────
        ClasseEnergie classeEnergie,
        ClasseGes     classeGes,

        // ─── Conditions de location ───────────────────────────────────────────
        BigDecimal    loyerMensuel,
        BigDecimal    chargesMensuelles,
        BigDecimal    depotGarantie,
        /** C12 (RDC) : garantie en mois de loyer et loyers payés d'avance. */
        Integer       garantieMois,
        Integer       avanceMois,
        /** C11 : équipements et heures de courant par jour. */
        List<Equipement> equipements,
        Integer       heuresElectriciteParJour,
        Devise        devise,
        Boolean       meuble,
        Boolean       colocation,
        LocalDate     disponibleDe,

        // B12 : bien archivé par l'administration (lecture seule)
        boolean       archive,

        // ─── Parties ──────────────────────────────────────────────────────────
        UserDTO       proprietaire,
        UserDTO       locataire,

        // ─── Documents & médias ───────────────────────────────────────────────
        List<String>  contrats,
        List<String>  quittances,
        List<String>  documents,
        List<String>  images,
        /** R1 : vignettes de 400 px, dans le même ordre que {@code images}. */
        List<String>  vignettes,
        List<String>  pois,

        // ─── Audit ────────────────────────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}