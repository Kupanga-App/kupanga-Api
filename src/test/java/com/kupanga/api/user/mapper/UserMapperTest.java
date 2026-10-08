package com.kupanga.api.user.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final String HASH = "$2a$10$vsVhkaAc3xSXEjwWRn1/y.47LEcuQ0SrlL6qlyRtezJVQZauUVVtS";

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("UserDTO ne déclare aucun champ password (P0-4)")
    void userDTO_hasNoPasswordComponent() {
        assertThat(Arrays.stream(UserDTO.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("password");
    }

    @Test
    @DisplayName("toDTO() : le JSON produit ne contient ni le champ password ni le hash (P0-4)")
    void toDTO_neverExposesPasswordHash() throws Exception {
        User user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .mail("john@mail.com")
                .password(HASH)
                .role(Role.ROLE_LOCATAIRE)
                .build();

        String json = objectMapper.writeValueAsString(userMapper.toDTO(user));

        assertThat(json)
                .doesNotContain("password")
                .doesNotContain(HASH)
                .contains("john@mail.com");
    }
}
