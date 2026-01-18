package dev.factory.events.repository;

import dev.factory.events.AbstractIntegrationTest;
import dev.factory.events.domain.EventEntity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EventRepositoryStatsIT extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    private EventEntity event(
            String eventId,
            Instant eventTime,
            String machineId,
            int defectCount
    ) {
        return new EventEntity(
                eventId,
                eventTime,
                Instant.now(),
                machineId,
                "F01",
                "L01",
                1000,
                defectCount
        );
    }

    @Test
    void countsAllEvents_andSumsOnlyPositiveDefects() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T10:00:00Z");

        eventRepository.save(event("E1", start.plusSeconds(10), "M1", 5));
        eventRepository.save(event("E2", start.plusSeconds(20), "M1", -1));
        eventRepository.save(event("E3", start.plusSeconds(30), "M1", 0));

        MachineStatsProjection stats =
                eventRepository.fetchStats("M1", start, end);

        assertThat(stats.getEventsCount()).isEqualTo(3);
        assertThat(stats.getDefectsCount()).isEqualTo(5);
    }

    @Test
    void startTimeIsInclusive_endTimeIsExclusive() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start, "M2", 1));
        eventRepository.save(event("E2", end, "M2", 1));

        MachineStatsProjection stats =
                eventRepository.fetchStats("M2", start, end);

        assertThat(stats.getEventsCount()).isEqualTo(1);
        assertThat(stats.getDefectsCount()).isEqualTo(1);
    }

    @Test
    void emptyWindowReturnsZeroCounts() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        MachineStatsProjection stats =
                eventRepository.fetchStats("M3", start, end);

        assertThat(stats.getEventsCount()).isZero();
        assertThat(stats.getDefectsCount()).isZero();
    }

    @Test
    void eventsOutsideWindowAreIgnored() {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T12:00:00Z");

        eventRepository.save(event("E1",
                start.minusSeconds(1), "M4", 10));
        eventRepository.save(event("E2",
                end.plusSeconds(1), "M4", 10));

        MachineStatsProjection stats =
                eventRepository.fetchStats("M4", start, end);

        assertThat(stats.getEventsCount()).isZero();
        assertThat(stats.getDefectsCount()).isZero();
    }

    @Test
    void statsAreIsolatedPerMachine() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T02:00:00Z");

        eventRepository.save(event("E1",
                start.plusSeconds(10), "M5", 3));
        eventRepository.save(event("E2",
                start.plusSeconds(20), "M6", 99));

        MachineStatsProjection stats =
                eventRepository.fetchStats("M5", start, end);

        assertThat(stats.getEventsCount()).isEqualTo(1);
        assertThat(stats.getDefectsCount()).isEqualTo(3);
    }
}
