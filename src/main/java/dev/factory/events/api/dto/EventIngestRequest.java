package dev.factory.events.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class EventIngestRequest {

    @NotBlank
    @Size(max = 64)
    @Pattern(
            regexp = "^E-\\d+$",
            message = "EventId must be in format E-<digits>"
    )
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
    @Pattern(
            regexp = "^F-\\d+$",
            message = "FactoryId must be in format F-<digits>"
    )
    private String factoryId;

    @NotBlank
    @Size(max = 64)
    @Pattern(
            regexp = "^L-\\d+$",
            message = "LineId must be in format L-<digits>"
    )
    private String lineId;

    @Min(0)
    private long durationMs;

    // Allow -1 for sentinel semantics
    @Min(-1)
    private int defectCount;
}
