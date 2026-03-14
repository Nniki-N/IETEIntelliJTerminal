package org.example;

import java.util.Objects;

/**
 * A fixed-width row in the terminal grid (an array of {@link Cell}).
 *
 * <p> Width is set at construction time and never changes. All column indices must be in {@code [0, width)}.
 *
 * <p> A line knows whether it ended because content automatically wrapped ({@link #isSoftWrapped()} {@code == true})
 * or because the terminal explicitly moved to the next line ({@code false}).
 * This flag is used by resize to group physical lines into logical lines.
 */
public final class TerminalLine {
    private final Cell[] cells;
    private final int width;

    /**
     * True when this line ended because content reached the right edge and wrapped automatically.
     */
    private boolean softWrapped;

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

        this.softWrapped = false;
    }

    /**
     * Private constructor used by {@link #copy}.
     */
    private TerminalLine(Cell[] cells, int width, boolean softWrapped) {
        this.cells = cells;
        this.width = width;
        this.softWrapped = softWrapped;
    }

    public int getWidth() {
        return width;
    }

    public boolean isSoftWrapped() {
        return softWrapped;
    }

    public void setSoftWrapped(boolean softWrapped) {
        this.softWrapped = softWrapped;
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
     *
     * <p> If the column is part of a wide character pair, the pair is cleared first so no
     * orphaned wide or continuation cell is left behind.
     */
    public void setCell(int column, char character, CellAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must ne not null");
        checkColumn(column);

        clearWidePairAt(column);
        cells[column].set(character, attributes);
    }

    /**
     * Resets the cell at {@code column} to empty.
     *
     * <p> If the column is part of a wide character pair, clears any pair it belongs to.
     */
    public void clearCell(int column) {
        checkColumn(column);

        clearWidePairAt(column);
        cells[column].setEmpty();
    }

    /**
     * Writes a wide character at {@code column}, taking two cells.
     * The left cell holds the character, while the right cell becomes a continuation placeholder.
     *
     * @throws IllegalArgumentException if {@code column + 1 >= width}. The caller must ensure
     *                                  that 2 columns are available before calling this.
     */
    public void setWideCell(int column, char character, CellAttributes attributes) {
        Objects.requireNonNull(attributes, "attributes must be not null");
        checkColumn(column);

        if (column + 1 >= width) {
            throw new IllegalArgumentException("Wide character at column " + column + " does not fit in width " + width);
        }

        clearWidePairAt(column);
        cells[column].set(character, attributes);
        clearWidePairAt(column + 1);
        cells[column + 1].setWideContinuation();
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
     * Inserts as many cells from {@code toInsert} as fit at {@code column} and
     * returns a single array of everything that did not make it onto this line:
     * {@code [overflow | displaced]}, where:
     * <ul>
     *   <li>overflow is the tail of {@code toInsert} that exceeded the line.</li>
     *   <li>displaced is cells that were pushed off the right edge by the shift.</li>
     * </ul>
     *
     * <p> Wide pair integrity: if the shift moves a wide char to {@code width - 1} while
     * its continuation is displaced as the first displaced cell, the orphaned wide char
     * is moved to the front of the returned array, and the cell at {@code width - 1} is
     * cleared.
     *
     * @param toInsert cells to insert starting at {@code column}. may be longer than
     *                 the space available, everything that excess becomes overflow
     * @return overflow + displaced, in this order.
     */
    public Cell[] insertCellsAt(int column, Cell[] toInsert) {
        Objects.requireNonNull(toInsert, "toInsert must not be null");
        checkColumn(column);

        if (toInsert.length == 0) {
            return new Cell[0];
        }

        int available = width - column;
        int fit = Math.min(toInsert.length, available);

        if (fit < toInsert.length && toInsert[fit].isWideContinuation()) {
            fit--;
        }

        int overflow = toInsert.length - fit;

        Cell[] displacedCopies = new Cell[fit];

        for (int i = 0; i < fit; i++) {
            displacedCopies[i] = cells[width - fit + i].copy();
        }

        // Shift cells[column ... width - fit - 1] rightward by fit positions
        for (int i = width - 1; i >= column + fit; i--) {
            Cell src = cells[i - fit];

            if (src.isEmpty()) {
                cells[i].setEmpty();
            } else if (src.isWideContinuation()) {
                cells[i].setWideContinuation();
            } else {
                cells[i].set(src.getCharacter(), src.getAttributes());
            }
        }

        // Write the fitting cells at [column ... column + fit - 1]
        for (int i = 0; i < fit; i++) {
            Cell src = toInsert[i];

            if (src.isEmpty()) {
                cells[column + i].setEmpty();
            } else if (src.isWideContinuation()) {
                cells[column + i].setWideContinuation();
            } else {
                cells[column + i].set(src.getCharacter(), src.getAttributes());
            }
        }

        boolean splitWide = fit > 0
                && displacedCopies[0].isWideContinuation()
                && !cells[width - 1].isEmpty()
                && !cells[width - 1].isWideContinuation();

        // Wide character was split case: set the last character to empty
        if (splitWide) {
            Cell orphan = cells[width - 1].copy();
            cells[width - 1].setEmpty();

            // Length: [orphan, wideContinuation, overflow cells, displaced]
            int resultLength = 1 + 1 + overflow + (fit - 1);

            Cell[] result = new Cell[resultLength];
            result[0] = orphan;
            result[1] = new Cell();
            result[1].setWideContinuation();

            System.arraycopy(toInsert, fit, result, 2, overflow);

            System.arraycopy(displacedCopies, 1, result, 2 + overflow, fit - 1);

            return result;
        }

        if (overflow == 0 && fit == 0) {
            return new Cell[0];
        }

        // Normal case: [overflow cells, displaced]
        Cell[] result = new Cell[overflow + fit];

        System.arraycopy(toInsert, fit, result, 0, overflow);
        System.arraycopy(displacedCopies, 0, result, overflow, fit);

        return result;
    }

    /**
     * Returns a copy of this line. The copy's state is independent of the origin line state.
     */
    public TerminalLine copy() {
        Cell[] copiedCells = new Cell[width];

        for (int i = 0; i < width; i++) {
            copiedCells[i] = cells[i].copy();
        }

        return new TerminalLine(copiedCells, width, softWrapped);
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
     * Clears a wide character pair so that overwriting one half does not leave an orphan.
     *
     * <ul>
     *   <li>If {@code column} is a continuation cell, the left half (column-1) is emptied.</li>
     *   <li>If the cell to the right of {@code column} is a continuation, it is emptied.</li>
     * </ul>
     */
    private void clearWidePairAt(int column) {
        if (column > 0 && cells[column].isWideContinuation()) {
            cells[column - 1].setEmpty();
        }

        if (column + 1 < width && cells[column + 1].isWideContinuation()) {
            cells[column + 1].setEmpty();
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
            sb.append(cell.isEmpty() || cell.isWideContinuation() ? ' ' : cell.getCharacter());
        }

        return sb.toString();
    }
}
