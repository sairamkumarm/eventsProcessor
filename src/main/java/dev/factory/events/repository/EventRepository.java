package dev.factory.events.repository;

import dev.factory.events.api.dto.MachineStatsResponse;
import dev.factory.events.domain.EventEntity;
import dev.factory.events.service.model.NormalizedEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT e
    FROM EventEntity e
    WHERE e.eventId IN :eventIds
    """)
    List<EventEntity> findAllByEventIdInForUpdate(@Param("eventIds") List<String> eventIds);


    @Modifying
    @Query(
            value = """
        INSERT INTO events(event_id, event_time, received_time, machine_id, factory_id, line_id, duration_ms, defect_count)
        VALUES (:#{#e.eventId}, :#{#e.eventTime}, :#{#e.receivedTime}, :#{#e.machineId}, :#{#e.factoryId}, :#{#e.lineId}, :#{#e.durationMs}, :#{#e.defectCount})
        ON CONFLICT(event_id) DO UPDATE
        SET
            event_time = EXCLUDED.event_time,
            received_time = EXCLUDED.received_time,
            machine_id = EXCLUDED.machine_id,
            factory_id = EXCLUDED.factory_id,
            line_id = EXCLUDED.line_id,
            duration_ms = EXCLUDED.duration_ms,
            defect_count = EXCLUDED.defect_count
        WHERE EXCLUDED.received_time > events.received_time
        """,
            nativeQuery = true
    )
    int upsertBatch(@Param("e") List<NormalizedEvent> events);

    //FOR TESTS ONLY
    @Query("""
    SELECT e
    FROM EventEntity e
    WHERE e.eventId IN :eventIds
    """)
    List<EventEntity> findAllByEventIdIn(@Param("eventIds") List<String> eventIds);

    @Query("""
    SELECT
        COUNT(e) as eventsCount,
        COALESCE(
            SUM(
                CASE
                    WHEN e.defectCount >= 0 THEN e.defectCount
                    ELSE 0
                END
            ),
            0
        ) as defectsCount
    FROM EventEntity e
    WHERE e.machineId = :machineId
      AND e.eventTime >= :startTime
      AND e.eventTime < :endTime
    """)
    MachineStatsProjection fetchStats(
            @Param("machineId") String machineId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query(value = """
    SELECT
        e.line_id as lineId,
        COALESCE(SUM(CASE WHEN e.defect_count >= 0 THEN e.defect_count ELSE 0 END), 0) as totalDefects,
        COUNT(*) as eventCount
    FROM events e
    WHERE e.factory_id = :factoryId
      AND e.event_time >= :startTime
      AND e.event_time < :endTime
    GROUP BY e.line_id
    ORDER BY totalDefects DESC
    LIMIT :limit
""", nativeQuery = true)
    List<TopDefectLineProjection> findTopDefectLinesNative(
            @Param("factoryId") String factoryId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("limit") int limit
    );


}
