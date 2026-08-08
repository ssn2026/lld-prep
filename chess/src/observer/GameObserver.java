package observer;

import model.Color;
import model.GameStatus;
import model.Move;

public interface GameObserver {

    void onMove(Move move);

    void onCheck(Color colorInCheck);

    void onGameOver(GameStatus finalStatus, Color winner);
}
