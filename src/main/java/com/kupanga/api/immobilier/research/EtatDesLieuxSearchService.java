package com.kupanga.api.immobilier.research;

import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxPageDTO;
import com.kupanga.api.immobilier.research.dto.EtatDesLieuxSearchDTO;
import com.kupanga.api.immobilier.research.specification.EtatDesLieuxSpecification;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service de recherche paginée des états des lieux.
 * Filtre les EDL accessibles à l'utilisateur connecté (propriétaire ou locataire).
 */
@Service
@RequiredArgsConstructor
public class EtatDesLieuxSearchService {

    private final EtatDesLieuxRepository edlRepository;
    private final EtatDesLieuxSpecification edlSpecification;
    private final EtatDesLieuxMapper edlMapper;
    private final UserService userService;

    /**
     * Recherche paginée des états des lieux selon les critères fournis.
     * Seuls les EDL accessibles à l'utilisateur connecté sont retournés.
     *
     * @param dto   critères de recherche, tri et pagination
     * @param email email de l'utilisateur connecté
     * @return page d'états des lieux correspondant aux critères
     */
    public EtatDesLieuxPageDTO rechercher(EtatDesLieuxSearchDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        Pagination pagination = dto.toPagination();

        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );

        Page<EtatDesLieuxDTO> page = edlRepository
                .findAll(edlSpecification.build(dto, user.getId(), user.getRole()), pageable)
                .map(edlMapper::toDTO);

        return EtatDesLieuxPageDTO.from(page);
    }
}
