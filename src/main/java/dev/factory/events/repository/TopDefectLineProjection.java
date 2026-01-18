package dev.factory.events.repository;

public interface TopDefectLineProjection {

    String getLineId();

    Long getTotalDefects();

    Long getEventCount();
}
