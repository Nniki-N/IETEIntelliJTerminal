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

    public void insertCellAt(int col, char ch, CellAttributes attributes) {
        checkCol(col);

        for (int i = width - 1; i > col; i--) {
            Cell src = cells[i - 1];

            if (src.isEmpty()) {
                cells[i].setEmpty();
            } else {
                cells[i].set(src.getCharacter(), src.getAttributes());
            }
        }

        cells[col].set(ch, attributes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width);

        for (Cell cell : cells) {
            sb.append(cell.isEmpty() ? ' ' : cell.getCharacter());
        }

        return sb.toString();
    }

    public TerminalLine copy() {
        Cell[] copiedCells = new Cell[width];

        for (int i = 0; i < width; i++) {
            copiedCells[i] = cells[i].copy();
        }

        return new TerminalLine(copiedCells, width);
    }
    private void checkCol(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException("Column " + col + " out of bounds for width " + width);
        }
    }
}
