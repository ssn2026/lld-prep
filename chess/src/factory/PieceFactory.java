package factory;

import model.Bishop;
import model.Color;
import model.King;
import model.Knight;
import model.Pawn;
import model.Piece;
import model.PieceType;
import model.Position;
import model.Queen;
import model.Rook;

/**
 * Centralizes Piece construction so callers (initial board setup, pawn
 * promotion) select a piece by PieceType/Color without knowing the concrete
 * subclasses.
 */
public final class PieceFactory {

    private PieceFactory() {
    }

    public static Piece createPiece(PieceType type, Color color, Position position) {
        switch (type) {
            case PAWN: return new Pawn(color, position);
            case KNIGHT: return new Knight(color, position);
            case BISHOP: return new Bishop(color, position);
            case ROOK: return new Rook(color, position);
            case QUEEN: return new Queen(color, position);
            case KING: return new King(color, position);
            default: throw new IllegalArgumentException("Unknown piece type: " + type);
        }
    }
}
