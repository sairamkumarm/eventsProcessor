package dev.factory.events.service;

import dev.factory.events.api.dto.DefectLineResponse;
import dev.factory.events.api.dto.TopDefectLinesResponse;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.repository.TopDefectLineProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineStatsServiceTest {

    @Mock
    private EventRepository eventRepository;

    private LineStatsService service;

    @BeforeEach
    void setUp() {
        service = new LineStatsService(eventRepository);
    }

    @Test
    void returnsMappedResultsWithCorrectPercentage() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");

        TopDefectLineProjection p1 = projection("L1", 50, 200);
        TopDefectLineProjection p2 = projection("L2", 20, 100);

        when(eventRepository.findTopDefectLinesNative("F01", from, to, 10))
                .thenReturn(List.of(p1, p2));

        TopDefectLinesResponse result = service.getTopDefectLines("F01", from, to, 10);

        assertEquals(2, result.getDefectLines().size());

        DefectLineResponse r1 = result.getDefectLines().get(0);
        assertEquals("L1", r1.getLineId());
        assertEquals(50, r1.getTotalDefects());
        assertEquals(200, r1.getEventCount());
        assertEquals(25.0, r1.getDefectsPercent());

        DefectLineResponse r2 = result.getDefectLines().get(1);
        assertEquals("L2", r2.getLineId());
        assertEquals(20, r2.getTotalDefects());
        assertEquals(100, r2.getEventCount());
        assertEquals(20.0, r2.getDefectsPercent());

        verify(eventRepository)
                .findTopDefectLinesNative("F01", from, to, 10);
    }

    @Test
    void zeroEventsYieldsZeroPercent() {
        Instant from = Instant.now();
        Instant to = from.plusSeconds(3600);

        TopDefectLineProjection p = projection("L0", 0, 0);

        when(eventRepository.findTopDefectLinesNative("F01", from, to, 5))
                .thenReturn(List.of(p));

        TopDefectLinesResponse result =
                service.getTopDefectLines("F01", from, to, 5);

        assertEquals(1, result.getDefectLines().size());

        DefectLineResponse r = result.getDefectLines().get(0);
        assertEquals("L0", r.getLineId());
        assertEquals(0, r.getTotalDefects());
        assertEquals(0, r.getEventCount());
        assertEquals(0.0, r.getDefectsPercent());
    }

    @Test
    void roundingIsToTwoDecimals() {
        Instant from = Instant.now();
        Instant to = from.plusSeconds(3600);

        // 1 defect over 3 events = 33.333...%
        TopDefectLineProjection p = projection("L3", 1, 3);

        when(eventRepository.findTopDefectLinesNative("F01", from, to, 1))
                .thenReturn(List.of(p));

        TopDefectLinesResponse result =
                service.getTopDefectLines("F01", from, to, 1);

        assertEquals(33.33, result.getDefectLines().get(0).getDefectsPercent());
    }

    private TopDefectLineProjection projection(
            String lineId,
            long totalDefects,
            long eventCount
    ) {
        return new TopDefectLineProjection() {
            @Override public String getLineId() { return lineId; }
            @Override public Long getTotalDefects() { return totalDefects; }
            @Override public Long getEventCount() { return eventCount; }
        };
    }
}
