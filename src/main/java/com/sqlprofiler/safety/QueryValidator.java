package com.sqlprofiler.safety;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class QueryValidator {

    // These words at the start of a query mean it will CHANGE data — never allowed
    private static final List<String> BLOCKED_STARTS = List.of(
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE",
        "ALTER", "TRUNCATE", "GRANT", "REVOKE", "REPLACE"
    );

    private static final int MAX_QUERY_LENGTH = 5000;

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty.");
        }

        if (sql.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Query is too long. Maximum " + MAX_QUERY_LENGTH + " characters.");
        }

        // Remove extra spaces and check what the query starts with
        String trimmed = sql.strip().toUpperCase();

        for (String blocked : BLOCKED_STARTS) {
            if (trimmed.startsWith(blocked)) {
                throw new IllegalArgumentException(
                    "Only SELECT queries are allowed for analysis. " +
                    "'" + blocked + "' statements are not permitted."
                );
            }
        }

        // Block multiple statements (SQL injection via semicolon)
        // Allow trailing semicolon but not one in the middle
        String withoutTrailing = trimmed.endsWith(";") 
            ? trimmed.substring(0, trimmed.length() - 1) 
            : trimmed;
        
        if (withoutTrailing.contains(";")) {
            throw new IllegalArgumentException(
                "Multiple SQL statements are not allowed."
            );
        }
    }
}