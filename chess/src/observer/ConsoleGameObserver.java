package observer;

import model.Color;
import model.GameStatus;
import model.Move;

public class ConsoleGameObserver implements GameObserver {

    @Override
    public void onMove(Move move) {
        System.out.println("  [event] move played: " + move);
    }

    @Override
    public void onCheck(Color colorInCheck) {
        System.out.println("  [event] " + colorInCheck + " is in CHECK");
    }

    @Override
    public void onGameOver(GameStatus finalStatus, Color winner) {
        if (finalStatus == GameStatus.CHECKMATE) {
            System.out.println("  [event] CHECKMATE - " + winner + " wins");
        } else {
            System.out.println("  [event] STALEMATE - draw");
        }
    }
}
