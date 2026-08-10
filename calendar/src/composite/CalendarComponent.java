package composite;

import java.time.LocalDateTime;
import java.util.List;
import model.Event;

/**
 * Composite pattern: the contract a single Calendar (leaf) and a
 * CalendarGroup (composite) both satisfy, so CalendarService can query "what
 * events does X have in this range" / "is X busy right now" without caring
 * whether X is one person's calendar or an aggregated team view.
 */
public interface CalendarComponent {

    List<Event> getEvents(LocalDateTime rangeStart, LocalDateTime rangeEnd);

    boolean isBusy(LocalDateTime instant);
}
