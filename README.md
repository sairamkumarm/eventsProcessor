# Factory Events Backend

## Overview

This Spring Boot microservice ingests events from factory machines, validates, deduplicates, and persists them in PostgreSQL. It provides machine- and line-level defect statistics over specified time windows. Designed for high concurrency, the system applies Flyway-managed migrations automatically and supports batch ingestion, metrics aggregation, and error handling.

Key highlights:

* Batch ingestion of events with automatic deduplication and updates on newer payloads
* Validation rules: duration limits, event timestamp constraints, and schema correctness
* Metrics aggregation per machine and per line, including top-defect lines per factory
* Thread-safe, high-concurrency capable via Spring Boot thread pool
* PostgreSQL 15 database with Flyway-managed migrations
* Configured for Asia/Kolkata timezone

---

## Architecture

### Components

1. **Event Service**
   Spring Boot REST API for ingestion and stats queries. Thread-safe, handles multiple concurrent requests, and performs deduplication, validation, and metrics aggregation.

2. **Database (PostgreSQL 15)**
   Persists all event data. Supports atomic upserts via `ON CONFLICT`. Temporary tables and JDBC batch inserts used for high-performance batch ingestion. Flyway migrations applied automatically at startup.

### Data Flow

```
[Machines] → [Spring Boot Ingestion API] → [PostgreSQL (Events Table)]
                                      ↘ [Stats Queries API]
```

---

## Technology Stack

* **Java 21**
* **Spring Boot**
* **PostgreSQL 15**
* **Flyway** (DB migrations in `src/main/resources/db/migration`)
* **Testcontainers** (integration testing)
* **Docker + Docker Compose**

---

## Setup & Run

1. Clone the repository
2. Ensure Docker and Docker Compose are installed

```bash
docker-compose up --build
```

* Spins up PostgreSQL 15 container (`events-postgres`)
* Builds the application if JAR not present
* Applies Flyway migrations automatically
* Exposes service on port 8080
* Configured timezone via `TZ=Asia/Kolkata`

**Environment Variables:**

| Name                         | Description                     |
| ---------------------------- | ------------------------------- |
| `SPRING_DATASOURCE_URL`      | JDBC URL for PostgreSQL         |
| `SPRING_DATASOURCE_USERNAME` | DB username                     |
| `SPRING_DATASOURCE_PASSWORD` | DB password                     |
| `TZ`                         | Timezone (default Asia/Kolkata) |

OpenAPI spec available at:
`http://localhost:8080/v3/api-docs`

Sample jsons of size 1000 and 5000 are included

---

## Endpoints

### 1. Batch Event Ingestion

**POST /events/batch**

**Request Body Example:**

```json
{
  "events": [
    {
      "eventId": "E-1",
      "eventTime": "2026-01-15T10:12:03.123Z",
      "receivedTime": "2026-01-15T10:12:04.500Z",
      "machineId": "M-001",
      "factoryId": "F-01",
      "lineId": "L-01",
      "durationMs": 1200,
      "defectCount": 0
    }
  ]
}
```

**Response Example:**

```json
{
  "accepted": 998,
  "deduped": 0,
  "updated": 0,
  "rejected": 2,
  "rejections": [
    {
      "eventId": "E-1",
      "reason": "EVENT_TIME_TOO_FAR_IN_FUTURE"
    },
    {
      "eventId": "E-2",
      "reason": "EVENT_TIME_TOO_FAR_IN_FUTURE"
    }
  ]
}
```

**Objects:**

* `EventBatchRequest`: array of `EventIngestRequest` (minimum 1)
* `EventIngestRequest`: `eventId`, `eventTime`, `receivedTime`, `machineId`, `factoryId`, `lineId`, `durationMs`, `defectCount`
* `EventBatchResponse`: summary of accepted, deduped, updated, rejected counts + optional rejections
* `EventRejection`: `eventId` + reason (`INVALID_DURATION`, `EVENT_TIME_TOO_FAR_IN_FUTURE`, `MALFORMED_REQUEST`)

**Validation & Deduplication Rules:**

* Deduplication based on `eventId`

    * Identical payload → counted as deduped
    * Newer payload → updated
    * Older payload ignored
* `defectCount = -1` stored but excluded from metrics
* `durationMs` outside 0–6 hours → rejected
* `eventTime > 15 min in future` → rejected
* Malformed input → structured `VALIDATION_ERROR` response

**Error Response Example:**

```json
{
  "error": "VALIDATION_ERROR",
  "details": [
    "events[0].eventId: EventId must be in format E-<digits>",
    "events[0].lineId: LineId must be in format L-<digits>",
    "events[0].factoryId: FactoryId must be in format F-<digits>"
  ]
}
```

**Performance Strategy:**

* Temporary table for batch ingestion
* JDBC batch insert + CTE for classification
* Ingestion of 1000 events reduced from 1860ms → ~300ms
* Computation offloaded to DB for efficiency

---

### 2. Machine Stats

**GET /events/stats?machineId=M-001&start=...&end=...**

**Response Example:**

```json
{
  "machineId": "M-001",
  "start": "2026-01-15T00:00:00Z",
  "end": "2026-01-15T06:00:00Z",
  "eventsCount": 1200,
  "defectsCount": 6,
  "avgDefectRate": 2.1,
  "status": "Warning"
}
```

**Object:** `MachineStatsResponse` – includes `machineId`, start/end time, `eventsCount`, `defectsCount`, average defect rate, status (`HEALTHY` / `WARNING`)

**Notes:** Start time inclusive, end time exclusive.

---

### 3. Top Defect Lines

**GET /events/stats/top-defect-lines?factoryId=F01&from=...&to=...&limit=10**

**Response Example:**

```json
{
  "defectLines": [
    {
      "lineId": "L-01",
      "totalDefects": 15,
      "eventCount": 500,
      "defectsPercent": 30
    },
    {
      "lineId": "L-02",
      "totalDefects": 20,
      "eventCount": 400,
      "defectsPercent": 5
    }
  ]
}
```
(NOTE: actual formula for defectsPercent scales the totalDefects to 100, as needed)

**Objects:**

* `DefectLineResponse`: `lineId`, `totalDefects`, `eventCount`, `defectsPercent`
* `TopDefectLinesResponse`: array of `DefectLineResponse`


### 4. Error Handling

Invalid inputs are rejected with a structured response:
```json
{
  "error": "VALIDATION_ERROR",
  "details": [
      "events[0].eventId: EventId must be in format E-<digits>",
      "events[0].lineId: LineId must be in format L-<digits>",
      "events[0].factoryId: FactoryId must be in format F-<digits>"
  ]
}
```

Dedupe / Update Logic
---

## Data Model

**`events` Table Columns:**

```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_time TIMESTAMPTZ NOT NULL,
    received_time TIMESTAMPTZ NOT NULL,
    machine_id VARCHAR(64) NOT NULL,
    factory_id VARCHAR(64) NOT NULL,
    line_id VARCHAR(64) NOT NULL,
    duration_ms BIGINT NOT NULL,
    defect_count INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_machine_time ON events(machine_id, event_time);
CREATE INDEX idx_events_factory_line_time ON events(factory_id, line_id, event_time);

```

Atomic upserts via `ON CONFLICT` for deduplication / updates.

---

## Thread Safety & Concurrency

* Spring Boot thread pool handles multiple concurrent requests
* Database enforces atomic upserts
* High concurrency handled at DB and application layer

---

## Edge Cases & Assumptions
* Defect count is not limited to -1, 0 or 1, one event can bring multiple defects with it.
* `durationMs` outside 0–6 hours → rejected
* `eventTime > 15 min in future` → rejected
* Start inclusive, end exclusive for stats queries

**Optional Improvements:**

* Sharded DB / partitioning for very high-volume factories
* Monitoring metrics & alerting on ingestion latency
* Optional caching for frequent stats queries

