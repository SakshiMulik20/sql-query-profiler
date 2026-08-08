package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrOnIndexedColumnRule implements DetectionRule {

    private static final Pattern SAME_COLUMN_OR_PATTERN = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)" +
            "\\s*(?:=|<>|!=|<=|>=|<|>|LIKE|ILIKE)\\s*" +
            "(?:'[^']*'|\"[^\"]*\"|[^\\s()]+)" +
            "\\s+OR\\s+" +
            "\\1\\s*(?:=|<>|!=|<=|>=|<|>|LIKE|ILIKE)",
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

        String whereClause = extractWhereClause(trimmedSql);

        if (whereClause.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = SAME_COLUMN_OR_PATTERN.matcher(whereClause);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String columnName = matcher.group(1);

        return Optional.of(new Finding(
            "OR_ON_INDEXED_COLUMN",
            "MEDIUM",
            "MEDIUM",
            "The WHERE clause contains multiple OR conditions on '" +
                columnName + "'. OR conditions can make it harder for " +
                "PostgreSQL to use one efficient index path and may result " +
                "in extra scanning or bitmap operations.",
            "Repeated OR condition detected for column: " + columnName,
            "Compare the execution plan with separate queries combined using " +
                "UNION ALL, or verify that the filtered column has a suitable " +
                "index. Test the rewritten query with EXPLAIN ANALYZE."
        ));
    }

    private String extractWhereClause(String sql) {
        String upperSql = sql.toUpperCase(Locale.ROOT);
        int whereIndex = upperSql.indexOf("WHERE");

        if (whereIndex < 0) {
            return "";
        }

        String clause = sql.substring(whereIndex + "WHERE".length());
        String upperClause = clause.toUpperCase(Locale.ROOT);

        String[] endingKeywords = {
            " GROUP BY",
            " ORDER BY",
            " LIMIT",
            " OFFSET",
            " FETCH",
            " FOR "
        };

        int endIndex = clause.length();

        for (String keyword : endingKeywords) {
            int keywordIndex = upperClause.indexOf(keyword);

            if (keywordIndex >= 0 && keywordIndex < endIndex) {
                endIndex = keywordIndex;
            }
        }

        return clause.substring(0, endIndex);
    }
}