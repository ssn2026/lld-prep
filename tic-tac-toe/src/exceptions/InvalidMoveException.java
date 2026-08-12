package exceptions;

public class InvalidMoveException extends RuntimeException {
    public InvalidMoveException(int row, int col, String reason) {
        super("Invalid move at (" + row + "," + col + "): " + reason);
    }
}
