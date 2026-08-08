package model;

import java.util.ArrayList;
import java.util.List;

/** Shared ray-casting move generation for Rook/Bishop/Queen. Not a Piece itself. */
final class SlidingPieceSupport {

    private SlidingPieceSupport() {
    }

    static List<Position> slide(Board board, Position from, Color color, int[][] directions) {
        List<Position> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int row = from.getRow() + dir[0];
            int col = from.getCol() + dir[1];
            while (row >= 0 && row <= 7 && col >= 0 && col <= 7) {
                Position dest = new Position(row, col);
                Piece target = board.getPiece(dest);
                if (target == null) {
                    moves.add(dest);
                } else {
                    if (target.getColor() != color) {
                        moves.add(dest);
                    }
                    break;
                }
                row += dir[0];
                col += dir[1];
            }
        }
        return moves;
    }
}
