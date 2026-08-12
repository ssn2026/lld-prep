package exceptions;

public class GameAlreadyWonException extends RuntimeException {
    public GameAlreadyWonException(String winner) {
        super("Game is already over -- " + winner + " won");
    }
}
