package exceptions;

public class InvalidLogLevelException extends RuntimeException {
    public InvalidLogLevelException(String levelText) {
        super("Unknown log level: " + levelText);
    }
}
