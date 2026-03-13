package org.example;

import java.util.Objects;

/**
 * <p> A fixed-width row in the terminal grid (an array of {@link Cell}).
 *
 * <p> Width is set at construction time and never changes. All column indices must be in {@code [0, width)}.
 */
public final class TerminalLine {
    private final Cell[] cells;
    private final int width;

    /**
     * Creates a blank line of the given width with all cells empty.
     */
    public TerminalLine(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive, got: " + width);
        }

        this.width = width;
        this.cells = new Cell[width];

        for (int i = 0; i < width; i++) {
            cells[i] = new Cell();
        }
    }

    /**
     * Private constructor used by {@link #copy}.
     */
    private TerminalLine(Cell[] cells, int width) {
        this.cells = cells;
        this.width = width;
    }

    /**
     * Returns the cell at {@code column}. Changes to the returned cell affect the cell in this line.
     */
    public Cell getCell(int column) {
        checkColumn(column);

        return cells[column];
    }

    /**
     * Overwrites the cell at {@code column} with the given character and attributes.
     */
    public void setCell(int column, char character, CellAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must ne not null");
        checkColumn(column);

        cells[column].set(character, attributes);
    }

    /**
     * Resets the cell at {@code column} to empty.
     */
    public void clearCell(int column) {
        checkColumn(column);

        cells[column].setEmpty();
    }

    /**
     * Fills every cell in this line with {@code character} and the given attributes.
     */
    public void fill(char character, CellAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must ne not null");

        for (Cell cell : cells) {
            cell.set(character, attributes);
        }
    }

    /**
     * Resets every cell in this line to empty.
     */
    public void clear() {
        for (Cell cell : cells) {
            cell.setEmpty();
        }
    }

    /**
     * Inserts {@code character} at {@code column}, shifting all cells from {@code column}
     * rightward by one.
     *
     * @return a copy of the rightmost cell that was pushed off the line,
     */
    // todo: think also about inserting many cells at once
    public Cell insertCellAt(int column, char character, CellAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must ne not null");
        checkColumn(column);

        Cell displaced = cells[width - 1].copy();

        for (int i = width - 1; i > column; i--) {
            Cell src = cells[i - 1];

            if (src.isEmpty()) {
                cells[i].setEmpty();
            } else {
                cells[i].set(src.getCharacter(), src.getAttributes());
            }
        }

        cells[column].set(character, attributes);

        return displaced;
    }

    /**
     * Returns a copy of this line. The copy's state is independent of the origin line state.
     */
    public TerminalLine copy() {
        Cell[] copiedCells = new Cell[width];

        for (int i = 0; i < width; i++) {
            copiedCells[i] = cells[i].copy();
        }

        return new TerminalLine(copiedCells, width);
    }

    /**
     * Ensures that the given {@code column} is within the valid range.
     */
    private void checkColumn(int column) {
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException("Column " + column + " out of bounds for width " + width);
        }
    }

    /**
     * Returns the line content as a string of exactly {@code width} characters.
     * Empty cells are rendered as spaces; trailing spaces are preserved.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width);

        for (Cell cell : cells) {
            sb.append(cell.isEmpty() ? ' ' : cell.getCharacter());
        }

        return sb.toString();
    }
}
