package com.kupanga.api.chat.research;

import com.kupanga.api.chat.dto.ConversationDTO;
import com.kupanga.api.chat.dto.ConversationPageDTO;
import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.mapper.ConversationMapper;
import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.chat.repository.MessageRepository;
import com.kupanga.api.chat.research.specification.ConversationSpecification;
import com.kupanga.api.pagination.Pagination;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationSearchService {

    private final ConversationRepository conversationRepository;
    private final ConversationSpecification conversationSpecification;
    private final ConversationMapper conversationMapper;
    private final MessageRepository messageRepository;
    private final UserService userService;

    /**
     * Retourne de maniere paginee les conversations de l'utilisateur connecte,
     * filtrees selon les criteres du dto.
     *
     * @param email email de l'utilisateur connecte
     * @param dto   criteres de recherche et pagination
     * @return une page de conversations
     */
    @Transactional
    public ConversationPageDTO rechercher(String email, ConversationSearchDTO dto) {

        userService.getUserByEmail(email);

        Pagination pagination = dto.toPagination();

        Pageable pageable = PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(pagination.direction(), pagination.sortBy())
        );

        Page<ConversationDTO> page = conversationRepository
                .findAll(conversationSpecification.build(email, dto), pageable)
                .map(conv -> {
                    ConversationDTO base = conversationMapper.toDto(conv);
                    long count = messageRepository.countNonLuByConversationAndDestinataire(conv.getId(), email);
                    return new ConversationDTO(
                            base.id(), base.bienId(), base.bienTitre(),
                            base.emailExpediteur(), base.emailDestinataire(),
                            base.lastMessage(), base.lastMessageAt(), base.createdAt(),
                            count
                    );
                });

        return ConversationPageDTO.from(page);
    }
}
