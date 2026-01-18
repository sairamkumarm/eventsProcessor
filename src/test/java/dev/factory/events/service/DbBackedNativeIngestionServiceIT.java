package dev.factory.events.service;

import dev.factory.events.AbstractIntegrationTest;
import dev.factory.events.api.dto.*;
import dev.factory.events.domain.EventEntity;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.testConfig.FixedClockTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(FixedClockTestConfig.class)
class DbBackedNativeIngestionServiceIT extends AbstractIntegrationTest {
    @Autowired
    private EventIngestionService ingestionService;

    @Autowired
    private EventRepository eventRepository;

    private EventIngestRequest baseEvent(String id) {
        EventIngestRequest e = new EventIngestRequest();
        e.setEventId(id);
        e.setEventTime(Instant.parse("2026-01-15T09:59:00Z"));
        e.setMachineId("M-001");
        e.setFactoryId("F01");
        e.setLineId("L-01");
        e.setDurationMs(1000);
        e.setDefectCount(0);
        return e;
    }

    @BeforeEach
    void clean() {
        eventRepository.deleteAll();
        FixedClockTestConfig.reset();
    }

    @Test
    void shouldInsertNewEvent() {

        EventBatchResponse res = ingestionService.ingest(
                new EventBatchRequest(List.of(baseEvent("E-1")))
        );

        assertThat(res.getAccepted()).isEqualTo(1);
        assertThat(res.getUpdated()).isZero();
        assertThat(res.getDeduped()).isZero();
        assertThat(res.getRejected()).isZero();

        List<EventEntity> stored =
                eventRepository.findAllByEventIdIn(List.of("E-1"));

        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().getMachineId()).isEqualTo("M-001");
    }

    @Test
    void identicalEventShouldBeDeduplicated() {

        EventIngestRequest e = baseEvent("E-2");

        ingestionService.ingest(new EventBatchRequest(List.of(e)));

        EventBatchResponse second =
                ingestionService.ingest(new EventBatchRequest(List.of(e)));

        assertThat(second.getAccepted()).isZero();
        assertThat(second.getUpdated()).isZero();
        assertThat(second.getDeduped()).isEqualTo(1);

        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    void newerPayloadShouldUpdateExisting() {

        ingestionService.ingest(
                new EventBatchRequest(List.of(baseEvent("E-3")))
        );

        FixedClockTestConfig.advanceSeconds(10);

        EventIngestRequest updated = baseEvent("E-3");
        updated.setDefectCount(5);

        EventBatchResponse res =
                ingestionService.ingest(new EventBatchRequest(List.of(updated)));

        assertThat(res.getUpdated()).isEqualTo(1);
        assertThat(res.getAccepted()).isZero();
        assertThat(res.getDeduped()).isZero();

        EventEntity stored =
                eventRepository.findAllByEventIdIn(List.of("E-3")).getFirst();

        assertThat(stored.getDefectCount()).isEqualTo(5);
    }

    @Test
    void invalidDurationShouldBeRejected() {

        EventIngestRequest e = baseEvent("E-4");
        e.setDurationMs(7 * 60 * 60 * 1000L);

        EventBatchResponse res =
                ingestionService.ingest(new EventBatchRequest(List.of(e)));

        assertThat(res.getRejected()).isEqualTo(1);
        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void futureEventTimeShouldBeRejected() {
        System.out.println(ingestionService.getClass());

        EventIngestRequest e = baseEvent("E-5");
        e.setEventTime(Instant.parse("2036-01-15T10:30:00Z"));

        EventBatchResponse res =
                ingestionService.ingest(new EventBatchRequest(List.of(e)));
        System.out.println(FixedClockTestConfig.NOW);
        assertThat(res.getRejected()).isEqualTo(1);
        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void concurrentIngestionShouldNotCorruptState() throws Exception {

        EventIngestRequest e1 = baseEvent("E-6");
        e1.setDefectCount(1);

        EventIngestRequest e2 = baseEvent("E-6");
        e2.setDefectCount(2);

        Thread t1 = new Thread(() ->
                ingestionService.ingest(new EventBatchRequest(List.of(e1)))
        );

        Thread t2 = new Thread(() ->
                ingestionService.ingest(new EventBatchRequest(List.of(e2)))
        );

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        List<EventEntity> stored =
                eventRepository.findAllByEventIdIn(List.of("E-6"));

        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().getDefectCount()).isIn(1, 2);
    }
}
