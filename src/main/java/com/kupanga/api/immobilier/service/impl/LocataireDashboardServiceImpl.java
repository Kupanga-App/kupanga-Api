package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.dto.readDTO.LocataireDashboardDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.LocataireDashboardService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocataireDashboardServiceImpl implements LocataireDashboardService {

    private final UserService            userService;
    private final BienRepository         bienRepository;
    private final ContratRepository      contratRepository;
    private final QuittanceRepository    quittanceRepository;
    private final EtatDesLieuxRepository etatDesLieuxRepository;
    private final QuittanceMapper        quittanceMapper;
    private final EtatDesLieuxMapper     etatDesLieuxMapper;

    @Override
    public LocataireDashboardDTO getDashboard(Authentication auth, Long bienId) {

        User locataire = userService.getUserByEmail(auth.getName());

        Bien bien = bienRepository.findById(bienId)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Bien introuvable pour l'id : " + bienId, HttpStatus.NOT_FOUND));

        if (bien.getLocataire() == null
                || !bien.getLocataire().getId().equals(locataire.getId())) {
            throw new KupangaBusinessException(
                    "Accès refusé : vous n'êtes pas le locataire de ce bien",
                    HttpStatus.FORBIDDEN);
        }

        Contrat contratActif = contratRepository.findByBienId(bienId).stream()
                .filter(c -> c.getStatut() == StatutContrat.SIGNE)
                .findFirst()
                .orElse(null);

        List<Quittance> quittances = quittanceRepository
                .findByBienIdAndLocataireId(bienId, locataire.getId());

        // findByBienId fetche déjà le locataire — on filtre  puis on charge les
        // sous-collections (pièces, éléments, compteurs, clés) via findWithAllRelations
        // pour éviter le N+1 sur les Set imbriqués.
        List<EtatDesLieux> etatsDesLieux = etatDesLieuxRepository.findByBienId(bienId)
                .stream()
                .filter(edl -> edl.getLocataire() != null
                        && edl.getLocataire().getId().equals(locataire.getId()))
                .map(edl -> etatDesLieuxRepository.findWithAllRelations(edl.getId()).orElseThrow())
                .toList();

        return buildDashboard(locataire, bien, contratActif, quittances, etatsDesLieux);
    }

    // ─── Builders ─────────────────────────────────────────────────────────────

    private LocataireDashboardDTO buildDashboard(
            User locataire,
            Bien bien,
            Contrat contratActif,
            List<Quittance> quittances,
            List<EtatDesLieux> etatsDesLieux) {

        LocataireDashboardDTO dto = new LocataireDashboardDTO();
        dto.setLocataire(buildLocataireDTO(locataire));
        dto.setBien(buildBienResumeDTO(bien));
        dto.setContrat(contratActif != null ? buildContratResumeDTO(bien, contratActif) : null);
        dto.setStatuts(buildStatutsDTO(contratActif, quittances));
        dto.setQuittances(quittances.stream().map(quittanceMapper::toDTO).toList());
        dto.setEtatsDesLieux(etatsDesLieux.stream().map(etatDesLieuxMapper::toDTO).toList());
        return dto;
    }

    private LocataireDashboardDTO.LocataireDTO buildLocataireDTO(User locataire) {
        LocataireDashboardDTO.LocataireDTO dto = new LocataireDashboardDTO.LocataireDTO();
        dto.setPrenom(locataire.getFirstName());
        dto.setNom(locataire.getLastName());
        dto.setInitiales(buildInitiales(locataire));
        return dto;
    }

    private LocataireDashboardDTO.BienResumeDTO buildBienResumeDTO(Bien bien) {
        LocataireDashboardDTO.BienResumeDTO dto = new LocataireDashboardDTO.BienResumeDTO();
        dto.setId(bien.getId());
        dto.setAdresse(bien.getAdresse() + ", " + bien.getCodePostal() + " " + bien.getVille());
        dto.setSurface(bien.getSurfaceHabitable());
        dto.setNbPieces(bien.getNombrePieces());
        dto.setType(bien.getTypeBien() != null ? bien.getTypeBien().name() : null);
        dto.setNomProprietaire(bien.getProprietaire() != null
                ? bien.getProprietaire().getFirstName() + " " + bien.getProprietaire().getLastName()
                : null);
        dto.setPhotos(bien.getImages() != null
                ? bien.getImages().stream()
                        .map(BienImage::getUrl)
                        .filter(Objects::nonNull)
                        .toList()
                : List.of());
        dto.setColocation(bien.getColocation());
        return dto;
    }

    private LocataireDashboardDTO.ContratResumeDTO buildContratResumeDTO(Bien bien, Contrat contrat) {
        LocataireDashboardDTO.ContratResumeDTO dto = new LocataireDashboardDTO.ContratResumeDTO();
        dto.setId(contrat.getId());
        dto.setType(Boolean.TRUE.equals(bien.getMeuble()) ? "MEUBLE" : "NON_MEUBLE");
        dto.setDateDebut(contrat.getDateDebut());
        dto.setDateFin(contrat.getDateFin());
        dto.setMoisEcoules(contrat.getDateDebut() != null
                ? (int) ChronoUnit.MONTHS.between(contrat.getDateDebut(), LocalDate.now())
                : null);
        dto.setDureeTotale(contrat.getDureeBailMois());
        dto.setLoyerMensuel(contrat.getLoyerMensuel());
        dto.setCharges(contrat.getChargesMensuelles());
        dto.setDepotGarantie(contrat.getDepotGarantie());
        dto.setStatut(contrat.getStatut());
        return dto;
    }

    private LocataireDashboardDTO.StatutsDTO buildStatutsDTO(
            Contrat contratActif, List<Quittance> quittances) {

        LocataireDashboardDTO.StatutsDTO dto = new LocataireDashboardDTO.StatutsDTO();

        dto.setDerniereQuittance(
                quittances.stream()
                        .filter(q -> q.getCreatedAt() != null
                                && q.getMois() != null
                                && q.getAnnee() != null)
                        .max(Comparator.comparing(Quittance::getCreatedAt))
                        .map(q -> q.getMois() + " " + q.getAnnee())
                        .orElse(null)
        );

        dto.setDateFinContrat(contratActif != null ? contratActif.getDateFin() : null);
        return dto;
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    private String buildInitiales(User user) {
        String first = (user.getFirstName() != null && !user.getFirstName().isEmpty())
                ? String.valueOf(user.getFirstName().charAt(0)).toUpperCase() : "";
        String last = (user.getLastName() != null && !user.getLastName().isEmpty())
                ? String.valueOf(user.getLastName().charAt(0)).toUpperCase() : "";
        return first + last;
    }
}
