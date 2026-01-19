package dev.factory.events.api.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DefectLineResponse {

    String lineId;
    long totalDefects;
    long eventCount;
    double defectsPercent;
}
