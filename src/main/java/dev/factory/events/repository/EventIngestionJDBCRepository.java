package dev.factory.events.repository;

import dev.factory.events.domain.IngestCounts;
import dev.factory.events.service.model.NormalizedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class EventIngestionJDBCRepository {

    private final JdbcTemplate jdbc;

    public EventIngestionJDBCRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public IngestCounts ingestBatch(List<NormalizedEvent> events) {

        // 1. temp table
        jdbc.execute("""
            CREATE TEMP TABLE tmp_events (
                event_id TEXT,
                event_time TIMESTAMPTZ,
                received_time TIMESTAMPTZ,
                machine_id TEXT,
                factory_id TEXT,
                line_id TEXT,
                duration_ms BIGINT,
                defect_count INT
            ) ON COMMIT DROP
        """);

        // 2. batch insert into temp table
        jdbc.batchUpdate("""
            INSERT INTO tmp_events (
                event_id, event_time, received_time,
                machine_id, factory_id, line_id,
                duration_ms, defect_count
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, events, 1000, (ps, e) -> {
            ps.setString(1, e.getEventId());
            ps.setTimestamp(2, Timestamp.from(e.getEventTime()));
            ps.setTimestamp(3, Timestamp.from(e.getReceivedTime()));
            ps.setString(4, e.getMachineId());
            ps.setString(5, e.getFactoryId());
            ps.setString(6, e.getLineId());
            ps.setLong(7, e.getDurationMs());
            ps.setInt(8, e.getDefectCount());
        });

        // 3. classify + upsert + count
        return jdbc.queryForObject("""
            WITH classified AS (
                SELECT
                    t.*,
                    e.event_id IS NULL AS is_new,
                    e.received_time < t.received_time AS is_newer,
                    (
                        e.event_time = t.event_time AND
                        e.machine_id = t.machine_id AND
                        e.factory_id = t.factory_id AND
                        e.line_id = t.line_id AND
                        e.duration_ms = t.duration_ms AND
                        e.defect_count = t.defect_count
                    ) AS is_identical
                FROM tmp_events t
                LEFT JOIN events e ON e.event_id = t.event_id
            ),
            upserted AS (
                INSERT INTO events (
                    event_id, event_time, received_time,
                    machine_id, factory_id, line_id,
                    duration_ms, defect_count
                )
                SELECT
                    event_id, event_time, received_time,
                    machine_id, factory_id, line_id,
                    duration_ms, defect_count
                FROM classified
                WHERE is_new OR (is_newer AND NOT is_identical)
                ON CONFLICT (event_id) DO UPDATE
                    SET
                        event_time = EXCLUDED.event_time,
                        received_time = EXCLUDED.received_time,
                        machine_id = EXCLUDED.machine_id,
                        factory_id = EXCLUDED.factory_id,
                        line_id = EXCLUDED.line_id,
                        duration_ms = EXCLUDED.duration_ms,
                        defect_count = EXCLUDED.defect_count
                    WHERE events.received_time < EXCLUDED.received_time
                RETURNING event_id
            )
            SELECT
                count(*) FILTER (WHERE is_new) AS accepted,
                count(*) FILTER (
                    WHERE NOT is_new AND is_newer AND NOT is_identical
                ) AS updated,
                count(*) FILTER (
                    WHERE NOT is_new AND is_identical
                ) AS deduped
            FROM classified
        """, (rs, rowNum) ->
                new IngestCounts(
                        rs.getInt("accepted"),
                        rs.getInt("updated"),
                        rs.getInt("deduped")
                )
        );
    }
}
