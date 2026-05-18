package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.UserAdminDTO;
import com.kupanga.api.backoffice.dto.UserAdminPageDTO;
import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.backoffice.specification.UserAdminSpecification;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'administration des utilisateurs.
 * Fournit les opérations de recherche paginée, suppression et statistiques pour le back-office.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository        userRepository;
    private final UserAdminSpecification userAdminSpecification;

    /**
     * Recherche paginée des utilisateurs selon les critères admin.
     *
     * @param dto critères de recherche, tri et pagination
     * @return page d'utilisateurs correspondant aux critères
     */
    @Transactional(readOnly = true)
    public UserAdminPageDTO rechercher(UserAdminSearchDTO dto) {
        Pagination pagination = dto.toPagination();
        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );
        Page<UserAdminDTO> page = userRepository
                .findAll(userAdminSpecification.build(dto), pageable)
                .map(UserAdminDTO::from);
        return UserAdminPageDTO.from(page);
    }

    /**
     * Supprime un utilisateur par son identifiant.
     *
     * @param id identifiant de l'utilisateur à supprimer
     */
    @Transactional
    public void supprimer(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Retourne le nombre total d'utilisateurs enregistrés.
     *
     * @return nombre total d'utilisateurs
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return userRepository.count();
    }

    /**
     * Retourne le nombre d'utilisateurs ayant un rôle donné.
     *
     * @param role le rôle à compter
     * @return nombre d'utilisateurs pour ce rôle
     */
    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
