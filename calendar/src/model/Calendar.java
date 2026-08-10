package model;

import composite.CalendarComponent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Calendar implements CalendarComponent {

    private final String ownerId;
    private final Map<String, Event> eventsById = new LinkedHashMap<>();

    public Calendar(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void addEvent(Event event) {
        eventsById.put(event.getEventId(), event);
    }

    public void removeEvent(String eventId) {
        eventsById.remove(eventId);
    }

    public boolean hasConflict(LocalDateTime start, LocalDateTime end) {
        for (Event event : eventsById.values()) {
            if (start.isBefore(event.getEnd()) && event.getStart().isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Event> getEvents(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<Event> result = new ArrayList<>();
        for (Event event : eventsById.values()) {
            if (rangeStart.isBefore(event.getEnd()) && event.getStart().isBefore(rangeEnd)) {
                result.add(event);
            }
        }
        result.sort(Comparator.comparing(Event::getStart));
        return result;
    }

    @Override
    public boolean isBusy(LocalDateTime instant) {
        for (Event event : eventsById.values()) {
            if (!instant.isBefore(event.getStart()) && instant.isBefore(event.getEnd())) {
                return true;
            }
        }
        return false;
    }
}
