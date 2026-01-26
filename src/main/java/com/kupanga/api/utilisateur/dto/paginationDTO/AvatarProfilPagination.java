package com.kupanga.api.utilisateur.dto.paginationDTO;

import com.kupanga.api.pagination.model.Pagination;
import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class AvatarProfilPagination {

    private List<AvatarProfilDTO> avatarProfilDTOList;
    private long totalElements;
    private int totalPages;
    private Pagination pagination;

}
