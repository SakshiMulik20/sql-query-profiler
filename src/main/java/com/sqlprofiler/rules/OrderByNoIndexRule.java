package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderByNoIndexRule implements DetectionRule {

    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
        "\\bORDER\\s+BY\\s+([^;]+?)(?=\\s+LIMIT\\b|\\s+OFFSET\\b|\\s+FETCH\\b|$)",
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

        if (explainOutput == null || explainOutput.isBlank()) {
            return Optional.empty();
        }

        Matcher orderMatcher = ORDER_BY_PATTERN.matcher(trimmedSql);

        if (!orderMatcher.find()) {
            return Optional.empty();
        }

        String planUpper = explainOutput.toUpperCase(Locale.ROOT);

        if (!planUpper.contains("SORT")) {
            return Optional.empty();
        }

        String orderedColumns = orderMatcher.group(1).trim();

        return Optional.of(new Finding(
            "ORDER_BY_NO_INDEX",
            "MEDIUM",
            "MEDIUM",
            "The query sorts results by '" + orderedColumns +
                "'. PostgreSQL used a sort step instead of returning rows " +
                "already ordered by a useful index. Sorting a large result " +
                "set can consume significant CPU and memory and may spill to disk.",
            "ORDER BY detected with a Sort step in the execution plan: " +
                orderedColumns,
            "Consider an index that matches the ORDER BY columns, for example: " +
                "CREATE INDEX idx_table_order ON table_name (" +
                orderedColumns.replaceAll("\\s+(ASC|DESC)\\b", "") +
                ");"
        ));
    }
}