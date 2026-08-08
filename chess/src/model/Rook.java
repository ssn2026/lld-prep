package model;

import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public Rook(Color color, Position position) {
        super(color, position);
    }

    @Override
    public PieceType getType() {
        return PieceType.ROOK;
    }

    @Override
    public List<Position> getPossibleMoves(Board board) {
        return SlidingPieceSupport.slide(board, position, color, DIRECTIONS);
    }

    @Override
    public Piece copy() {
        Rook copy = new Rook(color, position);
        copy.setHasMoved(hasMoved);
        return copy;
    }
}
