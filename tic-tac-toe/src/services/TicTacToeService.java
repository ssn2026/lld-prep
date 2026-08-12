package services;

import exceptions.InvalidMoveException;
import java.util.ArrayList;
import java.util.List;
import model.Board;
import model.GameStatus;
import model.Mark;
import observer.GameListener;
import state.DrawState;
import state.GameState;
import state.InProgressState;
import state.OWonState;
import state.XWonState;
import strategy.WinningStrategy;

/**
 * The single public entry point. Owns the Board, the current GameState
 * (game-phase lifecycle, held once here -- one game per service instance),
 * and whichever WinningStrategy decides what counts as a win.
 */
public class TicTacToeService {
    private final Board board;
    private final WinningStrategy winningStrategy;
    private final List<GameListener> listeners = new ArrayList<>();
    private GameState state = InProgressState.INSTANCE;
    private Mark currentMark = Mark.X;

    public TicTacToeService(int size, WinningStrategy winningStrategy) {
        this.board = new Board(size);
        this.winningStrategy = winningStrategy;
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void makeMove(int row, int col) {
        state.requireInProgress();
        if (!board.isInBounds(row, col)) {
            throw new InvalidMoveException(row, col, "out of bounds");
        }
        if (!board.isEmpty(row, col)) {
            throw new InvalidMoveException(row, col, "cell already occupied");
        }

        board.place(row, col, currentMark);
        notifyMove(currentMark, row, col);

        boolean won = winningStrategy.checkWinner(board, row, col, currentMark);
        if (won) {
            state = currentMark == Mark.X ? XWonState.INSTANCE : OWonState.INSTANCE;
            notifyGameOver(state.getStatus());
            return;
        }
        if (board.isFull()) {
            state = DrawState.INSTANCE;
            notifyGameOver(state.getStatus());
            return;
        }
        currentMark = currentMark == Mark.X ? Mark.O : Mark.X;
    }

    public GameStatus getStatus() {
        return state.getStatus();
    }

    public String renderBoard() {
        return board.render();
    }

    private void notifyMove(Mark mark, int row, int col) {
        for (GameListener listener : listeners) {
            listener.onMove(mark, row, col);
        }
    }

    private void notifyGameOver(GameStatus result) {
        for (GameListener listener : listeners) {
            listener.onGameOver(result);
        }
    }
}
