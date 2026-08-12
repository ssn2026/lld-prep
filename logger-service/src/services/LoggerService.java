package services;

import java.util.ArrayList;
import java.util.List;
import model.LogEvent;
import model.LogLevel;
import observer.LogAppender;
import strategy.LogFormatter;
import strategy.PlainTextFormatter;

/**
 * The single public entry point, and a Singleton: exactly one logger for
 * the whole process (real loggers are inherently process-wide state, so
 * unlike every other problem's services/ class -- which the *caller*
 * instantiates -- this one instantiates itself and hands out the same
 * instance to everyone).
 */
public final class LoggerService {
    private static final LoggerService INSTANCE = new LoggerService();

    private final List<LogAppender> appenders = new ArrayList<>();
    private LogFormatter formatter = new PlainTextFormatter();
    private LogLevel minLevel = LogLevel.DEBUG;
    private int seq = 0;

    private LoggerService() {
    }

    public static LoggerService getInstance() {
        return INSTANCE;
    }

    public void setMinLevel(LogLevel level) {
        this.minLevel = level;
    }

    public void setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
    }

    public void addAppender(LogAppender appender) {
        appenders.add(appender);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void log(LogLevel level, String message) {
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }
        LogEvent event = new LogEvent(++seq, level, message);
        String formatted = formatter.format(event);
        for (LogAppender appender : appenders) {
            appender.append(formatted);
        }
    }
}
