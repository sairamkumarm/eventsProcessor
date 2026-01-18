package dev.factory.events.service;

import dev.factory.events.api.dto.*;
import dev.factory.events.domain.EventEntity;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.service.model.NormalizedEvent;
import dev.factory.events.util.ClockProvider;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service("JPAIngestion")
public class DbBackedIngestionService implements EventIngestionService {

    private static final Duration MAX_DURATION = Duration.ofHours(6);
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(15);

    private final EventRepository eventRepository;
    private final ClockProvider clock;

    public DbBackedIngestionService(EventRepository eventRepository, @Qualifier("runningClock") ClockProvider clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EventBatchResponse ingest(EventBatchRequest request) {

        Instant now = clock.now();

        List<EventRejection> rejections = new ArrayList<>();
        List<NormalizedEvent> validEvents = new ArrayList<>();

        //reject on duration and time
        for (EventIngestRequest raw : request.getEvents()) {

            if (raw.getDurationMs() > MAX_DURATION.toMillis()) {
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

            validEvents.add(new NormalizedEvent(
                    raw.getEventId(),
                    raw.getEventTime(),
                    now,
                    raw.getMachineId(),
                    raw.getFactoryId(),
                    raw.getLineId(),
                    raw.getDurationMs(),
                    raw.getDefectCount()
            ));
        }

        // in-request de-duplication with eventId
        Map<String, NormalizedEvent> candidates = new HashMap<>();

        for (NormalizedEvent event : validEvents) {
            NormalizedEvent existing = candidates.get(event.getEventId());
            if (existing == null || event.getReceivedTime().isAfter(existing.getReceivedTime())) {
                candidates.put(event.getEventId(), event);
            }
        }

        if (candidates.isEmpty()) {
            return new EventBatchResponse(
                    0,
                    0,
                    0,
                    rejections.size(),
                    rejections
            );
        }

        //load existing state with locks
        List<String> eventIds = new ArrayList<>(candidates.keySet());
        List<EventEntity> existingEntities =
                eventRepository.findAllByEventIdInForUpdate(eventIds);

        Map<String, EventEntity> existingByEventId =
                existingEntities.stream()
                        .collect(Collectors.toMap(EventEntity::getEventId, e -> e));

        int accepted = 0;
        int updated = 0;
        int deduped = 0;

        List<EventEntity> toInsert = new ArrayList<>();
        List<EventEntity> toUpdate = new ArrayList<>();

        // re-normalise with db data
        for (NormalizedEvent candidate : candidates.values()) {

            EventEntity existing = existingByEventId.get(candidate.getEventId());

            if (existing == null) {
                toInsert.add(toEntity(candidate));
                accepted++;
                continue;
            }

            if (isIdentical(existing, candidate)) {
                deduped++;
                continue;
            }

            if (candidate.getReceivedTime().isAfter(existing.getReceivedTime())) {
                applyUpdate(existing, candidate);
                toUpdate.add(existing);
                updated++;
            }
        }

        // Step 5: batch write
        if (!toInsert.isEmpty()) {
            eventRepository.saveAll(toInsert);
        }

        if (!toUpdate.isEmpty()) {
            eventRepository.saveAll(toUpdate);
        }

        return new EventBatchResponse(
                accepted,
                deduped,
                updated,
                rejections.size(),
                rejections
        );
    }

    private EventEntity toEntity(NormalizedEvent e) {
        return new EventEntity(
                e.getEventId(),
                e.getEventTime(),
                e.getReceivedTime(),
                e.getMachineId(),
                e.getFactoryId(),
                e.getLineId(),
                e.getDurationMs(),
                e.getDefectCount()
        );
    }

    private void applyUpdate(EventEntity entity, NormalizedEvent e) {
        entity.setEventTime(e.getEventTime());
        entity.setReceivedTime(e.getReceivedTime());
        entity.setMachineId(e.getMachineId());
        entity.setFactoryId(e.getFactoryId());
        entity.setLineId(e.getLineId());
        entity.setDurationMs(e.getDurationMs());
        entity.setDefectCount(e.getDefectCount());
    }

    private boolean isIdentical(EventEntity entity, NormalizedEvent e) {
        return entity.getEventTime().equals(e.getEventTime()) &&
                entity.getMachineId().equals(e.getMachineId()) &&
                entity.getFactoryId().equals(e.getFactoryId()) &&
                entity.getLineId().equals(e.getLineId()) &&
                entity.getDurationMs() == e.getDurationMs() &&
                entity.getDefectCount() == e.getDefectCount();
    }
}
