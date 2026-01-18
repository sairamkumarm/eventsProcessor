package dev.factory.events.api.dto;

import lombok.Data;

@Data
public class TopDefectLineResponse {
    String lineId;
    long totalDefects;
    long eventCount;
    double defectsPercent;
}
