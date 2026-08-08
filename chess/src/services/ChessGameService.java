package services;

import exceptions.GameOverException;
import exceptions.InvalidMoveException;
import factory.PieceFactory;
import java.util.ArrayList;
import java.util.List;
import model.Board;
import model.Color;
import model.GameStatus;
import model.Move;
import model.Piece;
import model.PieceType;
import model.Player;
import model.Position;
import observer.GameObserver;
import state.GameState;
import state.InProgressState;

/**
 * Single public entry point for the chess system. All caller-facing
 * operations (making a move, inspecting the board/status) go through this
 * class; model/factory/state/observer classes are internal collaborators.
 */
public class ChessGameService {

    private static final PieceType[] BACK_RANK_ORDER = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
    };

    private final Board board = new Board();
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final List<Move> moveHistory = new ArrayList<>();
    private final List<GameObserver> observers = new ArrayList<>();

    private Color currentTurn = Color.WHITE;
    private GameState currentState = InProgressState.INSTANCE;

    public ChessGameService(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        setupBoard();
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public Move makeMove(String fromAlgebraic, String toAlgebraic) {
        return makeMove(fromAlgebraic, toAlgebraic, null);
    }

    public Move makeMove(String fromAlgebraic, String toAlgebraic, PieceType promotionChoice) {
        if (!currentState.allowsMove()) {
            throw new GameOverException("Game is over (" + currentState.getStatus() + "); no more moves allowed");
        }

        Position from = Position.of(fromAlgebraic);
        Position to = Position.of(toAlgebraic);

        Piece piece = board.getPiece(from);
        if (piece == null) {
            throw new InvalidMoveException("No piece at " + fromAlgebraic);
        }
        if (piece.getColor() != currentTurn) {
            throw new InvalidMoveException("It is " + currentTurn + "'s turn, not " + piece.getColor());
        }
        if (!piece.getPossibleMoves(board).contains(to)) {
            throw new InvalidMoveException(piece.getType() + " cannot move from " + fromAlgebraic + " to " + toAlgebraic);
        }
        if (!board.isMoveLegal(piece, to)) {
            throw new InvalidMoveException("Moving " + fromAlgebraic + " to " + toAlgebraic + " would leave "
                    + currentTurn + "'s king in check");
        }

        Piece captured = board.getPiece(to);
        board.removePiece(from);
        if (captured != null) {
            board.removePiece(to);
        }
        piece.setPosition(to);
        piece.setHasMoved(true);
        board.placePiece(piece, to);

        PieceType promotedTo = applyPromotionIfNeeded(piece, to, promotionChoice);

        Move move = new Move(from, to, piece.getType(), currentTurn,
                captured != null ? captured.getType() : null, promotedTo);
        moveHistory.add(move);

        currentTurn = currentTurn.opposite();
        currentState = GameState.evaluate(board, currentTurn);

        notifyObservers(move);
        return move;
    }

    public String renderBoard() {
        return board.render();
    }

    public GameStatus getStatus() {
        return currentState.getStatus();
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    private PieceType applyPromotionIfNeeded(Piece piece, Position to, PieceType promotionChoice) {
        boolean reachedBackRank = to.getRow() == 0 || to.getRow() == 7;
        if (piece.getType() != PieceType.PAWN || !reachedBackRank) {
            return null;
        }
        PieceType chosen = promotionChoice != null ? promotionChoice : PieceType.QUEEN;
        board.removePiece(to);
        Piece promoted = PieceFactory.createPiece(chosen, piece.getColor(), to);
        promoted.setHasMoved(true);
        board.placePiece(promoted, to);
        return chosen;
    }

    private void notifyObservers(Move move) {
        for (GameObserver observer : observers) {
            observer.onMove(move);
        }
        GameStatus status = currentState.getStatus();
        if (status == GameStatus.CHECK) {
            for (GameObserver observer : observers) {
                observer.onCheck(currentTurn);
            }
        } else if (status == GameStatus.CHECKMATE) {
            Color winner = currentTurn.opposite();
            for (GameObserver observer : observers) {
                observer.onGameOver(status, winner);
            }
        } else if (status == GameStatus.STALEMATE) {
            for (GameObserver observer : observers) {
                observer.onGameOver(status, null);
            }
        }
    }

    private void setupBoard() {
        for (int col = 0; col < 8; col++) {
            board.placePiece(PieceFactory.createPiece(BACK_RANK_ORDER[col], Color.WHITE, new Position(0, col)), new Position(0, col));
            board.placePiece(PieceFactory.createPiece(PieceType.PAWN, Color.WHITE, new Position(1, col)), new Position(1, col));
            board.placePiece(PieceFactory.createPiece(PieceType.PAWN, Color.BLACK, new Position(6, col)), new Position(6, col));
            board.placePiece(PieceFactory.createPiece(BACK_RANK_ORDER[col], Color.BLACK, new Position(7, col)), new Position(7, col));
        }
    }
}
