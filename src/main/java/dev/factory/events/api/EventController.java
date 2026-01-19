package dev.factory.events.api;

import dev.factory.events.api.dto.EventBatchRequest;
import dev.factory.events.api.dto.EventBatchResponse;
import dev.factory.events.api.dto.MachineStatsResponse;
import dev.factory.events.api.dto.TopDefectLinesResponse;
import dev.factory.events.service.EventIngestionService;
import dev.factory.events.service.LineStatsService;
import dev.factory.events.service.MachineStatsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Validated
@RestController
@RequestMapping("events")
public class EventController {

    @Autowired
    EventIngestionService eventIngestionService;

    @Autowired
    MachineStatsService machineStatsService;

    @Autowired
    LineStatsService lineStatsService;

    @PostMapping("batch")
    public EventBatchResponse batchIngestEvents(@Valid @RequestBody EventBatchRequest req){
        long start = System.currentTimeMillis();
        EventBatchResponse response = eventIngestionService.ingest(req);
        long end = System.currentTimeMillis();
        System.out.println("Ingested " + req.getEvents().size() + " events in " + (end - start) + " ms");

        return response;
    }

    @GetMapping("stats")
    public MachineStatsResponse getMachineStats(@RequestParam String machineId, @RequestParam Instant start, @RequestParam Instant end){
        return machineStatsService.getStats(machineId,start,end);
    }

    @GetMapping("stats/top-defect-lines")
    public TopDefectLinesResponse getTopDefectLines(@RequestParam String factoryId, @RequestParam Instant start, @RequestParam Instant end, @RequestParam int limit){
        return lineStatsService.getTopDefectLines(factoryId,start,end,limit);
    }

}
