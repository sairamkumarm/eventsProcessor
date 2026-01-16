package dev.factory.events.util;

import java.time.Instant;

public interface ClockProvider {
    Instant now();
}
