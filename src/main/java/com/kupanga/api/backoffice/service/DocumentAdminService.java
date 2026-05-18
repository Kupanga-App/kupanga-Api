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

@Service
@RequiredArgsConstructor
public class DocumentAdminService {

    private final ContratRepository      contratRepository;
    private final EtatDesLieuxRepository edlRepository;
    private final QuittanceRepository    quittanceRepository;
    private final BienRepository         bienRepository;

    @Transactional(readOnly = true)
    public long countTotalContrats() {
        return contratRepository.count();
    }

    @Transactional(readOnly = true)
    public long countTotalEdl() {
        return edlRepository.count();
    }

    @Transactional(readOnly = true)
    public long countTotalQuittances() {
        return quittanceRepository.count();
    }

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

    @Transactional(readOnly = true)
    public List<ContratAdminDTO> getContratsParBien(Long bienId) {
        return contratRepository.findByBienId(bienId).stream()
                .map(ContratAdminDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EdlAdminDTO> getEdlParBien(Long bienId) {
        return edlRepository.findByBienId(bienId).stream()
                .map(EdlAdminDTO::from)
                .toList();
    }

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
