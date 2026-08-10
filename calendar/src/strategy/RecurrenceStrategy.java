package strategy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Strategy pattern: turns a single first occurrence into the full list of
 * occurrence start times for a recurrence type. CalendarService picks the
 * right strategy per RecurrenceType and otherwise treats every kind of
 * recurrence identically -- generate the starts, then run the same
 * conflict-check-then-commit flow regardless of which algorithm produced them.
 */
public interface RecurrenceStrategy {
    List<LocalDateTime> generateStartTimes(LocalDateTime firstStart, int occurrenceCount);
}
