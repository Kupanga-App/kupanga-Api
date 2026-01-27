package com.kupanga.api.user.dto.paginationDTO;

import com.kupanga.api.pagination.model.Pagination;
import com.kupanga.api.user.dto.readDTO.AvatarProfilDTO;
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
