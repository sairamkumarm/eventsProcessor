package dev.factory.events.testConfig;

import dev.factory.events.util.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
public class FixedClockTestConfig {

    public static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-01-15T10:00:00Z"));

    @Bean("fixedClock")
    public ClockProvider clockProvider() {
        return NOW::get;
    }

    public static void advanceSeconds(long seconds) {
        NOW.updateAndGet(t -> t.plusSeconds(seconds));
    }

    public static void set(Instant instant) {
        NOW.set(instant);
    }

    public static void reset(){
        NOW.set(Instant.parse("2026-01-15T10:00:00Z"));
    }
}
