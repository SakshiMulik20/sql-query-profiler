package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionOnColumnRule implements DetectionRule {

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "\\b(LOWER|UPPER|TRIM|DATE|COALESCE|ABS|ROUND|SUBSTRING|CAST)" +
        "\\s*\\(\\s*" +
        "([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)",
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

        Matcher matcher = FUNCTION_PATTERN.matcher(whereClause);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String functionName = matcher.group(1).toUpperCase(Locale.ROOT);
        String columnName = matcher.group(2);
        String tableName = extractTableName(trimmedSql);

        return Optional.of(new Finding(
            "FUNCTION_ON_COLUMN",
            "HIGH",
            "HIGH",
            "The WHERE clause applies " + functionName +
                "() directly to column '" + columnName + "'. " +
                "PostgreSQL may be unable to use a normal index on this column " +
                "because it must calculate the function for many rows before comparing values.",
            "Function detected: " + functionName +
                "(" + columnName + ") in the WHERE clause",
            "Avoid applying " + functionName + "() to the column. " +
                "Compare the column using its native value, or create an expression index " +
                "such as: CREATE INDEX idx_" + tableName + "_" +
                functionName.toLowerCase(Locale.ROOT) + " ON " + tableName +
                " (" + functionName + "(" + columnName + "));"
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