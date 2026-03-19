package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.Quittance;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QuittanceMapper {

    private static final Map<Integer, String> MOIS_LABELS = Map.ofEntries(
            Map.entry(1,  "Janvier"),
            Map.entry(2,  "Février"),
            Map.entry(3,  "Mars"),
            Map.entry(4,  "Avril"),
            Map.entry(5,  "Mai"),
            Map.entry(6,  "Juin"),
            Map.entry(7,  "Juillet"),
            Map.entry(8,  "Août"),
            Map.entry(9,  "Septembre"),
            Map.entry(10, "Octobre"),
            Map.entry(11, "Novembre"),
            Map.entry(12, "Décembre")
    );

    public QuittanceDTO toDTO(Quittance q) {
        QuittanceDTO dto = new QuittanceDTO();

        dto.setId(q.getId());
        dto.setMois(q.getMois());
        dto.setAnnee(q.getAnnee());
        dto.setMoisLabel(MOIS_LABELS.getOrDefault(q.getMois(), "—") + " " + q.getAnnee());

        dto.setLoyerMensuel(q.getLoyerMensuel());
        dto.setChargesMensuelles(q.getChargesMensuelles());
        dto.setMontantTotal(q.getMontantTotal());

        dto.setDateEcheance(q.getDateEcheance());
        dto.setDatePaiement(q.getDatePaiement());
        dto.setStatut(q.getStatut());
        dto.setUrlPdf(q.getUrlPdf());

        dto.setNomProprietaire(q.getProprietaire().getFirstName()
                + " " + q.getProprietaire().getLastName());
        dto.setEmailProprietaire(q.getProprietaire().getMail());
        dto.setNomLocataire(q.getLocataire().getFirstName()
                + " " + q.getLocataire().getLastName());
        dto.setEmailLocataire(q.getLocataire().getMail());

        dto.setAdresseBien(q.getBien().getAdresse() + ", "
                + q.getBien().getCodePostal() + " " + q.getBien().getVille());
        dto.setTypeBien(q.getBien().getTypeBien() != null
                ? q.getBien().getTypeBien().name() : null);
        dto.setSurfaceHabitable(q.getBien().getSurfaceHabitable());

        dto.setCreatedAt(q.getCreatedAt());
        dto.setUpdatedAt(q.getUpdatedAt());

        return dto;
    }
}