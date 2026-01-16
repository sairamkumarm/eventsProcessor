package dev.factory.events.repository;

import dev.factory.events.AbstractIntegrationTest;
import dev.factory.events.domain.EventEntity;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class EventRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldInsertAndLoadEvent(){
        EventEntity e = new EventEntity(
                "E-01",
                Instant.parse("2026-01-15T10:00:00Z"),
                Instant.now(),
                "M-001",
                "F01",
                "L-01",
                1000,
                0
        );

        eventRepository.save(e);

        List<EventEntity> loaded = eventRepository.findAllByEventIdInForUpdate(List.of("E-01"));
        assertThat(loaded).hasSize(1);
        assertThat(loaded.getFirst().getMachineId()).isEqualTo("M-001");
    }
}
