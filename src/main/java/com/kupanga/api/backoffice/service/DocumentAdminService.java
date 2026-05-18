package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.BienDocumentsSummaryDTO;
import com.kupanga.api.backoffice.dto.ContratAdminDTO;
import com.kupanga.api.backoffice.dto.EdlAdminDTO;
import com.kupanga.api.backoffice.dto.QuittanceAdminDTO;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service d'administration des documents immobiliers.
 * Fournit les compteurs et la consultation des contrats, états des lieux et quittances pour le back-office.
 */
@Service
@RequiredArgsConstructor
public class DocumentAdminService {

    private final ContratRepository      contratRepository;
    private final EtatDesLieuxRepository edlRepository;
    private final QuittanceRepository    quittanceRepository;
    private final BienRepository         bienRepository;

    /**
     * Retourne le nombre total de contrats enregistrés.
     *
     * @return nombre total de contrats
     */
    @Transactional(readOnly = true)
    public long countTotalContrats() {
        return contratRepository.count();
    }

    /**
     * Retourne le nombre total d'états des lieux enregistrés.
     *
     * @return nombre total d'états des lieux
     */
    @Transactional(readOnly = true)
    public long countTotalEdl() {
        return edlRepository.count();
    }

    /**
     * Retourne le nombre total de quittances enregistrées.
     *
     * @return nombre total de quittances
     */
    @Transactional(readOnly = true)
    public long countTotalQuittances() {
        return quittanceRepository.count();
    }

    /**
     * Retourne un résumé des documents par bien, trié par nombre total de documents décroissant.
     * Seuls les biens ayant au moins un document sont inclus.
     *
     * @return liste de résumés (contrats + EDL + quittances) par bien
     */
    @Transactional(readOnly = true)
    public List<BienDocumentsSummaryDTO> getDocumentsParBien() {
        Map<Long, Long> contratsMap    = toMap(contratRepository.countParBien());
        Map<Long, Long> edlMap         = toMap(edlRepository.countParBien());
        Map<Long, Long> quittancesMap  = toMap(quittanceRepository.countParBien());

        return bienRepository.findAll().stream()
                .map(b -> new BienDocumentsSummaryDTO(
                        b.getId(),
                        b.getTitre(),
                        b.getVille(),
                        contratsMap.getOrDefault(b.getId(), 0L),
                        edlMap.getOrDefault(b.getId(), 0L),
                        quittancesMap.getOrDefault(b.getId(), 0L)
                ))
                .filter(d -> d.total() > 0)
                .sorted(Comparator.comparingLong(BienDocumentsSummaryDTO::total).reversed())
                .toList();
    }

    /**
     * Retourne la liste des contrats associés à un bien.
     *
     * @param bienId identifiant du bien
     * @return liste des contrats du bien
     */
    @Transactional(readOnly = true)
    public List<ContratAdminDTO> getContratsParBien(Long bienId) {
        return contratRepository.findByBienId(bienId).stream()
                .map(ContratAdminDTO::from)
                .toList();
    }

    /**
     * Retourne la liste des états des lieux associés à un bien.
     *
     * @param bienId identifiant du bien
     * @return liste des états des lieux du bien
     */
    @Transactional(readOnly = true)
    public List<EdlAdminDTO> getEdlParBien(Long bienId) {
        return edlRepository.findByBienId(bienId).stream()
                .map(EdlAdminDTO::from)
                .toList();
    }

    /**
     * Retourne la liste des quittances associées à un bien.
     *
     * @param bienId identifiant du bien
     * @return liste des quittances du bien
     */
    @Transactional(readOnly = true)
    public List<QuittanceAdminDTO> getQuittancesParBien(Long bienId) {
        return quittanceRepository.findByBienId(bienId).stream()
                .map(QuittanceAdminDTO::from)
                .toList();
    }

    private Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
        ));
    }
}
