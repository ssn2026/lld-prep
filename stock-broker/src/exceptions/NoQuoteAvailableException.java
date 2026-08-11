package exceptions;

public class NoQuoteAvailableException extends RuntimeException {
    public NoQuoteAvailableException(String message) {
        super(message);
    }
}
