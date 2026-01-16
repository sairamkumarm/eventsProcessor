package dev.factory.events.api.dto;

import lombok.Value;

import java.util.List;

@Value
public class EventBatchResponse {

    int accepted;
    int deduped;
    int updated;
    int rejected;

    List<EventRejection> rejections;
}
