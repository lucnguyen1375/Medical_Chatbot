package com.medicalchatbot.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UUID> findIdByUsername(String username) {
        return jdbcTemplate.query(
                "select id from app_users where username = ?",
                rs -> rs.next() ? Optional.of(rs.getObject("id", UUID.class)) : Optional.empty(),
                username
        );
    }
}
