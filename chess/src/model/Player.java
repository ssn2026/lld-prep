package model;

public class Player {

    private final String playerId;
    private final String name;
    private final Color color;

    public Player(String playerId, String name, Color color) {
        this.playerId = playerId;
        this.name = name;
        this.color = color;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }
}
