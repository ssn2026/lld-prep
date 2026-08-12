package exceptions;

public class InvalidBoardConfigException extends RuntimeException {
    public InvalidBoardConfigException(String message) {
        super(message);
    }
}
