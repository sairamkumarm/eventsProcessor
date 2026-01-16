CREATE INDEX idx_events_machine_time ON events(machine_id, event_time);
CREATE INDEX idx_events_factory_line_time ON events(factory_id, line_id, event_time);
