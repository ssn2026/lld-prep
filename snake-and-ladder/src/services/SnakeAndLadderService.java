package services;

import exceptions.GameAlreadyWonException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Board;
import model.MoveReason;
import model.Player;
import observer.GameListener;
import strategy.DiceStrategy;

/**
 * The single public entry point. Owns the Board, every Player, whose turn
 * it is, and drives one roll at a time through the configured DiceStrategy.
 */
public class SnakeAndLadderService {
    private final Board board;
    private final List<Player> players;
    private final DiceStrategy diceStrategy;
    private final List<GameListener> listeners = new java.util.ArrayList<>();
    private int currentPlayerIndex = 0;
    private String winner;

    public SnakeAndLadderService(Board board, List<String> playerNames, DiceStrategy diceStrategy) {
        this.board = board;
        this.players = playerNames.stream().map(Player::new).toList();
        this.diceStrategy = diceStrategy;
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public int rollAndMove() {
        if (winner != null) {
            throw new GameAlreadyWonException(winner);
        }
        Player current = players.get(currentPlayerIndex);
        int roll = diceStrategy.roll();
        int from = current.getPosition();
        int tentative = from + roll;

        if (tentative > board.getSize()) {
            advanceTurn();
            return roll;
        }

        current.setPosition(tentative);
        notifyPositionChanged(current.getName(), from, tentative, MoveReason.DICE_ROLL);

        Integer jumpTo = board.getJumps().get(tentative);
        if (jumpTo != null) {
            MoveReason reason = jumpTo < tentative ? MoveReason.SNAKE : MoveReason.LADDER;
            current.setPosition(jumpTo);
            notifyPositionChanged(current.getName(), tentative, jumpTo, reason);
        }

        if (current.getPosition() == board.getSize()) {
            winner = current.getName();
            notifyGameWon(winner);
            return roll;
        }

        advanceTurn();
        return roll;
    }

    public Map<String, Integer> getPositions() {
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (Player player : players) {
            positions.put(player.getName(), player.getPosition());
        }
        return positions;
    }

    public String getWinner() {
        return winner;
    }

    public String getCurrentPlayerName() {
        return players.get(currentPlayerIndex).getName();
    }

    private void advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private void notifyPositionChanged(String playerName, int from, int to, MoveReason reason) {
        for (GameListener listener : listeners) {
            listener.onPositionChanged(playerName, from, to, reason);
        }
    }

    private void notifyGameWon(String playerName) {
        for (GameListener listener : listeners) {
            listener.onGameWon(playerName);
        }
    }
}
