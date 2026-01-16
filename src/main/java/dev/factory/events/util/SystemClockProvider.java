package dev.factory.events.util;

import java.time.Instant;

public class SystemClockProvider implements ClockProvider{
    @Override
    public Instant now() {
        return Instant.now();
    }
}
