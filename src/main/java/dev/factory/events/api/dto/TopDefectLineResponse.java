package dev.factory.events.api.dto;

import lombok.Value;

@Value
public class TopDefectLineResponse {
    String lineId;
    long totalDefects;
    long eventCount;
    double defectsPercent;
}
