package com.kupanga.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public String health() {

        jdbcTemplate.execute("SELECT 1");

        return "OK : render et neon reveillé";
    }
}
