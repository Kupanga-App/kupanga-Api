package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.mapper.UserMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {UserMapper.class, DocumentPdfUrlMapper.class})
public interface ContratMapper {

    @Mapping(target = "bienId",                source = "bien.id")
    @Mapping(target = "proprietaire",          source = "proprietaire",
            qualifiedByName = "mapUserSansInfosSensibles")
    @Mapping(target = "locataire",             source = "locataire",
            qualifiedByName = "mapUserSansInfosSensibles")
    @Mapping(target = "proprietaireASigné",    expression = "java(contrat.getSignatureProprietaire() != null)")
    @Mapping(target = "locataireASigné",       expression = "java(contrat.getSignatureLocataire() != null)")
    @Mapping(target = "urlPdf",                source = "clePdf", qualifiedByName = "urlContrat")
    ContratDTO toDTO(Contrat contrat);

    // ─── User sans infos sensibles : ni id, ni rôle, ni état du profil (vue aussi servie par token public) ──
    @Named("mapUserSansInfosSensibles")
    @Mapping(target = "id",                ignore = true)
    @Mapping(target = "role",              ignore = true)
    @Mapping(target = "hasCompleteProfil", ignore = true)
    UserDTO mapUserSansInfosSensibles(User user);
}