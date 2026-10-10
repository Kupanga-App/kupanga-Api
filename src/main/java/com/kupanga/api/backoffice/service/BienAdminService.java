package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.BienAdminDTO;
import com.kupanga.api.backoffice.dto.BienAdminPageDTO;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.backoffice.specification.BienAdminSpecification;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutEdl;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.pagination.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service d'administration des biens immobiliers.
 * Fournit les opérations de recherche paginée, archivage et statistiques pour le back-office.
 */
@Service
@RequiredArgsConstructor
public class BienAdminService {

    private final BienRepository        bienRepository;
    private final BienAdminSpecification bienAdminSpecification;
    private final ContratRepository     contratRepository;
    private final EtatDesLieuxRepository etatDesLieuxRepository;

    /**
     * Recherche paginée des biens selon les critères admin.
     *
     * @param dto critères de recherche, tri et pagination
     * @return page de biens correspondant aux critères
     */
    @Transactional(readOnly = true)
    public BienAdminPageDTO rechercher(BienAdminSearchDTO dto) {
        Pagination pagination = dto.toPagination();
        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );
        Page<BienAdminDTO> page = bienRepository
                .findAll(bienAdminSpecification.build(dto), pageable)
                .map(BienAdminDTO::from);
        return BienAdminPageDTO.from(page);
    }

    /**
     * B12 : archive un bien au lieu de le supprimer. Ses baux, quittances, EDL et conversations sont conservés ;
     * il sort de la recherche publique et passe en lecture seule.
     *
     * @param id identifiant du bien
     * @return {@code false} si le bien n'existe pas
     */
    @Transactional
    public boolean archiver(Long id) {
        Bien bien = bienRepository.findById(id).orElse(null);
        if (bien == null) {
            return false;
        }
        if (!bien.isArchive()) {
            bien.setArchive(true);
            bien.setDateArchivage(LocalDateTime.now());
            // Liens de signature en cours invalidés ; le propriétaire pourra relancer après un désarchivage
            contratRepository.expirerNonSignesDuBien(id, StatutContrat.SIGNE, StatutContrat.EXPIRE);
            etatDesLieuxRepository.expirerNonSignesDuBien(id, StatutEdl.SIGNE, StatutEdl.EXPIRE);
        }
        return true;
    }

    /**
     * B12 : remet en ligne un bien archivé, sauf si son propriétaire a été anonymisé (annonce sans propriétaire).
     *
     * @param id identifiant du bien
     * @return {@code false} si le bien n'existe pas ou si son propriétaire est anonymisé
     */
    @Transactional
    public boolean desarchiver(Long id) {
        Bien bien = bienRepository.findById(id).orElse(null);
        if (bien == null || bien.getProprietaire() == null || bien.getProprietaire().isAnonymise()) {
            return false;
        }
        bien.setArchive(false);
        bien.setDateArchivage(null);
        return true;
    }

    /**
     * Retourne le nombre total de biens enregistrés.
     *
     * @return nombre total de biens
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return bienRepository.count();
    }

    /**
     * Retourne le nombre de villes distinctes parmi tous les biens.
     *
     * @return nombre de villes uniques
     */
    @Transactional(readOnly = true)
    public long countDistinctVilles() {
        return bienRepository.countDistinctVilles();
    }

    /**
     * Retourne la répartition des biens par ville sous forme de map triée.
     *
     * @return map nom de ville → nombre de biens
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getBiensParVille() {
        return bienRepository.countParVille().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long)   row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Retourne la répartition des biens par type sous forme de map triée.
     *
     * @return map type de bien → nombre de biens
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getBiensParType() {
        return bienRepository.countParType().stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
