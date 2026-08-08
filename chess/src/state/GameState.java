package state;

import model.Board;
import model.Color;
import model.GameStatus;

/**
 * State pattern: which GameStatus the game is in gates whether
 * ChessGameService.makeMove() accepts further moves (allowsMove()).
 * evaluate() is the shared transition logic all four states funnel through
 * after each half-move so it lives in one place, but each concrete state's
 * allowsMove()/getStatus() differ, which is the actual behavioral variation
 * State pattern buys here (checkmate/stalemate freeze the game; the other
 * two don't).
 */
public interface GameState {

    GameStatus getStatus();

    boolean allowsMove();

    static GameState evaluate(Board board, Color colorToMove) {
        boolean inCheck = board.isSquareAttacked(board.findKing(colorToMove), colorToMove.opposite());
        boolean hasLegalMove = board.hasAnyLegalMove(colorToMove);
        if (!hasLegalMove) {
            return inCheck ? CheckmateState.INSTANCE : StalemateState.INSTANCE;
        }
        return inCheck ? CheckState.INSTANCE : InProgressState.INSTANCE;
    }
}
