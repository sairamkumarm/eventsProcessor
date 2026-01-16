package dev.factory.events.api.dto;

import lombok.Value;

@Value
public class EventRejection {

    String eventId;
    RejectionReason reason;
}
