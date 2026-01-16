package dev.factory.events.service.model;

import lombok.Value;

import java.time.Instant;

@Value
public class NormalizedEvent {

    String eventId;
    Instant eventTime;
    Instant receivedTime;
    String machineId;
    String factoryId;
    String lineId;
    long durationMs;
    int defectCount;
}
