# SQL Query Profiler — User Guide

## 1. Introduction
The SQL Query Profiler is a tool designed to help developers identify the reasons behind slow PostgreSQL queries. After you submit a SQL query, the application analyzes its execution plan and highlights any potential issues. It is particularly beneficial for backend developers tasked with diagnosing database performance issues. The tool offers suggestions for improvement but does not automatically modify the database.

## 2. Before You Begin

## 3. Starting the Application

Before starting, make sure you have:

- Java and Maven installed.
- PostgreSQL running locally or through Docker.
- Postman installed for sending API requests.
- The SQL Query Profiler project was downloaded and opened in an IDE.

If you are using the Docker-based demo database, ensure that Docker is running. If you are running your own PostgreSQL installation, Docker is not necessary.

Before starting the application, ensure you have the database username, password, and port number ready.

## 3. Starting the Application

### Step 1: Start PostgreSQL

If you are using Docker, open a terminal in the project folder and run:

```bash
docker compose up -d
```

Check if the database is running or not:

```bash
docker compose ps
```

If you are using a local PostgreSQL installation, start PostgreSQL from your system.

### Step 2: Set the database password

Open this file:

```text
src/main/resources/application.properties
```

Update the password to match your PostgreSQL password:

```properties
spring.datasource.password=YOUR_PASSWORD
```

Do not share your real password with anyone or any AI tool.

### Step 3: Start the Spring Boot application

In the project folder, run:

```bash
mvn spring-boot:run
```

Wait until the terminal shows that the application has started on port `8080`.

### Step 4: Check that it is running

In Postman, send this request:

```http
GET http://localhost:8080/api/history
```

If you receive a JSON response, the application is running. If you do not receive a response, the application may not be running properly.

## 4. Analyzing a SQL Query

To analyze a query, send a `POST` request to:

```http
http://localhost:8080/api/analyze
```

In Postman, select **Body → raw → JSON** and enter:

```json
{
  "query": "SELECT id, customer_id, amount, created_at FROM demo_orders WHERE customer_id = 42 LIMIT 20"
}
```

Click **Send**.

The application will analyze the query and return a report. The report shows the query result, execution details, findings, and suggested fixes.

To see previous queries, send:

```http
GET http://localhost:8080/api/history
```

## 5. Understanding the Results

A **finding** is a possible issue found in the query. For example, the tool may detect that the query is using a sequential scan.

**Severity** tells you how important the issue may be. A `CRITICAL` finding should be checked first.

**Confidence** tells you how sure the tool is about the finding. A `HIGH` confidence finding has stronger supporting evidence.

A **suggested fix** is a possible improvement. For example:

```sql
CREATE INDEX idx_demo_orders_customer_id
ON demo_orders(customer_id);
```

The profiler does not automatically apply fixes. You should read the suggestion, check whether it makes sense for your database, and apply it manually if needed.

## 6. Comparing Query Performance

You can compare a query before and after making a change.

### Step 1: Analyze the original query

Send this request:

```http
POST http://localhost:8080/api/analyze
```

Use the query you want to test.

### Step 2: Find the history ID

Send:

```http
GET http://localhost:8080/api/history
```

Find the analysis you just created and note its `id`. For example:

```json
{
  "id": 3
}
```

### Step 3: Apply the suggested change

Open your database client and run the suggested SQL manually. For example:

```sql
CREATE INDEX idx_demo_orders_customer_id
ON demo_orders(customer_id);
```

### Step 4: Compare the results

Send this request:

```http
POST http://localhost:8080/api/compare
```

Use your actual history ID in the request:

```json
{
  "beforeHistoryId": 3,
  "afterQuery": "SELECT id, customer_id, amount, created_at FROM demo_orders WHERE customer_id = 42 LIMIT 20"
}
```

The response will show the old execution time, the new execution time, and whether performance improved.

## 7. Troubleshooting

### `mvn` is not recognized

This means Maven is not installed or is not added to your system PATH.

Check whether the project includes `mvnw.cmd`. If it does, run:

```powershell
.\mvnw.cmd spring-boot:run
```

If not, install Maven and reopen the terminal.

### The application does not start

Check that PostgreSQL is running. Also check that the username, password, port, and database name in `application.properties` are correct.

### The comparison result is not better

That can happen. A suggested fix may not improve performance for every query or dataset. Review the execution plan and test with realistic data before deciding whether to use the change.

## 8. Safety Considerations

- Use a demo database while testing.
- Review suggested SQL before running it.
- Do not run unknown queries on a production database.
- Keep database passwords private.
- Do not share database passwords with anyone or any AI tool.
- Do not commit real passwords to GitHub.
