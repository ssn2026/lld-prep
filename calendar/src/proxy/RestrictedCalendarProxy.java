package proxy;

import composite.CalendarComponent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Calendar;
import model.Event;

/**
 * Protection proxy: wraps a real Calendar and, for any viewer who isn't the
 * owner or an invited attendee, swaps each event's details for a redacted
 * "Busy" placeholder before returning it. CalendarService always looks
 * through this proxy when a caller views someone else's calendar, so the
 * privacy rule lives in exactly one place instead of being re-checked by
 * every caller. Busy/free status itself (isBusy()) is never restricted --
 * only the event details are.
 */
public class RestrictedCalendarProxy implements CalendarComponent {

    private final Calendar realCalendar;
    private final String viewerId;

    public RestrictedCalendarProxy(Calendar realCalendar, String viewerId) {
        this.realCalendar = realCalendar;
        this.viewerId = viewerId;
    }

    @Override
    public List<Event> getEvents(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        List<Event> visible = new ArrayList<>();
        for (Event event : realCalendar.getEvents(rangeStart, rangeEnd)) {
            visible.add(canViewDetails(event) ? event : event.redacted());
        }
        return visible;
    }

    @Override
    public boolean isBusy(LocalDateTime instant) {
        return realCalendar.isBusy(instant);
    }

    private boolean canViewDetails(Event event) {
        return viewerId.equals(realCalendar.getOwnerId()) || event.getAttendeeIds().contains(viewerId);
    }
}
