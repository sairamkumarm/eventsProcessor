package dev.factory.events.api;

import dev.factory.events.api.dto.EventBatchRequest;
import dev.factory.events.api.dto.EventBatchResponse;
import dev.factory.events.service.EventIngestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("events")
public class eventController {

    @Autowired
    EventIngestionService service;

    @PostMapping("batch")
    public EventBatchResponse batchIngestEvents(@Valid @RequestBody EventBatchRequest req){
//        return service.ingest(req);
        long start = System.currentTimeMillis();

        EventBatchResponse response = service.ingest(req);

        long end = System.currentTimeMillis();
        System.out.println("Ingested " + req.getEvents().size() + " events in " + (end - start) + " ms");

        return response;
    }
}
