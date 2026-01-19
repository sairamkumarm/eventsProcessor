package dev.factory.events.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class TopDefectLinesResponse {
    List<DefectLineResponse> defectLines;
}
