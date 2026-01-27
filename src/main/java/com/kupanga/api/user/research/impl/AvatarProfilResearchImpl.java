package com.kupanga.api.user.research.impl;

import com.kupanga.api.pagination.model.Pagination;
import com.kupanga.api.user.dto.paginationDTO.AvatarProfilPagination;
import com.kupanga.api.user.research.researchDTO.AvatarProfileResearchDTO;
import com.kupanga.api.user.entity.AvatarProfil;
import com.kupanga.api.user.mapper.AvatarProfilMapper;
import com.kupanga.api.user.research.service.AvatarProfilResearch;
import com.kupanga.api.user.service.AvatarProfilService;
import com.kupanga.api.pagination.sort.SortEnum;
import com.kupanga.api.user.research.specification.AvatarProfileSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvatarProfilResearchImpl implements AvatarProfilResearch {

    private final AvatarProfilService avatarProfilService;
    private final AvatarProfilMapper avatarProfilMapper;

    @Override
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
