package dev.factory.events.service;

import dev.factory.events.api.dto.MachineStatsResponse;
import dev.factory.events.repository.EventRepository;
import dev.factory.events.repository.MachineStatsProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class MachineStatsService {


    private final EventRepository eventRepository;

    public MachineStatsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public MachineStatsResponse getStats(
            String machineId,
            Instant startTime,
            Instant endTime
    ) {
        MachineStatsProjection stats =
                eventRepository.fetchStats(machineId, startTime, endTime);

        long events = stats.getEventsCount();
        long defects = stats.getDefectsCount();
        long hours = Duration.between(startTime,endTime).toHours();
        double avgDefectRate =
                events == 0 ? 0.0 : (double) defects / hours;
        avgDefectRate = Math.round(avgDefectRate * 100)/100.00;
        MachineStatsResponse res = new MachineStatsResponse();
        res.setMachineId(machineId);
        res.setStart(startTime);
        res.setEnd(endTime);
        res.setEventsCount(events);
        res.setDefectsCount(defects);
        res.setAvgDefectRate(avgDefectRate);
        res.setStatus(avgDefectRate >= 2 ? "WARNING" : "HEALTHY");

        return res;
    }
}
