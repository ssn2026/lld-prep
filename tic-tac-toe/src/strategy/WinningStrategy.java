package strategy;

import model.Board;
import model.Mark;

public interface WinningStrategy {
    boolean checkWinner(Board board, int row, int col, Mark mark);
}
