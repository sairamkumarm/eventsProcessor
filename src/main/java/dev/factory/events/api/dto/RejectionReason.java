package dev.factory.events.api.dto;

public enum RejectionReason {
    INVALID_DURATION,
    EVENT_TIME_TOO_FAR_IN_FUTURE,
    MALFORMED_REQUEST
}
