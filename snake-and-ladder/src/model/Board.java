package model;

import java.util.Collections;
import java.util.Map;

/** Immutable; only ever constructed via builder.BoardBuilder. */
public class Board {
    private final int size;
    private final Map<Integer, Integer> jumps;

    public Board(int size, Map<Integer, Integer> jumps) {
        this.size = size;
        this.jumps = Collections.unmodifiableMap(jumps);
    }

    public int getSize() {
        return size;
    }

    /** cell -> destination, for both snake heads and ladder bottoms alike. */
    public Map<Integer, Integer> getJumps() {
        return jumps;
    }
}
