# 🔍 SQL Query Profiler

SQL Query Profiler is a Spring Boot application that analyzes PostgreSQL queries, detects common performance problems, explains the evidence, and suggests possible fixes.

It also stores query-analysis history and compares performance before and after a developer manually applies a fix.

---

## 🚀 Current Status

**Stage 1A is complete.**

### ✅ Implemented

- 🔎 Manual SQL analysis
- 🛡️ Safe `EXPLAIN ANALYZE` collection
- 🧠 15 SQL performance detection rules
- ⚠️ Severity and confidence for findings
- 📝 Query history
- 🔄 SQL query normalization
- 💡 Suggested SQL fixes
- 📊 Before-and-after performance comparison
- 🐘 Docker Compose PostgreSQL demo database
- 🧪 Automated tests
- ⚙️ GitHub Actions CI pipeline
- 📋 Basic structured logs

### 🔮 Planned Next

- 🤖 Automatic monitoring
- 🐌 Active slow-query detection
- 🔒 Lock-wait detection
- 🔌 Connection saturation detection
- 📈 Query frequency monitoring
- 🚨 Incident dashboard
- 🔔 Threshold-based alerts
- ❤️ Worker health monitoring

---

## ⚙️ How It Works

```text
Developer submits SQL
        ↓
Query validation
        ↓
Safe EXPLAIN ANALYZE
        ↓
Execution metrics collected
        ↓
15 detection rules run
        ↓
Findings and recommendations returned
        ↓
Analysis saved to query history
```

### Before-and-After Performance Comparison

The profiler allows developers to measure the impact of a manually applied SQL optimization.

```text
Analyze query before fix
        ↓
Developer reviews recommendation
        ↓
Developer applies fix manually
        ↓
Analyze the query again
        ↓
Before/after comparison returned
```

---

## 🏗️ Architecture

The detailed system architecture, database environment, component responsibilities, and future monitoring architecture are documented here:

👉 [View Architecture Documentation](docs/architecture.md)

---

## 🔎 Example Problem Detected

A query such as:

```sql
SELECT id, customer_id, product_name, amount, created_at
FROM demo_orders
WHERE customer_id = 42
LIMIT 20;
```

may produce a sequential-scan finding when no index exists on `customer_id`.

The profiler can recommend:

```sql
CREATE INDEX idx_demo_orders_customer_id
ON demo_orders(customer_id);
```

After manually creating the index, the query can be analyzed again and compared with the earlier result.

---

## 📊 Example Comparison

A test comparison produced:

```text
Before:       7.49 ms
After:        0.405 ms
Improvement:  94.59%
```

The sequential-scan finding disappeared after the index was created.

> **Note:** Actual execution times depend on database cache, hardware, PostgreSQL configuration, and system load.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| ☕ Java 17 | Application development |
| 🌱 Spring Boot | Backend framework |
| 🌐 Spring Web MVC | REST API |
| 🗄️ Spring Data JPA | Database persistence |
| ⚙️ Hibernate | ORM |
| 🐘 PostgreSQL | Database |
| 🐳 Docker Compose | Reproducible database environment |
| 📦 Maven | Build and dependency management |
| 🧪 JUnit | Automated testing |
| 🔄 GitHub Actions | Continuous integration |

---

## 📁 Project Structure

```text
sql-query-profiler/
│
├── src/
│   ├── main/
│   │   ├── java/com/sqlprofiler/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   ├── monitor/
│   │   │   ├── repository/
│   │   │   ├── rules/
│   │   │   ├── safety/
│   │   │   └── service/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/com/sqlprofiler/
│           └── rules/
│
├── docker/
│   └── initdb/
│       └── 01-demo-orders.sql
│
├── docs/
│   └── architecture.md
│
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## 🐳 Running the PostgreSQL Demo Database

Make sure Docker Desktop is running.

### Start PostgreSQL

```bash
docker compose up -d
```

### Check the container

```bash
docker compose ps
```

### Check the demo row count

```bash
docker compose exec postgres psql -U postgres -d sqlprofiler -c "SELECT COUNT(*) FROM demo_orders;"
```

The demo database contains approximately **50,000 rows**.

### Stop PostgreSQL without deleting its data

```bash
docker compose down
```

### Completely reset the demo database

```bash
docker compose down -v
docker compose up -d
```

> ⚠️ **Warning:** `docker compose down -v` deletes the Docker database volume, including demo data and query history.

---

## 🔐 Environment Configuration

Do **not** commit real passwords to GitHub.

Set the database password using an environment variable:

```text
DB_PASSWORD=your-local-password
```

The Spring configuration expects:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/sqlprofiler
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

The Docker database is exposed on host port **5433** to avoid conflicts with a PostgreSQL installation using port **5432**.

---

## ▶️ Starting the Application

### PowerShell

```powershell
$env:DB_PASSWORD="your-local-password"
mvnw.cmd spring-boot:run
```

### Command Prompt

```cmd
set DB_PASSWORD=your-local-password
mvnw.cmd spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

---

## 🔌 API Endpoints

### ❤️ Health Check

```http
GET /api/health
```

### 🔎 Analyze a Query

```http
POST /api/analyze
Content-Type: application/json
```

Request:

```json
{
  "query": "SELECT id, customer_id, product_name, amount, created_at FROM demo_orders WHERE customer_id = 42 LIMIT 20"
}
```

### 📜 View Query History

```http
GET /api/history
```

### 📊 Compare Before and After

```http
POST /api/compare
Content-Type: application/json
```

Request:

```json
{
  "beforeHistoryId": 1,
  "afterQuery": "SELECT id, customer_id, product_name, amount, created_at FROM demo_orders WHERE customer_id = 42 LIMIT 20"
}
```

> **Note:** `beforeHistoryId` must refer to a previous analysis saved in query history.

---

## 🧪 Running Tests

Run the complete test suite:

```cmd
mvnw.cmd clean test
```

The GitHub Actions CI pipeline also builds the project and runs the tests automatically after pushes and pull requests.

---

## 📋 Structured Logs

The application emits structured analysis events without logging the full SQL query.

Example:

```text
event=analysis_started query_length=105

event=explain_started query_length=105 timeout_seconds=10

event=analysis_completed status=ISSUES_FOUND findings_count=1 execution_time_ms=0.543 rows_scanned=20

event=history_saved history_id=4 status=ISSUES_FOUND findings_count=1
```

---

## 🛡️ Safety Notes

- User SQL is validated before execution.
- Analysis uses `EXPLAIN ANALYZE`.
- A statement timeout limits execution time.
- The profiler does not automatically apply recommended fixes in Stage 1A.
- Database changes must be reviewed and applied manually.
- Automatic fix application is planned for Stage 2.
- Real credentials must not be committed to the repository.

---

# 🗺️ Roadmap

## Stage 1A — Manual SQL Analysis ✅

- SQL query analysis
- Safe `EXPLAIN ANALYZE`
- Performance detection rules
- Severity and confidence
- Query history
- SQL normalization
- Suggested fixes
- Before/after comparison
- Docker PostgreSQL environment
- Automated tests
- GitHub Actions CI
- Structured logging

---

## Stage 1B — Automatic Monitoring 🔮

- Background polling with `@Scheduled`
- Active slow-query detection
- Lock-wait detection using `pg_locks`
- Connection saturation detection
- Query frequency detection
- Incident dashboard
- Threshold-based alerts
- Basic metrics
- Worker health checks

---

## Stage 2 — Production Grade 🚀

- `pg_stat_statements` trends
- Worker retries and reliability
- Alert deduplication
- Incident timelines
- Safe query-cancellation workflow
- Query regression detection
- CLI tool
- API keys and scopes
- Rate limiting
- Secure shareable reports
- Optional Chrome extension