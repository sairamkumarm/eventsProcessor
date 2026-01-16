package dev.factory.events.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EventBatchRequest {

    @NotEmpty
    @Valid
    private List<EventIngestRequest> events;

    public EventBatchRequest(List<EventIngestRequest> events) {
        this.events = events;
    }

    public EventBatchRequest() {
    }
}
