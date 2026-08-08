package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;
import java.util.Optional;

public interface DetectionRule {
    Optional<Finding> analyze(String sql, String explainOutput);
}