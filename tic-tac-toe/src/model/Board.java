package model;

public class Board {
    private final int size;
    private final Mark[][] grid;
    private int filledCount = 0;

    public Board(int size) {
        this.size = size;
        this.grid = new Mark[size][size];
        for (Mark[] row : grid) {
            java.util.Arrays.fill(row, Mark.EMPTY);
        }
    }

    public int getSize() {
        return size;
    }

    public Mark get(int row, int col) {
        return grid[row][col];
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    public boolean isEmpty(int row, int col) {
        return grid[row][col] == Mark.EMPTY;
    }

    public void place(int row, int col, Mark mark) {
        grid[row][col] = mark;
        filledCount++;
    }

    public boolean isFull() {
        return filledCount == size * size;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Mark m = grid[r][c];
                sb.append(m == Mark.EMPTY ? '.' : m.name().charAt(0));
                if (c < size - 1) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }
}
