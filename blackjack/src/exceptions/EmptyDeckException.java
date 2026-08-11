package exceptions;

public class EmptyDeckException extends RuntimeException {
    public EmptyDeckException() {
        super("No cards left in the deck");
    }
}
