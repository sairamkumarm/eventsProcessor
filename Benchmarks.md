## Ingestion Performance Benchmark

### Test Environment

* **CPU:** Intel i5 10th Gen
* **RAM:** 16 GB
* **OS:** Windows 11 25H2
* **Database:** PostgreSQL 15 (Docker)
* **JDK:** Java 21
* **Framework:** Spring Boot
* **Execution Mode:** Local, Docker Compose

### Benchmark Setup

* Single batch ingestion request
* Events are valid, non-duplicated unless stated
* Database is warm (tables created, Flyway migrations already applied)
* No artificial delays or throttling
* Measurements taken across multiple runs and averaged

### Measurement approach
* Ingestion performance was measured inside the application by timing the ingestion pipeline execution (validation → deduplication/update → persistence → commit).

* Network and HTTP transport latency were intentionally excluded to focus on backend execution cost, which is the primary scalability bottleneck for this system.

* Timing was logged at the service layer and averaged across multiple runs.

### Results

| Ingestion Strategy                         | 1,000 Events | 5,000 Events |
| ------------------------------------------ | ------------ | ------------ |
| JPA + `SELECT ... FOR UPDATE`              | ~1860 ms     | ~6–8 seconds |
| JDBC + Temp Table + DB-side Classification | ~160–300 ms  | ~800 ms      |

### Observations

* The **JPA + row-level locking** approach scales poorly due to:

    * Per-row locking
    * Entity hydration overhead
    * Increased contention under concurrency

* The **JDBC + temporary table approach** significantly improves performance by:

    * Eliminating row-level locks during comparison
    * Performing deduplication and update classification entirely inside the database
    * Using a single `INSERT … ON CONFLICT` upsert path
    * Minimizing round-trips and ORM overhead

* PostgreSQL effectively acts as the concurrency coordinator and persistence layer, which it is optimized for.

### Conclusion

The final ingestion pipeline comfortably meets the requirement of processing **1,000 events under 1 second** on a standard laptop, while also scaling predictably to larger batch sizes. The JDBC-based ingestion path is therefore used as the primary implementation, with the JPA-based approach retained only as a reference and fallback.

---
