package dev.factory.events.repository;

import dev.factory.events.domain.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<EventEntity,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM EventEntity e
            WHERE e.eventId IN :eventIds
            """)
    List<EventEntity> findAllByEventIdInForUpdate(@Param("eventIds") List<String> eventIds);


    //FOR TESTS ONLY
    @Query("""
    SELECT e
    FROM EventEntity e
    WHERE e.eventId IN :eventIds
    """)
    List<EventEntity> findAllByEventIdIn(@Param("eventIds") List<String> eventIds);

}
