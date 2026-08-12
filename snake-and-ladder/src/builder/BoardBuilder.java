package builder;

import exceptions.InvalidBoardConfigException;
import java.util.HashMap;
import java.util.Map;
import model.Board;

/** Fluent Builder: Board has no public constructor, only this. */
public class BoardBuilder {
    private final int size;
    private final Map<Integer, Integer> jumps = new HashMap<>();

    public BoardBuilder(int size) {
        this.size = size;
    }

    public BoardBuilder addSnake(int head, int tail) {
        validateCell(head);
        validateCell(tail);
        if (tail >= head) {
            throw new InvalidBoardConfigException("Snake tail (" + tail + ") must be below its head (" + head + ")");
        }
        addJump(head, tail);
        return this;
    }

    public BoardBuilder addLadder(int bottom, int top) {
        validateCell(bottom);
        validateCell(top);
        if (top <= bottom) {
            throw new InvalidBoardConfigException("Ladder top (" + top + ") must be above its bottom (" + bottom + ")");
        }
        addJump(bottom, top);
        return this;
    }

    private void addJump(int from, int to) {
        if (jumps.containsKey(from)) {
            throw new InvalidBoardConfigException("Cell " + from + " already has a snake or ladder starting on it");
        }
        jumps.put(from, to);
    }

    private void validateCell(int cell) {
        if (cell <= 1 || cell >= size) {
            throw new InvalidBoardConfigException("Cell " + cell + " must be strictly between 1 and " + size);
        }
    }

    public Board build() {
        return new Board(size, jumps);
    }
}
