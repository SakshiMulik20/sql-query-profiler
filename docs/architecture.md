# Architecture

## Stage 1A Architecture

```mermaid
flowchart TD
    Client[Developer or Postman]

    Client --> Controller[QueryController]

    Controller --> Analyze[POST /api/analyze]
    Controller --> History[GET /api/history]
    Controller --> Compare[POST /api/compare]

    Analyze --> Service[QueryAnalyzerService]
    Compare --> Service
    History --> Repository[QueryHistoryRepository]

    Service --> Validator[QueryValidator]
    Service --> Explain[Safe EXPLAIN ANALYZE]
    Service --> Rules[15 Detection Rules]
    Service --> Normalizer[SqlNormalizer]
    Service --> Repository

    Explain --> PostgreSQL[(PostgreSQL)]
    Validator --> Service

    Rules --> Report[QueryReport]
    Normalizer --> Report

    Repository --> PostgreSQL

    PostgreSQL --> DemoTable[demo_orders]
    PostgreSQL --> HistoryTable[query_history]

    Report --> Client
    Repository --> Client
```

## Before-and-After Comparison

```mermaid
sequenceDiagram
    participant D as Developer
    participant A as SQL Query Profiler
    participant DB as PostgreSQL

    D->>A: Submit query before fix
    A->>DB: Run EXPLAIN ANALYZE
    DB-->>A: Plan and execution metrics
    A-->>D: Findings and recommended fix

    D->>DB: Apply fix manually

    D->>A: Submit comparison request
    A->>DB: Run EXPLAIN ANALYZE again
    DB-->>A: New plan and execution metrics
    A-->>D: Before/after comparison
```

## Database Environment

```mermaid
flowchart LR
    Spring[Spring Boot application<br/>localhost:8080]

    Host[Host machine<br/>localhost:5433]

    Docker[Docker PostgreSQL container<br/>port 5432]

    Demo[demo_orders<br/>50,000 demo rows]

    History[query_history<br/>saved analysis results]

    Spring --> Host
    Host --> Docker
    Docker --> Demo
    Docker --> History
```

## Current Responsibilities

| Component | Responsibility |
|---|---|
| `QueryController` | Receives HTTP requests and returns responses |
| `QueryAnalyzerService` | Coordinates validation, analysis, rules, normalization, and history |
| `QueryValidator` | Rejects unsafe or invalid input before database execution |
| Safe EXPLAIN flow | Collects query plans and execution metrics |
| Detection rules | Identify possible SQL performance problems |
| `SqlNormalizer` | Groups similar SQL queries into one structural pattern |
| `QueryHistoryRepository` | Saves and retrieves analysis history |
| PostgreSQL | Stores demo data and profiler history |
| Docker Compose | Provides a reproducible PostgreSQL environment |

## Future Stage 1B Architecture

The monitoring architecture will be added later:

```mermaid
flowchart TD
    Scheduler[Scheduled Monitoring Worker]

    Collector[PostgreSQL Snapshot Collector]

    Slow[Slow Query Detector]
    Locks[Lock Wait Detector]
    Connections[Connection Saturation Detector]
    Frequency[Query Frequency Detector]

    Incidents[Incident Store]

    Dashboard[Incident Dashboard]

    Health[Worker Health]

    Scheduler --> Collector
    Scheduler --> Health

    Collector --> Slow
    Collector --> Locks
    Collector --> Connections
    Collector --> Frequency

    Slow --> Incidents
    Locks --> Incidents
    Connections --> Incidents
    Frequency --> Incidents

    Incidents --> Dashboard
    Health --> Dashboard
```

The Stage 1B components shown above are planned and are not part of the current Stage 1A implementation.