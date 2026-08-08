package model;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color, Position position) {
        super(color, position);
    }

    @Override
    public PieceType getType() {
        return PieceType.PAWN;
    }

    @Override
    public List<Position> getPossibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int direction = color == Color.WHITE ? 1 : -1;
        int row = position.getRow();
        int col = position.getCol();

        Position oneForward = new Position(row + direction, col);
        if (oneForward.isOnBoard() && board.getPiece(oneForward) == null) {
            moves.add(oneForward);
            Position twoForward = new Position(row + 2 * direction, col);
            if (!hasMoved && twoForward.isOnBoard() && board.getPiece(twoForward) == null) {
                moves.add(twoForward);
            }
        }

        for (int dCol : new int[] {-1, 1}) {
            Position diagonal = new Position(row + direction, col + dCol);
            if (diagonal.isOnBoard()) {
                Piece target = board.getPiece(diagonal);
                if (target != null && target.getColor() != color) {
                    moves.add(diagonal);
                }
            }
        }
        return moves;
    }

    @Override
    public Piece copy() {
        Pawn copy = new Pawn(color, position);
        copy.setHasMoved(hasMoved);
        return copy;
    }
}
