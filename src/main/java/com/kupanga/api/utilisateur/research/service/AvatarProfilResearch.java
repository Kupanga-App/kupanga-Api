package com.kupanga.api.utilisateur.research.service;

import com.kupanga.api.utilisateur.dto.paginationDTO.AvatarProfilPagination;
import com.kupanga.api.utilisateur.research.researchDTO.AvatarProfileResearchDTO;

public interface AvatarProfilResearch {

    AvatarProfilPagination research(AvatarProfileResearchDTO avatarProfileResearchDTO) ;
}
