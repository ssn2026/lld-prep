package model;

import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRECTIONS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    public Bishop(Color color, Position position) {
        super(color, position);
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }

    @Override
    public List<Position> getPossibleMoves(Board board) {
        return SlidingPieceSupport.slide(board, position, color, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Bishop copy = new Bishop(color, position);
        copy.setHasMoved(hasMoved);
        return copy;
    }
}
