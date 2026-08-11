package model;

import java.util.Collections;
import java.util.List;

public class Team {
    private final String name;
    private final List<String> players;

    public Team(String name, List<String> players) {
        this.name = name;
        this.players = Collections.unmodifiableList(players);
    }

    public String getName() {
        return name;
    }

    public List<String> getPlayers() {
        return players;
    }
}
