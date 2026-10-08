package com.kupanga.api.backoffice.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pagination du back-office : paramètres hors bornes ramenés dans les limites (plus de 500).
 */
@DisplayName("Tests unitaires — bornes de pagination du back-office")
class AdminSearchDTOTest {

    @Test
    @DisplayName("BienAdminSearchDTO — page=-1 → 0, size=0 → 1, size=10000 → 100")
    void bienAdminSearch_bornes() {
        assertThat(new BienAdminSearchDTO(null, null, null, -1, 0))
                .extracting(BienAdminSearchDTO::page, BienAdminSearchDTO::size).containsExactly(0, 1);
        assertThat(new BienAdminSearchDTO(null, null, null, 2, 10000).size()).isEqualTo(BienAdminSearchDTO.SIZE_MAX);
        assertThat(new BienAdminSearchDTO(null, null, null, 2, 10))
                .extracting(BienAdminSearchDTO::page, BienAdminSearchDTO::size).containsExactly(2, 10);
    }

    @Test
    @DisplayName("UserAdminSearchDTO — page=-1 → 0, size=0 → 1, size=10000 → 100")
    void userAdminSearch_bornes() {
        assertThat(new UserAdminSearchDTO(null, null, null, null, -1, 0))
                .extracting(UserAdminSearchDTO::page, UserAdminSearchDTO::size).containsExactly(0, 1);
        assertThat(new UserAdminSearchDTO(null, null, null, null, 2, 10000).size()).isEqualTo(UserAdminSearchDTO.SIZE_MAX);
        assertThat(new UserAdminSearchDTO(null, null, null, null, 2, 10))
                .extracting(UserAdminSearchDTO::page, UserAdminSearchDTO::size).containsExactly(2, 10);
    }
}
