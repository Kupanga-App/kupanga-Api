package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.mapper.ContratMapper;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.research.dto.ContratPageDTO;
import com.kupanga.api.immobilier.research.dto.ContratSearchDTO;
import com.kupanga.api.immobilier.research.specification.ContratSpecification;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContratSearchService {

    private final ContratRepository contratRepository;
    private final ContratSpecification contratSpecification;
    private final ContratMapper contratMapper;
    private final UserService userService;

    public ContratPageDTO rechercher(ContratSearchDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        Pagination pagination = dto.toPagination();

        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );

        Page<ContratDTO> page = contratRepository
                .findAll(contratSpecification.build(dto, user.getId(), user.getRole()), pageable)
                .map(contratMapper::toDTO);

        return ContratPageDTO.from(page);
    }
}
