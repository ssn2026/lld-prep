package model;

import java.time.LocalDateTime;
import java.util.Set;

public class Event {

    private final String eventId;
    private final String title;
    private final String description;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String ownerId;
    private final Set<String> attendeeIds;
    private final String seriesId;

    public Event(String eventId, String title, String description, LocalDateTime start, LocalDateTime end,
            String ownerId, Set<String> attendeeIds, String seriesId) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.ownerId = ownerId;
        this.attendeeIds = Set.copyOf(attendeeIds);
        this.seriesId = seriesId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Set<String> getAttendeeIds() {
        return attendeeIds;
    }

    /** Null for a one-off event; shared across every occurrence of a recurring series. */
    public String getSeriesId() {
        return seriesId;
    }

    /**
     * Redacted copy for viewers without detail access -- keeps timing (so the
     * busy block is still accurate) but strips title/description/attendees.
     * Used by proxy.RestrictedCalendarProxy.
     */
    public Event redacted() {
        return new Event(eventId, "Busy", null, start, end, ownerId, Set.of(), seriesId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event other)) {
            return false;
        }
        return eventId.equals(other.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}
