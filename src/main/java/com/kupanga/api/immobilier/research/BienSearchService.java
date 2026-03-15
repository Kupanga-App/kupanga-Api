package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.research.dto.BienPageDTO;
import com.kupanga.api.immobilier.research.dto.BienSearchDTO;
import com.kupanga.api.immobilier.research.dto.Pagination;
import com.kupanga.api.immobilier.research.specification.BienSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BienSearchService {

    private final BienRepository bienRepository;
    private final BienSpecification bienSpecification;
    private final BienMapper bienMapper;

    public BienPageDTO rechercher(BienSearchDTO dto) {
        Pagination pagination = dto.toPagination();

        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );

        Page<BienDTO> page = bienRepository
                .findAll(bienSpecification.build(dto), pageable)
                .map(bienMapper::toDTO);

        return BienPageDTO.from(page);
    }
}
