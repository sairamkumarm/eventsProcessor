package dev.factory.events.repository;

import dev.factory.events.AbstractIntegrationTest;
import dev.factory.events.domain.EventEntity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EventRepositoryTopDefectLinesIT extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    private EventEntity event(
            String eventId,
            Instant eventTime,
            String factoryId,
            String lineId,
            int defectCount
    ) {
        return new EventEntity(
                eventId,
                eventTime,
                Instant.now(),
                "M1",
                factoryId,
                lineId,
                1000,
                defectCount
        );
    }

    @Test
    void groupsByLine_andSumsDefects_andCountsEvents() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start.plusSeconds(10), "F01", "L1", 5));
        eventRepository.save(event("E2", start.plusSeconds(20), "F01", "L1", 3));
        eventRepository.save(event("E3", start.plusSeconds(30), "F01", "L2", 7));

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 10);

        assertThat(result).hasSize(2);

        TopDefectLineProjection first = result.get(0);
        TopDefectLineProjection second = result.get(1);

        assertThat(first.getLineId()).isEqualTo("L1");
        assertThat(first.getTotalDefects()).isEqualTo(8);
        assertThat(first.getEventCount()).isEqualTo(2);

        assertThat(second.getLineId()).isEqualTo("L2");
        assertThat(second.getTotalDefects()).isEqualTo(7);
        assertThat(second.getEventCount()).isEqualTo(1);
    }

    @Test
    void ordersByTotalDefectsDescending() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start.plusSeconds(10), "F01", "L1", 1));
        eventRepository.save(event("E2", start.plusSeconds(20), "F01", "L2", 10));
        eventRepository.save(event("E3", start.plusSeconds(30), "F01", "L3", 5));

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 10);

        assertThat(result)
                .extracting(TopDefectLineProjection::getLineId)
                .containsExactly("L2", "L3", "L1");
    }

    @Test
    void startTimeInclusive_endTimeExclusive() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start, "F01", "L1", 5));
        eventRepository.save(event("E2", end, "F01", "L1", 5));

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalDefects()).isEqualTo(5);
        assertThat(result.get(0).getEventCount()).isEqualTo(1);
    }

    @Test
    void factoryIsolationIsEnforced() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start.plusSeconds(10), "F01", "L1", 5));
        eventRepository.save(event("E2", start.plusSeconds(20), "F02", "L1", 99));

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalDefects()).isEqualTo(5);
    }

    @Test
    void limitIsAppliedAtDatabaseLevel() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        eventRepository.save(event("E1", start.plusSeconds(10), "F01", "L1", 1));
        eventRepository.save(event("E2", start.plusSeconds(20), "F01", "L2", 2));
        eventRepository.save(event("E3", start.plusSeconds(30), "F01", "L3", 3));

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 2);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(TopDefectLineProjection::getLineId)
                .containsExactly("L3", "L2");
    }

    @Test
    void emptyWindowReturnsEmptyList() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T01:00:00Z");

        List<TopDefectLineProjection> result =
                eventRepository.findTopDefectLinesNative("F01", start, end, 10);

        assertThat(result).isEmpty();
    }
}
