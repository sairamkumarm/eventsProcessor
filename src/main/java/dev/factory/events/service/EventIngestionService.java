package dev.factory.events.service;

import dev.factory.events.api.dto.EventBatchRequest;
import dev.factory.events.api.dto.EventBatchResponse;

public interface EventIngestionService {
    EventBatchResponse ingest(EventBatchRequest request);
}
