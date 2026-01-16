package dev.factory.events.service;

import dev.factory.events.AbstractIntegrationTest;
import dev.factory.events.api.dto.EventBatchRequest;
import dev.factory.events.api.dto.EventBatchResponse;
import dev.factory.events.api.dto.EventIngestRequest;
import dev.factory.events.domain.EventEntity;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.testConfig.FixedClockTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(FixedClockTestConfig.class)
public class DbBackedIngestionServiceIT extends AbstractIntegrationTest{
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
    @Test
    void shouldInsertNewEvent() {

        EventBatchRequest req = new EventBatchRequest();
        req.setEvents(List.of(baseEvent("E-1")));

        EventBatchResponse res = ingestionService.ingest(req);

        assertThat(res.getAccepted()).isEqualTo(1);
        assertThat(res.getUpdated()).isZero();
        assertThat(res.getDeduped()).isZero();
        assertThat(res.getRejected()).isZero();

        List<EventEntity> events =
                eventRepository.findAllByEventIdIn(List.of("E-1"));

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getMachineId()).isEqualTo("M-001");
    }

    @Test
    void identicalEventShouldBeDeduplicated() {

        EventIngestRequest e = baseEvent("E-2");

        EventBatchRequest req = new EventBatchRequest();
        req.setEvents(List.of(e));

        ingestionService.ingest(req);
        EventBatchResponse second = ingestionService.ingest(req);

        assertThat(second.getDeduped()).isEqualTo(1);
        assertThat(second.getAccepted()).isZero();

        List<EventEntity> events =
                eventRepository.findAllByEventIdIn(List.of("E-2"));

        assertThat(events).hasSize(1);
    }

    @Test
    void newerPayloadShouldUpdateExisting() {

        EventIngestRequest e1 = baseEvent("E-3");

        ingestionService.ingest(new EventBatchRequest(List.of(e1)));

        EventIngestRequest e2 = baseEvent("E-3");
        e2.setDefectCount(5);

        FixedClockTestConfig.advanceSeconds(10);
        EventBatchResponse res =
                ingestionService.ingest(new EventBatchRequest(List.of(e2)));

        assertThat(res.getUpdated()).isEqualTo(1);

        EventEntity stored =
                eventRepository.findAllByEventIdIn(List.of("E-3")).getFirst();

        assertThat(stored.getDefectCount()).isEqualTo(5);
    }
    @Test
    void invalidDurationShouldBeRejected() {

        EventIngestRequest e = baseEvent("E-4");
        e.setDurationMs(7 * 60 * 60 * 1000L);

        EventBatchRequest req = new EventBatchRequest();
        req.setEvents(List.of(e));

        EventBatchResponse res = ingestionService.ingest(req);

        assertThat(res.getRejected()).isEqualTo(1);
        assertThat(eventRepository.count()).isZero();
    }
    @Test
    void futureEventTimeShouldBeRejected() {

        EventIngestRequest e = baseEvent("E-5");
        e.setEventTime(Instant.parse("2026-01-15T10:30:00Z"));

        EventBatchRequest req = new EventBatchRequest();
        req.setEvents(List.of(e));

        EventBatchResponse res = ingestionService.ingest(req);

        assertThat(res.getRejected()).isEqualTo(1);
        assertThat(eventRepository.count()).isZero();
    }
    @Test
    void concurrentIngestionShouldNotCorruptState() throws Exception {

        EventIngestRequest e1 = baseEvent("E-6");
        e1.setDefectCount(1);

        EventIngestRequest e2 = baseEvent("E-6");
        e2.setDefectCount(2);

        Runnable r1 = () -> ingestionService.ingest(
                new EventBatchRequest(List.of(e1)));

        Runnable r2 = () -> ingestionService.ingest(
                new EventBatchRequest(List.of(e2)));

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        EventEntity stored =
                eventRepository.findAllByEventIdIn(List.of("E-6")).getFirst();

        assertThat(stored.getDefectCount()).isIn(1, 2);
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @BeforeEach
    void clean(){
        eventRepository.deleteAll();
    }

}
