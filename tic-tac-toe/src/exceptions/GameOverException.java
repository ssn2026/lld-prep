package exceptions;

import model.GameStatus;

public class GameOverException extends RuntimeException {
    public GameOverException(GameStatus status) {
        super("Cannot move -- game is already over (" + status + ")");
    }
}
