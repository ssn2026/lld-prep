package observer;

import model.GameStatus;
import model.Mark;

public interface GameListener {
    void onMove(Mark mark, int row, int col);

    void onGameOver(GameStatus result);
}
