package dev.factory.events.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

//@Profile("!test")
@Component("runningClock")
public class SystemClockProvider implements ClockProvider{
    @Override
    public Instant now() {
        return Instant.now();
    }

    public SystemClockProvider(){}
}
