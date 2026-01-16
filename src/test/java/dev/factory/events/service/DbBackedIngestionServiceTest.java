package dev.factory.events.service;

import dev.factory.events.api.dto.*;
import dev.factory.events.domain.EventEntity;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.util.ClockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;

class DbBackedIngestionServiceTest {
    private EventRepository eventRepository;
    private ClockProvider clockProvider;
    private DbBackedIngestionService service;

    private Instant fixedNow;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        clockProvider = mock(ClockProvider.class);
        service = new DbBackedIngestionService(eventRepository, clockProvider);

        fixedNow = Instant.parse("2026-01-15T10:00:00Z");
        when(clockProvider.now()).thenReturn(fixedNow);
    }

    private EventIngestRequest baseEvent(String eventId) {
        EventIngestRequest e = new EventIngestRequest();
        e.setEventId(eventId);
        e.setEventTime(fixedNow.minusSeconds(60));
        e.setMachineId("M1");
        e.setFactoryId("F1");
        e.setLineId("L1");
        e.setDurationMs(1000);
        e.setDefectCount(0);
        return e;
    }

    @Test
    void identicalDuplicateEventIsDeduped() {

        EventIngestRequest e1 = baseEvent("E1");
        EventIngestRequest e2 = baseEvent("E1");

        EventBatchRequest request = new EventBatchRequest();
        request.setEvents(List.of(e1, e2));

        EventEntity existing = new EventEntity(
                "E1",
                e1.getEventTime(),
                fixedNow.minusSeconds(10),
                "M1",
                "F1",
                "L1",
                1000,
                0
        );

        when(eventRepository.findAllByEventIdInForUpdate(List.of("E1")))
                .thenReturn(List.of(existing));

        EventBatchResponse response = service.ingest(request);

        assertThat(response.getDeduped()).isEqualTo(1);
        assertThat(response.getAccepted()).isZero();
        assertThat(response.getUpdated()).isZero();

        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    void newerPayloadUpdatesExistingEvent() {

        EventIngestRequest e = baseEvent("E1");
        e.setDefectCount(2);

        EventBatchRequest request = new EventBatchRequest();
        request.setEvents(List.of(e));

        EventEntity existing = new EventEntity(
                "E1",
                e.getEventTime(),
                fixedNow.minusSeconds(100),
                "M1",
                "F1",
                "L1",
                1000,
                0
        );

        when(eventRepository.findAllByEventIdInForUpdate(List.of("E1")))
                .thenReturn(List.of(existing));

        EventBatchResponse response = service.ingest(request);

        assertThat(response.getUpdated()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventEntity>> captor =
                (ArgumentCaptor<List<EventEntity>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

        verify(eventRepository).saveAll(captor.capture());

        EventEntity updated = captor.getValue().get(0);
        assertThat(updated.getDefectCount()).isEqualTo(2);
    }

    @Test
    void olderPayloadIsIgnored() {

        when(clockProvider.now()).thenReturn(fixedNow.minusSeconds(100));

        EventIngestRequest e = baseEvent("E1");
        e.setDefectCount(5);

        EventBatchRequest request = new EventBatchRequest();
        request.setEvents(List.of(e));

        EventEntity existing = new EventEntity(
                "E1",
                e.getEventTime(),
                fixedNow,
                "M1",
                "F1",
                "L1",
                1000,
                0
        );

        when(eventRepository.findAllByEventIdInForUpdate(List.of("E1")))
                .thenReturn(List.of(existing));

        EventBatchResponse response = service.ingest(request);

        assertThat(response.getUpdated()).isZero();
        assertThat(response.getDeduped()).isZero();
        assertThat(response.getAccepted()).isZero();

        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    void durationAboveLimitIsRejected() {

        EventIngestRequest e = baseEvent("E1");
        e.setDurationMs(7 * 60 * 60 * 1000L);

        EventBatchRequest request = new EventBatchRequest();
        request.setEvents(List.of(e));

        EventBatchResponse response = service.ingest(request);

        assertThat(response.getRejected()).isEqualTo(1);
        assertThat(response.getRejections().get(0).getReason())
                .isEqualTo(RejectionReason.INVALID_DURATION);

        verifyNoInteractions(eventRepository);
    }

    @Test
    void futureEventTimeIsRejected() {

        EventIngestRequest e = baseEvent("E1");
        e.setEventTime(fixedNow.plusSeconds(20 * 60));

        EventBatchRequest request = new EventBatchRequest();
        request.setEvents(List.of(e));

        EventBatchResponse response = service.ingest(request);

        assertThat(response.getRejected()).isEqualTo(1);
        assertThat(response.getRejections().get(0).getReason())
                .isEqualTo(RejectionReason.EVENT_TIME_TOO_FAR_IN_FUTURE);

        verifyNoInteractions(eventRepository);
    }

}