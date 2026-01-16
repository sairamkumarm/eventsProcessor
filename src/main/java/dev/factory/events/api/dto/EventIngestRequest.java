package dev.factory.events.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class EventIngestRequest {

    @NotBlank
    @Size(max = 64)
    private String eventId;

    @NotNull
    private Instant eventTime;

    @NotNull
    private Instant receivedTime;

    @NotBlank
    @Size(max = 64)
    private String machineId;

    @NotBlank
    @Size(max = 64)
    private String factoryId;

    @NotBlank
    @Size(max = 64)
    private String lineId;

    @Min(0)
    private long durationMs;

    // Allow -1 for sentinel semantics
    @Min(-1)
    private int defectCount;
}
