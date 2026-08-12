package observer;

import model.MoveReason;

public interface GameListener {
    void onPositionChanged(String playerName, int from, int to, MoveReason reason);

    void onGameWon(String playerName);
}
