package dev.factory.events.api.dto;

import lombok.Value;

import java.time.Instant;

@Value
public class MachineStatsResponse {

    String machineId;
    Instant start;
    Instant end;

    long eventsCount;
    long defectsCount;
    double avgDefectRate;

    String status;
}
