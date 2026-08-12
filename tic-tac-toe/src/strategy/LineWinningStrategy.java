package strategy;

import model.Board;
import model.Mark;

/**
 * Checks only the row, column, and (if applicable) both diagonals that
 * pass through the just-played cell -- O(size), not a full O(size^2) board
 * scan -- since a move can only ever complete a line it's actually on.
 */
public class LineWinningStrategy implements WinningStrategy {
    @Override
    public boolean checkWinner(Board board, int row, int col, Mark mark) {
        int size = board.getSize();

        boolean rowWin = true, colWin = true;
        for (int i = 0; i < size; i++) {
            if (board.get(row, i) != mark) {
                rowWin = false;
            }
            if (board.get(i, col) != mark) {
                colWin = false;
            }
        }
        if (rowWin || colWin) {
            return true;
        }

        if (row == col) {
            boolean diagWin = true;
            for (int i = 0; i < size; i++) {
                if (board.get(i, i) != mark) {
                    diagWin = false;
                    break;
                }
            }
            if (diagWin) {
                return true;
            }
        }

        if (row + col == size - 1) {
            boolean antiDiagWin = true;
            for (int i = 0; i < size; i++) {
                if (board.get(i, size - 1 - i) != mark) {
                    antiDiagWin = false;
                    break;
                }
            }
            if (antiDiagWin) {
                return true;
            }
        }

        return false;
    }
}
