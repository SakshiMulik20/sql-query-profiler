package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeadingWildcardRule implements DetectionRule {

    private static final Pattern LEADING_WILDCARD_PATTERN = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)" +
            "(?:\\s*::\\s*[A-Za-z_][A-Za-z0-9_]*)?" +
            "\\s+(?:NOT\\s+)?(?:LIKE|ILIKE)\\s+'%[^']*'",
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
        Matcher matcher = LEADING_WILDCARD_PATTERN.matcher(whereClause);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String columnName = matcher.group(1);

        return Optional.of(new Finding(
            "LEADING_WILDCARD",
            "MEDIUM",
            "HIGH",
            "The query searches column '" + columnName +
                "' with a pattern beginning with '%'. " +
                "A leading wildcard prevents a normal B-tree index " +
                "from efficiently locating the beginning of the value.",
            "Leading wildcard pattern detected for column: " + columnName,
            "Avoid starting the search pattern with '%'. " +
                "For contains-search, consider PostgreSQL pg_trgm and a GIN " +
                "or GiST index, for example: " +
                "CREATE INDEX idx_" + columnName.replace(".", "_") +
                "_trgm ON demo_orders USING gin (" + columnName +
                " gin_trgm_ops);"
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