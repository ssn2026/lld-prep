package strategy;

import model.LogEvent;

public class PlainTextFormatter implements LogFormatter {
    @Override
    public String format(LogEvent event) {
        return "[" + event.getSeq() + "] " + event.getLevel() + " " + event.getMessage();
    }
}
