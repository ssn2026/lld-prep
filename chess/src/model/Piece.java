package model;

import java.util.List;

/**
 * Each subclass implements only its own movement geometry and reports
 * pseudo-legal moves (board-boundary and same-color-occupancy aware, but
 * blind to whether the move would leave its own king in check). Filtering
 * pseudo-legal moves down to fully legal ones is Board's job (isMoveLegal),
 * since that requires simulating the move on a whole-board copy.
 */
public abstract class Piece {

    protected final Color color;
    protected Position position;
    protected boolean hasMoved;

    protected Piece(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.hasMoved = false;
    }

    public abstract PieceType getType();

    public abstract List<Position> getPossibleMoves(Board board);

    public Color getColor() {
        return color;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public abstract Piece copy();

    public char getSymbol() {
        char base;
        switch (getType()) {
            case PAWN: base = 'p'; break;
            case KNIGHT: base = 'n'; break;
            case BISHOP: base = 'b'; break;
            case ROOK: base = 'r'; break;
            case QUEEN: base = 'q'; break;
            case KING: base = 'k'; break;
            default: base = '?';
        }
        return color == Color.WHITE ? Character.toUpperCase(base) : base;
    }
}
