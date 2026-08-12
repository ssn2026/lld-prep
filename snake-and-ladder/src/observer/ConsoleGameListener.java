package observer;

import model.MoveReason;

public class ConsoleGameListener implements GameListener {
    @Override
    public void onPositionChanged(String playerName, int from, int to, MoveReason reason) {
        System.out.println("[listener] " + playerName + ": " + from + " -> " + to + " (" + reason + ")");
    }

    @Override
    public void onGameWon(String playerName) {
        System.out.println("[listener] " + playerName + " WINS!");
    }
}
