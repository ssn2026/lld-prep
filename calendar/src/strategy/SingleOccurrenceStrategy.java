package strategy;

import java.time.LocalDateTime;
import java.util.List;

/** RecurrenceType.NONE: exactly one occurrence, ignoring occurrenceCount. */
public class SingleOccurrenceStrategy implements RecurrenceStrategy {
    @Override
    public List<LocalDateTime> generateStartTimes(LocalDateTime firstStart, int occurrenceCount) {
        return List.of(firstStart);
    }
}
