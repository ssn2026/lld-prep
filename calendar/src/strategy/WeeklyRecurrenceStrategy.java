package strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WeeklyRecurrenceStrategy implements RecurrenceStrategy {
    @Override
    public List<LocalDateTime> generateStartTimes(LocalDateTime firstStart, int occurrenceCount) {
        List<LocalDateTime> starts = new ArrayList<>();
        for (int i = 0; i < occurrenceCount; i++) {
            starts.add(firstStart.plusWeeks(i));
        }
        return starts;
    }
}
