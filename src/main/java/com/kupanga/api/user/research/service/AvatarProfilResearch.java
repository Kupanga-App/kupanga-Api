package com.kupanga.api.user.research.service;

import com.kupanga.api.user.dto.paginationDTO.AvatarProfilPagination;
import com.kupanga.api.user.research.researchDTO.AvatarProfileResearchDTO;

public interface AvatarProfilResearch {

    AvatarProfilPagination research(AvatarProfileResearchDTO avatarProfileResearchDTO) ;
}
