package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.mapper.UserMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ContratMapper {

    @Mapping(target = "bienId",                source = "bien.id")
    @Mapping(target = "proprietaire",          source = "proprietaire",
            qualifiedByName = "mapUserSansInfosSensibles")
    @Mapping(target = "locataire",             source = "locataire",
            qualifiedByName = "mapUserSansInfosSensibles")
    @Mapping(target = "proprietaireASigné",    expression = "java(contrat.getSignatureProprietaire() != null)")
    @Mapping(target = "locataireASigné",       expression = "java(contrat.getSignatureLocataire() != null)")
    ContratDTO toDTO(Contrat contrat);

    // ─── User sans id ni password ─────────────────────────────────────────────
    @Named("mapUserSansInfosSensibles")
    @Mapping(target = "password", ignore = true)
    UserDTO mapUserSansInfosSensibles(User user);
}