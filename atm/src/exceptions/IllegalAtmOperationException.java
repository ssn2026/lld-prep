package exceptions;

public class IllegalAtmOperationException extends RuntimeException {
    public IllegalAtmOperationException(String message) {
        super(message);
    }
}
