package com.kupanga.api.user.research;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.mapper.UserMapper;
import com.kupanga.api.user.repository.UserRepository;
import com.kupanga.api.user.research.dto.LocatairePageDTO;
import com.kupanga.api.user.research.dto.LocataireSearchDTO;
import com.kupanga.api.user.research.specification.LocataireSpecification;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocataireSearchService {

    private final UserRepository      userRepository;
    private final LocataireSpecification locataireSpecification;
    private final UserMapper          userMapper;
    private final UserService userService;
    private final BienService bienService;

    /**
     * Retourne de manière paginée les utilisateurs ayant envoyé un message
     * dans une conversation liée au bien identifié par {@code bienId},
     * filtrés selon les critères du {@code dto}.
     *
     * @param bienId l'identifiant du bien
     * @param dto    les critères de recherche et de pagination
     * @param email email propriétaire
     * @return une page de {@link UserDTO}
     */
    public LocatairePageDTO rechercher( String email, Long bienId, LocataireSearchDTO dto) {

        User user = userService.getUserByEmail(email);

        if( !bienService.existsByIdAndProprietaireId(bienId , user.getId())){
            throw new KupangaBusinessException("Ce bien ne vous appartient pas." ,
                    HttpStatus.UNAUTHORIZED);
        }

        Pagination pagination = dto.toPagination();

        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );

        Page<UserDTO> page = userRepository
                .findAll(locataireSpecification.build(bienId, dto), pageable)
                .map(userMapper::toDTOWithoutCredentials);

        return LocatairePageDTO.from(page);
    }
}