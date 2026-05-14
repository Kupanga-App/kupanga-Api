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

@Service
@RequiredArgsConstructor
public class BienAdminService {

    private final BienRepository        bienRepository;
    private final BienAdminSpecification bienAdminSpecification;

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

    @Transactional
    public void supprimer(Long id) {
        bienRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return bienRepository.count();
    }

    @Transactional(readOnly = true)
    public long countDistinctVilles() {
        return bienRepository.countDistinctVilles();
    }

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
