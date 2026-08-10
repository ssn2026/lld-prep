package exceptions;

public class CalendarGroupNotFoundException extends RuntimeException {
    public CalendarGroupNotFoundException(String message) {
        super(message);
    }
}
