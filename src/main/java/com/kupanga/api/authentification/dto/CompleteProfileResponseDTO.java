package com.kupanga.api.authentification.dto;

import com.kupanga.api.user.dto.readDTO.UserDTO;
import lombok.Builder;

@Builder
public record CompleteProfileResponseDTO(

        UserDTO userDTO ,
        AuthResponseDTO authResponseDTO
) {
}
