package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the 8x8 grid plus every piece of board-scoped logic that needs a
 * whole-board view: attack detection, full-legality filtering (a pseudo-legal
 * move from Piece.getPossibleMoves is only legal if it doesn't leave the
 * mover's own king in check), and simulation via copy(). Centralizing this
 * here means ChessGameService (validating a submitted move) and the state/
 * classes (deciding check/checkmate/stalemate) share one implementation
 * instead of two copies drifting apart.
 */
public class Board {

    private final Piece[][] grid = new Piece[8][8];

    public Piece getPiece(Position pos) {
        return grid[pos.getRow()][pos.getCol()];
    }

    public void placePiece(Piece piece, Position pos) {
        grid[pos.getRow()][pos.getCol()] = piece;
    }

    public void removePiece(Position pos) {
        grid[pos.getRow()][pos.getCol()] = null;
    }

    public List<Piece> getPiecesByColor(Color color) {
        List<Piece> pieces = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null && piece.getColor() == color) {
                    pieces.add(piece);
                }
            }
        }
        return pieces;
    }

    public Position findKing(Color color) {
        for (Piece piece : getPiecesByColor(color)) {
            if (piece.getType() == PieceType.KING) {
                return piece.getPosition();
            }
        }
        throw new IllegalStateException("No king on board for " + color);
    }

    public boolean isSquareAttacked(Position target, Color byColor) {
        for (Piece piece : getPiecesByColor(byColor)) {
            if (piece.getPossibleMoves(this).contains(target)) {
                return true;
            }
        }
        return false;
    }

    /** True if moving {@code piece} to {@code dest} would not leave the mover's own king in check. */
    public boolean isMoveLegal(Piece piece, Position dest) {
        Board simulated = copy();
        Piece movingCopy = simulated.getPiece(piece.getPosition());
        simulated.removePiece(piece.getPosition());
        simulated.removePiece(dest);
        movingCopy.setPosition(dest);
        simulated.placePiece(movingCopy, dest);
        Position kingPos = simulated.findKing(piece.getColor());
        return !simulated.isSquareAttacked(kingPos, piece.getColor().opposite());
    }

    public boolean hasAnyLegalMove(Color color) {
        for (Piece piece : getPiecesByColor(color)) {
            for (Position dest : piece.getPossibleMoves(this)) {
                if (isMoveLegal(piece, dest)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Board copy() {
        Board copy = new Board();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                if (piece != null) {
                    copy.grid[row][col] = piece.copy();
                }
            }
        }
        return copy;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int row = 7; row >= 0; row--) {
            sb.append(row + 1).append(" ");
            for (int col = 0; col < 8; col++) {
                Piece piece = grid[row][col];
                sb.append(piece == null ? '.' : piece.getSymbol()).append(' ');
            }
            sb.append('\n');
        }
        sb.append("  a b c d e f g h");
        return sb.toString();
    }
}
