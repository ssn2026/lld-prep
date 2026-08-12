package observer;

import exceptions.LogWriteException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Appends one line per call to a real file on disk, creating it if needed. */
public class FileLogAppender implements LogAppender {
    private final Path path;

    public FileLogAppender(Path path) {
        this.path = path;
    }

    @Override
    public void append(String formattedLine) {
        try {
            Files.writeString(path, formattedLine + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new LogWriteException(path, e);
        }
    }
}
