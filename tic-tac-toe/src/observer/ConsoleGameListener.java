package observer;

import model.GameStatus;
import model.Mark;

public class ConsoleGameListener implements GameListener {
    @Override
    public void onMove(Mark mark, int row, int col) {
        System.out.println("[listener] " + mark + " played (" + row + "," + col + ")");
    }

    @Override
    public void onGameOver(GameStatus result) {
        System.out.println("[listener] game over: " + result);
    }
}
