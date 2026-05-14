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

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository        userRepository;
    private final UserAdminSpecification userAdminSpecification;

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

    @Transactional
    public void supprimer(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
