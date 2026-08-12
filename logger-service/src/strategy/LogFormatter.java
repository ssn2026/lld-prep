package strategy;

import model.LogEvent;

public interface LogFormatter {
    String format(LogEvent event);
}
