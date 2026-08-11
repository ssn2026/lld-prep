package exceptions;

public class RoundNotReadyException extends RuntimeException {
    public RoundNotReadyException(String message) {
        super(message);
    }
}
