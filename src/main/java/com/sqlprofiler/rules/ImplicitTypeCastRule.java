package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImplicitTypeCastRule implements DetectionRule {

    private static final Pattern POSTGRES_CAST_PATTERN = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)" +
        "\\s*::\\s*([A-Za-z_][A-Za-z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {
        if (sql == null || sql.isBlank()) {
            return Optional.empty();
        }

        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase(Locale.ROOT);

        if (!upperSql.startsWith("SELECT")) {
            return Optional.empty();
        }

        if (upperSql.contains("PG_")
                || upperSql.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        String explain = explainOutput == null ? "" : explainOutput;

        String searchableText = trimmedSql + "\n" + explain;
        Matcher matcher = POSTGRES_CAST_PATTERN.matcher(searchableText);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String columnName = matcher.group(1);
        String castType = matcher.group(2);
        String tableName = extractTableName(trimmedSql);

        return Optional.of(new Finding(
            "IMPLICIT_TYPE_CAST",
            "HIGH",
            "HIGH",
            "Column '" + columnName + "' is being converted to type '" +
                castType + "'. Type conversion on a filtered column can prevent " +
                "PostgreSQL from using the column's normal index efficiently.",
            "Type cast detected: " + columnName + "::" + castType,
            "Compare the value using the column's native type. " +
                "For example, use WHERE customer_id = 42 instead of " +
                "WHERE customer_id::text = '42'."
        ));
    }

    private String extractTableName(String sql) {
        String[] words = sql.trim().split("\\s+");

        for (int i = 0; i < words.length - 1; i++) {
            if ("FROM".equalsIgnoreCase(words[i])) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9_]", "");
            }
        }

        return "the_table";
    }
}