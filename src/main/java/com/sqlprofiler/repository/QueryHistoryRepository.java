package com.sqlprofiler.repository;

import com.sqlprofiler.model.QueryHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueryHistoryRepository
        extends JpaRepository<QueryHistory, Long> {

    List<QueryHistory> findTop50ByOrderByAnalyzedAtDesc();

    List<QueryHistory> findTop50ByNormalizedQueryOrderByAnalyzedAtDesc(
        String normalizedQuery
    );
}