package services;

import composite.CalendarComponent;
import composite.CalendarGroup;
import exceptions.EventConflictException;
import exceptions.EventNotFoundException;
import exceptions.InvalidEventException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import model.Calendar;
import model.Event;
import model.RecurrenceType;
import model.User;
import proxy.RestrictedCalendarProxy;
import repository.CalendarGroupRepository;
import repository.CalendarRepository;
import repository.EventRepository;
import repository.UserRepository;
import strategy.DailyRecurrenceStrategy;
import strategy.MonthlyRecurrenceStrategy;
import strategy.RecurrenceStrategy;
import strategy.SingleOccurrenceStrategy;
import strategy.WeeklyRecurrenceStrategy;

/**
 * Single public entry point for the calendar system. All caller-facing
 * operations (user/event/group management, viewing) go through this class;
 * model/composite/proxy/strategy/repository classes are internal
 * collaborators.
 */
public class CalendarService {

    private final UserRepository userRepository = new UserRepository();
    private final CalendarRepository calendarRepository = new CalendarRepository();
    private final EventRepository eventRepository = new EventRepository();
    private final CalendarGroupRepository groupRepository = new CalendarGroupRepository();
    private final AtomicInteger eventSequence = new AtomicInteger(1);
    private final AtomicInteger seriesSequence = new AtomicInteger(1);

    public void registerUser(String userId, String name) {
        userRepository.save(new User(userId, name));
        calendarRepository.save(new Calendar(userId));
    }

    /**
     * Validates the whole series (each occurrence against every participant's
     * calendar, and occurrences against each other) before committing any of
     * it -- a conflict on occurrence 3 of 4 must not leave 1-2 half-booked.
     */
    public List<Event> createEvent(String ownerId, String title, String description,
            LocalDateTime start, LocalDateTime end, Set<String> attendeeIds,
            RecurrenceType recurrenceType, int occurrenceCount) {
        if (!end.isAfter(start)) {
            throw new InvalidEventException("Event end must be after start");
        }

        Set<String> participants = new LinkedHashSet<>(attendeeIds);
        participants.add(ownerId);
        for (String participantId : participants) {
            userRepository.findByUserId(participantId);
        }

        RecurrenceStrategy strategy = resolveStrategy(recurrenceType);
        List<LocalDateTime> occurrenceStarts = strategy.generateStartTimes(start, occurrenceCount);
        Duration duration = Duration.between(start, end);

        requireOccurrencesDontOverlapEachOther(occurrenceStarts, duration);
        for (LocalDateTime occurrenceStart : occurrenceStarts) {
            LocalDateTime occurrenceEnd = occurrenceStart.plus(duration);
            for (String participantId : participants) {
                if (calendarRepository.findByOwnerId(participantId).hasConflict(occurrenceStart, occurrenceEnd)) {
                    throw new EventConflictException("Event conflicts with an existing event on "
                            + participantId + "'s calendar at " + occurrenceStart);
                }
            }
        }

        String seriesId = recurrenceType == RecurrenceType.NONE ? null : "SERIES" + seriesSequence.getAndIncrement();
        List<Event> occurrences = new ArrayList<>();
        for (LocalDateTime occurrenceStart : occurrenceStarts) {
            occurrences.add(new Event("E" + eventSequence.getAndIncrement(), title, description,
                    occurrenceStart, occurrenceStart.plus(duration), ownerId, attendeeIds, seriesId));
        }

        for (Event event : occurrences) {
            eventRepository.save(event);
            for (String participantId : participants) {
                calendarRepository.findByOwnerId(participantId).addEvent(event);
            }
        }
        return occurrences;
    }

    public void cancelEvent(String eventId) {
        Event event = eventRepository.findByEventId(eventId);
        removeFromAllCalendars(event);
        eventRepository.remove(eventId);
    }

    public void cancelSeries(String seriesId) {
        List<Event> seriesEvents = eventRepository.findBySeriesId(seriesId);
        if (seriesEvents.isEmpty()) {
            throw new EventNotFoundException("No events found for series " + seriesId);
        }
        for (Event event : seriesEvents) {
            removeFromAllCalendars(event);
            eventRepository.remove(event.getEventId());
        }
    }

    public List<Event> viewCalendar(String viewerId, String ownerId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        userRepository.findByUserId(viewerId);
        Calendar calendar = calendarRepository.findByOwnerId(ownerId);
        CalendarComponent view = new RestrictedCalendarProxy(calendar, viewerId);
        return view.getEvents(rangeStart, rangeEnd);
    }

    public void createGroup(String groupId) {
        groupRepository.save(new CalendarGroup(groupId));
    }

    /** memberId may be either a userId (adds that user's calendar) or an existing groupId (nests a sub-group). */
    public void addGroupMember(String groupId, String memberId) {
        CalendarGroup group = groupRepository.findByGroupId(groupId);
        CalendarComponent member = groupRepository.exists(memberId)
                ? groupRepository.findByGroupId(memberId)
                : calendarRepository.findByOwnerId(memberId);
        group.addMember(member);
    }

    public List<Event> getGroupEvents(String groupId, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return groupRepository.findByGroupId(groupId).getEvents(rangeStart, rangeEnd);
    }

    private void removeFromAllCalendars(Event event) {
        Set<String> participants = new LinkedHashSet<>(event.getAttendeeIds());
        participants.add(event.getOwnerId());
        for (String participantId : participants) {
            calendarRepository.findByOwnerId(participantId).removeEvent(event.getEventId());
        }
    }

    private void requireOccurrencesDontOverlapEachOther(List<LocalDateTime> starts, Duration duration) {
        for (int i = 0; i < starts.size(); i++) {
            LocalDateTime aStart = starts.get(i);
            LocalDateTime aEnd = aStart.plus(duration);
            for (int j = i + 1; j < starts.size(); j++) {
                LocalDateTime bStart = starts.get(j);
                LocalDateTime bEnd = bStart.plus(duration);
                if (aStart.isBefore(bEnd) && bStart.isBefore(aEnd)) {
                    throw new InvalidEventException(
                            "Recurring occurrences overlap each other (duration exceeds recurrence interval)");
                }
            }
        }
    }

    private RecurrenceStrategy resolveStrategy(RecurrenceType type) {
        return switch (type) {
            case NONE -> new SingleOccurrenceStrategy();
            case DAILY -> new DailyRecurrenceStrategy();
            case WEEKLY -> new WeeklyRecurrenceStrategy();
            case MONTHLY -> new MonthlyRecurrenceStrategy();
        };
    }
}
