package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.*;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EtatDesLieuxMapper {

    public EtatDesLieuxDTO toDTO(EtatDesLieux edl) {
        EtatDesLieuxDTO dto = new EtatDesLieuxDTO();

        dto.setId(edl.getId());
        dto.setType(edl.getType());
        dto.setStatut(edl.getStatut());
        dto.setDateRealisation(edl.getDateRealisation());
        dto.setHeureRealisation(edl.getHeureRealisation());
        dto.setObservations(edl.getObservations());
        dto.setUrlPdf(edl.getUrlPdf());

        // Signatures
        dto.setSignatureProprietaire(edl.getSignatureProprietaire());
        dto.setDateSignatureProprietaire(edl.getDateSignatureProprietaire());
        dto.setSignatureLocataire(edl.getSignatureLocataire());
        dto.setDateSignatureLocataire(edl.getDateSignatureLocataire());

        // Parties
        dto.setNomProprietaire(edl.getProprietaire().getFirstName()
                + " " + edl.getProprietaire().getLastName());
        dto.setEmailProprietaire(edl.getProprietaire().getMail());
        dto.setNomLocataire(edl.getLocataire().getFirstName()
                + " " + edl.getLocataire().getLastName());
        dto.setEmailLocataire(edl.getLocataire().getMail());

        // Bien
        dto.setAdresseBien(edl.getBien().getAdresse() + ", "
                + edl.getBien().getCodePostal() + " " + edl.getBien().getVille());
        dto.setTypeBien(edl.getBien().getTypeBien() != null
                ? edl.getBien().getTypeBien().name() : null);

        // Collections
        dto.setPieces(mapPieces(edl.getPieces()));
        dto.setCompteurs(mapCompteurs(edl.getCompteurs()));
        dto.setCles(mapCles(edl.getCles()));

        return dto;
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private Set<EtatDesLieuxDTO.PieceEdlDTO> mapPieces(Set<PieceEdl> pieces) {
        if (pieces == null) return Set.of();
        return pieces.stream().map(p -> {
            EtatDesLieuxDTO.PieceEdlDTO dto = new EtatDesLieuxDTO.PieceEdlDTO();
            dto.setId(p.getId());
            dto.setNomPiece(p.getNomPiece());
            dto.setOrdre(p.getOrdre());
            dto.setObservations(p.getObservations());
            dto.setElements(mapElements(p.getElements()));
            return dto;
        }).collect(Collectors.toSet());
    }

    private Set<EtatDesLieuxDTO.ElementEdlDTO> mapElements(Set<ElementEdl> elements) {
        if (elements == null) return Set.of();
        return elements.stream().map(e -> {
            EtatDesLieuxDTO.ElementEdlDTO dto = new EtatDesLieuxDTO.ElementEdlDTO();
            dto.setId(e.getId());
            dto.setTypeElement(e.getTypeElement().name());
            dto.setEtatElement(e.getEtatElement().name());
            dto.setDescription(e.getDescription());
            dto.setObservation(e.getObservation());
            return dto;
        }).collect(Collectors.toSet());
    }

    private Set<EtatDesLieuxDTO.CompteurReleveDTO> mapCompteurs(Set<CompteurReleve> compteurs) {
        if (compteurs == null) return Set.of();
        return compteurs.stream().map(c -> {
            EtatDesLieuxDTO.CompteurReleveDTO dto = new EtatDesLieuxDTO.CompteurReleveDTO();
            dto.setId(c.getId());
            dto.setTypeCompteur(c.getTypeCompteur().name());
            dto.setNumeroCompteur(c.getNumeroCompteur());
            dto.setIndex(c.getIndex());
            dto.setUnite(c.getUnite());
            return dto;
        }).collect(Collectors.toSet());
    }

    private Set<EtatDesLieuxDTO.CleRemiseDTO> mapCles(Set<CleRemise> cles) {
        if (cles == null) return Set.of();
        return cles.stream().map(c -> {
            EtatDesLieuxDTO.CleRemiseDTO dto = new EtatDesLieuxDTO.CleRemiseDTO();
            dto.setId(c.getId());
            dto.setTypeCle(c.getTypeCle());
            dto.setQuantite(c.getQuantite());
            return dto;
        }).collect(Collectors.toSet());
    }
}