package model;

/**
 * seq is used instead of a wall-clock timestamp so test transcripts stay
 * byte-for-byte reproducible across runs (same simplification job-scheduler's
 * WALKTHROUGH.md flags for real timestamps -- here we sidestep it outright
 * since a logger has no real concurrency to demonstrate).
 */
public class LogEvent {
    private final int seq;
    private final LogLevel level;
    private final String message;

    public LogEvent(int seq, LogLevel level, String message) {
        this.seq = seq;
        this.level = level;
        this.message = message;
    }

    public int getSeq() {
        return seq;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
