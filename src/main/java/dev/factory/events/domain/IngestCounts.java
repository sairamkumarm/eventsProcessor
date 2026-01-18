package dev.factory.events.domain;

public record IngestCounts(
        int accepted,
        int updated,
        int deduped
) {}

