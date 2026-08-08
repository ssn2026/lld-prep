package model;

import java.util.Objects;

/**
 * row 0 = rank 1 (White's back rank), row 7 = rank 8 (Black's back rank);
 * col 0 = file 'a', col 7 = file 'h'.
 */
public final class Position {

    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public static Position of(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        char fileChar = Character.toLowerCase(algebraic.charAt(0));
        char rankChar = algebraic.charAt(1);
        int col = fileChar - 'a';
        int row = rankChar - '1';
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        return new Position(row, col);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isOnBoard() {
        return row >= 0 && row <= 7 && col >= 0 && col <= 7;
    }

    public String toAlgebraic() {
        return "" + (char) ('a' + col) + (char) ('1' + row);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
