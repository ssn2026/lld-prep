package composite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.Event;

/**
 * Composite pattern: a group aggregates other CalendarComponents -- individual
 * calendars or nested groups -- behind the same interface a single Calendar
 * implements. A member can itself be a CalendarGroup, so a team-of-teams view
 * (e.g. an "Org" group containing a "Backend" group and a standalone user)
 * just works without CalendarService special-casing nesting.
 */
public class CalendarGroup implements CalendarComponent {

    private final String groupId;
    private final List<CalendarComponent> members = new ArrayList<>();

    public CalendarGroup(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void addMember(CalendarComponent member) {
        members.add(member);
    }

    @Override
    public List<Event> getEvents(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Set<Event> merged = new LinkedHashSet<>();
        for (CalendarComponent member : members) {
            merged.addAll(member.getEvents(rangeStart, rangeEnd));
        }
        List<Event> result = new ArrayList<>(merged);
        result.sort(Comparator.comparing(Event::getStart));
        return result;
    }

    @Override
    public boolean isBusy(LocalDateTime instant) {
        for (CalendarComponent member : members) {
            if (member.isBusy(instant)) {
                return true;
            }
        }
        return false;
    }
}
