package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.BienAdminDTO;
import com.kupanga.api.backoffice.dto.BienAdminPageDTO;
import com.kupanga.api.backoffice.dto.BienAdminSearchDTO;
import com.kupanga.api.backoffice.specification.BienAdminSpecification;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.pagination.Pagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service d'administration des biens immobiliers.
 * Fournit les opérations de recherche paginée, suppression et statistiques pour le back-office.
 */
@Service
@RequiredArgsConstructor
public class BienAdminService {

    private final BienRepository        bienRepository;
    private final BienAdminSpecification bienAdminSpecification;

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
     * Supprime un bien par son identifiant.
     *
     * @param id identifiant du bien à supprimer
     */
    @Transactional
    public void supprimer(Long id) {
        bienRepository.deleteById(id);
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
