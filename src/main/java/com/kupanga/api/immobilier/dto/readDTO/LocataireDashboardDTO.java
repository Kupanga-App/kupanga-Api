package com.kupanga.api.immobilier.dto.readDTO;

import com.kupanga.api.immobilier.entity.StatutContrat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LocataireDashboardDTO {

    private LocataireDTO        locataire;
    private BienResumeDTO       bien;
    private ContratResumeDTO    contrat;        // null si aucun contrat SIGNE
    private StatutsDTO          statuts;
    private List<QuittanceDTO>  quittances;
    private List<EtatDesLieuxDTO> etatsDesLieux;

    // ─── Sous-DTOs ────────────────────────────────────────────────────────────

    @Getter @Setter
    public static class LocataireDTO {
        private String prenom;
        private String nom;
        private String initiales;           // "AD" pour Alice Dupont
    }

    @Getter @Setter
    public static class BienResumeDTO {
        private Long            id;
        private String          adresse;    // "12 rue de la Paix, 75001 Paris"
        private Double          surface;
        private Integer         nbPieces;
        private String          type;       // ex: "APPARTEMENT"
        private String          nomProprietaire;
        private List<String>    photos;
        private Boolean         colocation;
    }

    @Getter @Setter
    public static class ContratResumeDTO {
        private Long            id;
        private String          type;           // "MEUBLE" | "NON_MEUBLE"
        private LocalDate       dateDebut;
        private LocalDate       dateFin;
        private Integer         moisEcoules;    // calculé depuis dateDebut
        private Integer         dureeTotale;    // dureeBailMois
        private Double          loyerMensuel;
        private Double          charges;
        private Double          depotGarantie;
        private StatutContrat   statut;
    }

    @Getter @Setter
    public static class StatutsDTO {
        private String      derniereQuittance;  // ex: "Mars 2026" — null si aucune
        private LocalDate   dateFinContrat;     // null si bail à durée indéterminée
    }
}
