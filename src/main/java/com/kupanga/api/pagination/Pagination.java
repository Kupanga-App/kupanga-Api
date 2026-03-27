package com.kupanga.api.pagination;

import lombok.*;
import org.springframework.data.domain.Sort;

@Builder
public record Pagination(

        int page,
        int size,
        String sortBy,
        Sort.Direction direction
) {

}
