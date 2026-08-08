package model;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public Knight(Color color, Position position) {
        super(color, position);
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }

    @Override
    public List<Position> getPossibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        for (int[] offset : OFFSETS) {
            Position dest = new Position(position.getRow() + offset[0], position.getCol() + offset[1]);
            if (!dest.isOnBoard()) {
                continue;
            }
            Piece target = board.getPiece(dest);
            if (target == null || target.getColor() != color) {
                moves.add(dest);
            }
        }
        return moves;
    }

    @Override
    public Piece copy() {
        Knight copy = new Knight(color, position);
        copy.setHasMoved(hasMoved);
        return copy;
    }
}
