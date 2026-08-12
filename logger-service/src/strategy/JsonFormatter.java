package strategy;

import model.LogEvent;

public class JsonFormatter implements LogFormatter {
    @Override
    public String format(LogEvent event) {
        return "{\"seq\":" + event.getSeq() + ",\"level\":\"" + event.getLevel()
                + "\",\"message\":\"" + escape(event.getMessage()) + "\"}";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
