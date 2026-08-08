package com.sqlprofiler.service;

import org.springframework.stereotype.Component;

@Component
public class SqlNormalizer {

    public String normalize(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        String normalized = sql.trim();

        normalized = normalized.replaceAll(
            "'([^']|'')*'",
            "?"
        );

        normalized = normalized.replaceAll(
            "\\b\\d+(?:\\.\\d+)?\\b",
            "?"
        );

        normalized = normalized.replaceAll(
            "\\s+",
            " "
        );

        return normalized.trim().toUpperCase();
    }
}