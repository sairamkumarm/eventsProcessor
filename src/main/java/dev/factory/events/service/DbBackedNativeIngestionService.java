package dev.factory.events.service;

import dev.factory.events.api.dto.*;
import dev.factory.events.domain.IngestCounts;
import dev.factory.events.repository.EventIngestionJDBCRepository;
import dev.factory.events.service.model.NormalizedEvent;
import dev.factory.events.util.ClockProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Service("nativeIngestion")
public class DbBackedNativeIngestionService implements EventIngestionService {

    private static final Duration MAX_DURATION = Duration.ofHours(6);
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(15);

    private final EventIngestionJDBCRepository repo;
    private final ClockProvider clock;

    public DbBackedNativeIngestionService(EventIngestionJDBCRepository repo, @Qualifier("runningClock") ClockProvider clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    public EventBatchResponse ingest(EventBatchRequest request) {

        Instant now = clock.now();
//        System.out.println(now);
        List<EventRejection> rejections = new ArrayList<>();
        Map<String, NormalizedEvent> dedupedInRequest = new HashMap<>();

        for (EventIngestRequest raw : request.getEvents()) {

            if (raw.getDurationMs() < 0 || raw.getDurationMs() > MAX_DURATION.toMillis()) {
                rejections.add(new EventRejection(
                        raw.getEventId(),
                        RejectionReason.INVALID_DURATION
                ));
                continue;
            }

            if (raw.getEventTime().isAfter(now.plus(FUTURE_TOLERANCE))) {
                rejections.add(new EventRejection(
                        raw.getEventId(),
                        RejectionReason.EVENT_TIME_TOO_FAR_IN_FUTURE
                ));
                continue;
            }

            NormalizedEvent ne = new NormalizedEvent(
                    raw.getEventId(),
                    raw.getEventTime(),
                    now,
                    raw.getMachineId(),
                    raw.getFactoryId(),
                    raw.getLineId(),
                    raw.getDurationMs(),
                    raw.getDefectCount()
            );

            NormalizedEvent existing = dedupedInRequest.get(ne.getEventId());
            if (existing == null || ne.getReceivedTime().isAfter(existing.getReceivedTime())) {
                dedupedInRequest.put(ne.getEventId(), ne);
            }
        }

        if (dedupedInRequest.isEmpty()) {
            return new EventBatchResponse(
                    0, 0, 0,
                    rejections.size(),
                    rejections
            );
        }

        IngestCounts counts =
                repo.ingestBatch(new ArrayList<>(dedupedInRequest.values()));

        return new EventBatchResponse(
                counts.accepted(),
                counts.deduped(),
                counts.updated(),
                rejections.size(),
                rejections
        );
    }
}
