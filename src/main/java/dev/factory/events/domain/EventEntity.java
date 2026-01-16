package dev.factory.events.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(
        name = "events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_events_event_id", columnNames = "event_id")
        }
)
@Data
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "received_time", nullable = false)
    private Instant receivedTime;

    @Column(name = "machine_id", nullable = false, length = 64)
    private String machineId;

    @Column(name = "factory_id", nullable = false, length = 64)
    private String factoryId;

    @Column(name = "line_id", nullable = false, length = 64)
    private String lineId;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "defect_count", nullable = false)
    private int defectCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate(){
        Instant now = Instant.now();
        this.createdAt=now;
        this.updatedAt=now;
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt= Instant.now();
    }

    protected EventEntity(){}

    public EventEntity(
            String eventId,
            Instant eventTime,
            Instant receivedTime,
            String machineId,
            String factoryId,
            String lineId,
            long durationMs,
            int defectCount
    ) {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.receivedTime = receivedTime;
        this.machineId = machineId;
        this.factoryId = factoryId;
        this.lineId = lineId;
        this.durationMs = durationMs;
        this.defectCount = defectCount;
    }


}
