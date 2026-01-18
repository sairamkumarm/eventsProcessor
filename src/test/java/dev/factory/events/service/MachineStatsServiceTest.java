package dev.factory.events.service;

import dev.factory.events.api.dto.MachineStatsResponse;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.repository.MachineStatsProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineStatsServiceTest {

    private EventRepository eventRepository;
    private MachineStatsService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        service = new MachineStatsService(eventRepository);
    }

    private MachineStatsProjection projection(long events, long defects) {
        MachineStatsProjection p = mock(MachineStatsProjection.class);
        when(p.getEventsCount()).thenReturn(events);
        when(p.getDefectsCount()).thenReturn(defects);
        return p;
    }


    @Test
    void normalStats_computesAverageCorrectly() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T10:00:00Z");

        MachineStatsProjection stats = projection(10, 5);

        when(eventRepository.fetchStats(eq("M1"), eq(start), eq(end)))
                .thenReturn(stats);

        MachineStatsResponse res =
                service.getStats("M1", start, end);

        assertEquals("M1", res.getMachineId());
        assertEquals(start, res.getStart());
        assertEquals(end, res.getEnd());
        assertEquals(10, res.getEventsCount());
        assertEquals(5, res.getDefectsCount());
        assertEquals(0.5, res.getAvgDefectRate());
        assertEquals("OK", res.getStatus());
    }

    @Test
    void zeroEvents_returnsZeroAverage_andOKStatus() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        MachineStatsProjection stats = projection(0, 0);

        when(eventRepository.fetchStats(eq("M2"), eq(start), eq(end)))
                .thenReturn(stats);

        MachineStatsResponse res =
                service.getStats("M2", start, end);

        assertEquals(0, res.getEventsCount());
        assertEquals(0, res.getDefectsCount());
        assertEquals(0.0, res.getAvgDefectRate());
        assertEquals("OK", res.getStatus());
    }

    @Test
    void eventsWithNoDefects_averageIsZero() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(5*60*60);

        MachineStatsProjection stats = projection(15, 0);

        when(eventRepository.fetchStats(eq("M3"), eq(start), eq(end)))
                .thenReturn(stats);

        MachineStatsResponse res =
                service.getStats("M3", start, end);

        assertEquals(15, res.getEventsCount());
        assertEquals(0, res.getDefectsCount());
        assertEquals(0.0, res.getAvgDefectRate());
        assertEquals("OK", res.getStatus());
    }


    @Test
    void DefectAverageOver2CausesWarning() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(10*60*60);


        MachineStatsProjection stats = projection(500_000, 52);

        when(eventRepository.fetchStats(eq("M4"), eq(start), eq(end)))
                .thenReturn(stats);

        MachineStatsResponse res =
                service.getStats("M4", start, end);

        assertEquals(500_000, res.getEventsCount());
        assertEquals(52, res.getDefectsCount());
        assertEquals(5.2, res.getAvgDefectRate());
        assertEquals("WARNING",res.getStatus());
    }
    @Test
    void repositoryIsCalledWithExactArguments() {
        Instant start = Instant.parse("2026-01-01T10:00:00Z");
        Instant end = Instant.parse("2026-01-01T11:00:00Z");


        MachineStatsProjection stats = projection(3, 1);

        when(eventRepository.fetchStats(eq("MX"), eq(start), eq(end)))
                .thenReturn(stats);

        service.getStats("MX", start, end);

        Mockito.verify(eventRepository)
                .fetchStats("MX", start, end);
    }
}
