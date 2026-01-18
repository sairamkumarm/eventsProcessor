package dev.factory.events.service;

import dev.factory.events.api.dto.TopDefectLineResponse;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.repository.TopDefectLineProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineStatsService {

    private final EventRepository eventRepository;

    public LineStatsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<TopDefectLineResponse> getTopDefectLines(
            String factoryId,
            Instant from,
            Instant to,
            int limit
    ) {
        List<TopDefectLineProjection> raw =
                eventRepository.findTopDefectLinesNative(factoryId, from, to, limit);

        return raw.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TopDefectLineResponse toResponse(TopDefectLineProjection p) {
        long eventCount = p.getEventCount();
        long totalDefects = p.getTotalDefects();

        double defectsPercent;
        if (eventCount == 0) {
            defectsPercent = 0.0;
        } else {
            defectsPercent = roundToTwoDecimals(
                    (totalDefects * 100.0) / eventCount
            );
        }

        TopDefectLineResponse r = new TopDefectLineResponse();
        r.setLineId(p.getLineId());
        r.setTotalDefects(totalDefects);
        r.setEventCount(eventCount);
        r.setDefectsPercent(defectsPercent);

        return r;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
