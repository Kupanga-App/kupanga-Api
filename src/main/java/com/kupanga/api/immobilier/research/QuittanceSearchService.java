package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.research.dto.QuittancePageDTO;
import com.kupanga.api.immobilier.research.dto.QuittanceSearchDTO;
import com.kupanga.api.immobilier.research.sort.QuittanceSortEnum;
import com.kupanga.api.immobilier.research.specification.QuittanceSpecification;
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
public class QuittanceSearchService {

    private final QuittanceRepository quittanceRepository;
    private final QuittanceSpecification quittanceSpecification;
    private final QuittanceMapper quittanceMapper;
    private final UserService userService;

    public QuittancePageDTO rechercher(QuittanceSearchDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        Pagination pagination = dto.toPagination();

        // tri compound annee + mois quand sortBy = "annee" (défaut)
        Sort sort;
        if (QuittanceSortEnum.ANNEE.getFieldName().equals(pagination.sortBy())) {
            sort = Sort.by(pagination.direction(), "annee")
                       .and(Sort.by(pagination.direction(), "mois"));
        } else {
            sort = Sort.by(pagination.direction(), pagination.sortBy());
        }

        Pageable pageable = PageRequest.of(pagination.page(), pagination.size(), sort);

        Page<QuittanceDTO> page = quittanceRepository
                .findAll(quittanceSpecification.build(dto, user.getId(), user.getRole()), pageable)
                .map(quittanceMapper::toDTO);

        return QuittancePageDTO.from(page);
    }
}
