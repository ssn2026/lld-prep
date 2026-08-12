package state;

import exceptions.GameOverException;
import model.GameStatus;

/**
 * Game-phase lifecycle, held once on TicTacToeService -- one game per
 * service instance, same shape as the ATM's AtmState and CrickInfo's
 * MatchState.
 */
public interface GameState {
    GameStatus getStatus();

    default void requireInProgress() {
        throw new GameOverException(getStatus());
    }
}
