package com.kupanga.api.utilisateur.service.research.impl;

import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.utilisateur.dto.AvatarProfilPagination;
import com.kupanga.api.utilisateur.dto.researchDTO.AvatarProfileResearchDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import com.kupanga.api.utilisateur.mapper.AvatarProfilMapper;
import com.kupanga.api.utilisateur.service.AvatarProfilService;
import com.kupanga.api.utilisateur.service.research.sort.SortEnum;
import com.kupanga.api.utilisateur.service.research.specification.AvatarProfileSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvatarProfilResearchImpl {

    private final AvatarProfilService avatarProfilService;
    private final AvatarProfilMapper avatarProfilMapper;

    public AvatarProfilPagination research(AvatarProfileResearchDTO avatarProfileResearchDTO){

        Specification<AvatarProfil> spec = AvatarProfileSpecification.searchAll();

        Pagination pagination = new Pagination();

        if(avatarProfileResearchDTO.getPageNumber().isPresent()){

            pagination.setPage(avatarProfileResearchDTO.getPageNumber().get());
        }

        if(avatarProfileResearchDTO.getPageSize().isPresent()){

            pagination.setSize(avatarProfileResearchDTO.getPageSize().get());
        }

        Sort sort = getSort(avatarProfileResearchDTO , pagination);

        Pageable pageable = PageRequest.of(pagination.getPage() , pagination.getSize() , sort );
        Page<AvatarProfil> page = avatarProfilService.finAll(spec , pageable);

        return AvatarProfilPagination.builder()
                .avatarProfilDTOList(avatarProfilMapper.toDTOList(page.getContent()))
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .pagination(pagination)
                .build();
    }

    private Sort getSort(AvatarProfileResearchDTO avatarProfileResearchDTO , Pagination pagination){

        String sortBy = avatarProfileResearchDTO.getSort().isEmpty() ? null : avatarProfileResearchDTO.getSort().get();

        if(!(SortEnum.isValidField(sortBy))){

            return Sort.by( pagination.getDirection() , pagination.getSortBy());
        }
        return Sort.by( pagination.getDirection() , avatarProfileResearchDTO.getSort().get());
    }
}
