package com.sqlprofiler.service;

import java.util.NoSuchElementException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlprofiler.model.Finding;
import com.sqlprofiler.model.QueryHistory;
import com.sqlprofiler.model.QueryReport;
import com.sqlprofiler.repository.QueryHistoryRepository;
import com.sqlprofiler.rules.CartesianJoinRule;
import com.sqlprofiler.rules.CorrelatedSubqueryRule;
import com.sqlprofiler.rules.DetectionRule;
import com.sqlprofiler.rules.FunctionOnColumnRule;
import com.sqlprofiler.rules.HighRowEstimateRule;
import com.sqlprofiler.rules.ImplicitTypeCastRule;
import com.sqlprofiler.rules.LeadingWildcardRule;
import com.sqlprofiler.rules.NestedLoopRule;
import com.sqlprofiler.rules.NoLimitRule;
import com.sqlprofiler.rules.NoWhereClauseRule;
import com.sqlprofiler.rules.NotInSubqueryRule;
import com.sqlprofiler.rules.OrOnIndexedColumnRule;
import com.sqlprofiler.rules.OrderByNoIndexRule;
import com.sqlprofiler.rules.SelectStarRule;
import com.sqlprofiler.rules.SeqScanRule;
import com.sqlprofiler.rules.UnparameterizedQueryRule;
import com.sqlprofiler.safety.QueryValidator;
import com.sqlprofiler.model.ComparisonReport;

@Service
public class QueryAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalyzerService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueryValidator validator;
    
    @Autowired
    private QueryHistoryRepository historyRepository;

    @Autowired
    private SqlNormalizer sqlNormalizer;

    @Autowired
    private ObjectMapper objectMapper;

    // Register all rules here — add new ones to this list as you build them
    private final List<DetectionRule> rules = List.of(
        new SeqScanRule(),
        new NoWhereClauseRule(),
        new SelectStarRule(),
        new NoLimitRule(),
        new FunctionOnColumnRule(),
        new CartesianJoinRule(),
        new ImplicitTypeCastRule(),
        new NestedLoopRule(),
        new HighRowEstimateRule(),
        new OrOnIndexedColumnRule(),
        new LeadingWildcardRule(),
        new OrderByNoIndexRule(),
        new CorrelatedSubqueryRule(),
        new NotInSubqueryRule(),
        new UnparameterizedQueryRule()
 
        // NoWhereClauseRule, SelectStarRule, etc. will go here
    );

    public QueryReport analyze(String sqlQuery) {
        QueryReport report = new QueryReport();
        report.setOriginalQuery(sqlQuery);
        
        log.info(
        	    "event=analysis_started query_length={}",
        	    sqlQuery.length()
        	);

        // Step 1 — Validate before touching the database
        try {
            validator.validate(sqlQuery);
        } catch (IllegalArgumentException e) {
        	log.warn(
        		    "event=validation_failed status=INVALID reason=input_rejected"
        		);
            report.setOverallStatus("INVALID");
            report.addFinding(new Finding(
                "VALIDATION_ERROR",
                "HIGH",
                "HIGH",
                e.getMessage(),
                "Input validation",
                ""
            ));
            return report;
        }

        // Step 2 — Run EXPLAIN ANALYZE safely
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Safety: abort if query runs longer than 10 seconds
            stmt.execute("SET statement_timeout = '10s'");

            log.info(
            	    "event=explain_started query_length={} timeout_seconds=10",
            	    sqlQuery.length()
            	);

            ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + sqlQuery);

            StringBuilder explainOutput = new StringBuilder();
            while (rs.next()) {
                explainOutput.append(rs.getString(1)).append("\n");
            }

            String output = explainOutput.toString();

            // Step 3 — Extract metrics
            report.setExecutionTime(extractExecutionTime(output));
            report.setRowsScanned(extractRowsScanned(output));

            // Step 4 — Run every detection rule
            for (DetectionRule rule : rules) {
                Optional<Finding> finding = rule.analyze(sqlQuery, output);
                finding.ifPresent(report::addFinding);
            }

            // Step 5 — If no rules fired, query is clean
//            if (!report.hasFindings()) {
//                log.info("Analysis complete — no issues found");
//            } else {
//                log.info("Analysis complete — {} finding(s)", report.getFindings().size());
//            }
            log.info(
            	    "event=analysis_completed status={} findings_count={} execution_time_ms={} rows_scanned={}",
            	    report.getOverallStatus(),
            	    report.getFindings().size(),
            	    report.getExecutionTime(),
            	    report.getRowsScanned()
            	);
            
            saveToHistory(report);

        } catch (SQLException e) {
            // Never log the full exception — it can contain credentials
        	log.error(
        		    "event=analysis_failed status=ERROR sql_state={}",
        		    e.getSQLState()
        		);
            report.setOverallStatus("ERROR");
            report.addFinding(new Finding(
                "ANALYSIS_ERROR",
                "HIGH",
                "HIGH",
                "Could not analyze query. Please check your database connection.",
                "Database connection",
                ""
            ));
        }

        return report;
    }
    
    public ComparisonReport compare(
    	    Long beforeHistoryId,
    	    String afterQuery
    	) {
    	    if (beforeHistoryId == null) {
    	        throw new IllegalArgumentException(
    	            "beforeHistoryId is required."
    	        );
    	    }

    	    if (afterQuery == null || afterQuery.isBlank()) {
    	        throw new IllegalArgumentException(
    	            "afterQuery is required."
    	        );
    	    }

    	    QueryHistory before = historyRepository.findById(beforeHistoryId)
    	        .orElseThrow(() -> new NoSuchElementException(
    	            "Query history record not found: " + beforeHistoryId
    	        ));

    	    log.info(
    	        "Starting before/after comparison for history ID: {}",
    	        beforeHistoryId
    	    );

    	    QueryReport after = analyze(afterQuery);

    	    double beforeExecutionTime = before.getExecutionTime();
    	    double afterExecutionTime = after.getExecutionTime();

    	    double executionTimeImprovementMs =
    	        beforeExecutionTime - afterExecutionTime;

    	    double improvementPercentage = 0.0;

    	    if (beforeExecutionTime > 0) {
    	        improvementPercentage =
    	            (executionTimeImprovementMs / beforeExecutionTime) * 100.0;
    	    }

    	    String performanceStatus;

    	    if ("ERROR".equals(after.getOverallStatus())
    	        || "INVALID".equals(after.getOverallStatus())) {

    	        performanceStatus = "ANALYSIS_FAILED";

    	    } else if (afterExecutionTime < beforeExecutionTime) {

    	        performanceStatus = "IMPROVED";

    	    } else if (afterExecutionTime > beforeExecutionTime) {

    	        performanceStatus = "REGRESSED";

    	    } else {

    	        performanceStatus = "NO_CHANGE";
    	    }

    	    boolean statusImproved = statusRank(after.getOverallStatus())
    	        < statusRank(before.getOverallStatus());

    	    return new ComparisonReport(
    	        before,
    	        after,
    	        round(beforeExecutionTime),
    	        round(afterExecutionTime),
    	        round(executionTimeImprovementMs),
    	        round(improvementPercentage),
    	        performanceStatus,
    	        statusImproved
    	    );
    	}
    
    private int statusRank(String status) {
        if ("OPTIMIZED".equals(status)) {
            return 0;
        }

        if ("ISSUES_FOUND".equals(status)) {
            return 1;
        }

        if ("CRITICAL".equals(status)) {
            return 2;
        }

        if ("INVALID".equals(status)
            || "ERROR".equals(status)) {
            return 3;
        }

        return 4;
    }
    
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double extractExecutionTime(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("Execution Time:")) {
                try {
                    return Double.parseDouble(
                        line.split(":")[1].trim().replace(" ms", "")
                    );
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private long extractRowsScanned(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("rows=")) {
                try {
                    String[] parts = line.split("rows=");
                    if (parts.length > 1) {
                        return Long.parseLong(parts[1].trim().split("[^0-9]")[0]);
                    }
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    private void saveToHistory(QueryReport report) {
        QueryHistory history = new QueryHistory();

        history.setOriginalQuery(report.getOriginalQuery());
        history.setNormalizedQuery(
            sqlNormalizer.normalize(report.getOriginalQuery())
        );
        history.setExecutionTime(report.getExecutionTime());
        history.setRowsScanned(report.getRowsScanned());
        history.setOverallStatus(report.getOverallStatus());
        history.setAnalyzedAt(LocalDateTime.now());

        try {
            history.setFindingsJson(
                objectMapper.writeValueAsString(report.getFindings())
            );
        } catch (JsonProcessingException e) {
            log.error("Could not serialize query findings");
            history.setFindingsJson("[]");
        }

        QueryHistory savedHistory = historyRepository.save(history);

        log.info(
            "event=history_saved history_id={} status={} findings_count={}",
            savedHistory.getId(),
            report.getOverallStatus(),
            report.getFindings().size()
        );
    }
}