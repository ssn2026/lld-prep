package model;

import java.util.List;

public class Queen extends Piece {

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    public Queen(Color color, Position position) {
        super(color, position);
    }

    @Override
    public PieceType getType() {
        return PieceType.QUEEN;
    }

    @Override
    public List<Position> getPossibleMoves(Board board) {
        return SlidingPieceSupport.slide(board, position, color, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Queen copy = new Queen(color, position);
        copy.setHasMoved(hasMoved);
        return copy;
    }
}
