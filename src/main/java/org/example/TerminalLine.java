package org.example;

public final class TerminalLine {
    private final Cell[] cells;
    private final int width;

    public TerminalLine(int width) {
        this.width = width;
        this.cells = new Cell[width];

        for (int i = 0; i < width; i++) {
            cells[i] = new Cell();
        }
    }

    private TerminalLine(Cell[] cells, int width) {
        this.cells = cells;
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public Cell getCell(int col) {
        checkCol(col);
        return cells[col];
    }

    public void setCell(int col, char character, CellAttributes attributes) {
        checkCol(col);
        cells[col].set(character, attributes);
    }

    public void clearCell(int col) {
        checkCol(col);
        cells[col].setEmpty();
    }

    public void fill(char character, CellAttributes attributes) {
        for (Cell cell : cells) {
            cell.set(character, attributes);
        }
    }

    public void clear() {
        for (Cell cell : cells) {
            cell.setEmpty();
        }
    }

    private void checkCol(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException("Column " + col + " out of bounds for width " + width);
        }
    }
}
