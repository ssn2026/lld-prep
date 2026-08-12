package exceptions;

import java.nio.file.Path;

public class LogWriteException extends RuntimeException {
    public LogWriteException(Path path, Throwable cause) {
        super("Failed to write log to " + path, cause);
    }
}
