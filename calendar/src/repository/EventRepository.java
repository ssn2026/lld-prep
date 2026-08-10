package repository;

import exceptions.EventNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Event;

public class EventRepository {

    private final Map<String, Event> eventsById = new LinkedHashMap<>();

    public void save(Event event) {
        eventsById.put(event.getEventId(), event);
    }

    public Event findByEventId(String eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new EventNotFoundException("No event with id " + eventId);
        }
        return event;
    }

    public void remove(String eventId) {
        eventsById.remove(eventId);
    }

    public List<Event> findBySeriesId(String seriesId) {
        List<Event> result = new ArrayList<>();
        for (Event event : eventsById.values()) {
            if (seriesId.equals(event.getSeriesId())) {
                result.add(event);
            }
        }
        return result;
    }
}
