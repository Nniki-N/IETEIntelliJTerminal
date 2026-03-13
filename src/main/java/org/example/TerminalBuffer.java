package org.example;

import java.util.*;

public final class TerminalBuffer {
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_HEIGHT = 24;
    private static final int DEFAULT_MAX_SCROLLBACK = 1000;

    private final int width;
    private final int height;
    private final int maxScrollback;

    /**
     * Visible lines, always contains exactly {@code height} elements.
     */
    private final List<TerminalLine> screen;

    /**
     * Scrollback history, oldest line first.
     */
    private final List<TerminalLine> scrollback;

    private int cursorColumn;
    private int cursorRow;

    /**
     * Attributes applied to all subsequent write/insert/fill operations.
     */
    private CellAttributes currentAttributes;

    /**
     * Creates a buffer with custom dimensions and scrollback limit.
     *
     * @param width         columns per line and must be positive
     * @param height        number of visible rows and must be positive
     * @param maxScrollback maximum lines kept in history. When set 0, disables scrollback
     */
    public TerminalBuffer(int width, int height, int maxScrollback) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (maxScrollback < 0) {
            throw new IllegalArgumentException("maxScrollback must be >= 0");
        }

        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;

        this.screen = new ArrayList<>(height);

        for (int i = 0; i < height; i++) {
            screen.add(new TerminalLine(width));
        }

        this.scrollback = new ArrayList<>();
        this.cursorColumn = 0;
        this.cursorRow = 0;
        this.currentAttributes = CellAttributes.DEFAULT;
    }

    /**
     * Creates a buffer with default dimensions (80×24, 1000-line scrollback).
     */
    public TerminalBuffer() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MAX_SCROLLBACK);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxScrollback() {
        return maxScrollback;
    }

    public List<TerminalLine> getScreen() {
        return screen;
    }

    public List<TerminalLine> getScrollback() {
        return scrollback;
    }

    public int getCursorColumn() {
        return cursorColumn;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public CellAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    /**
     * Returns the {@link CellAttributes} at ({@code column}, {@code row}).
     *
     * @param row screen row (&gt;= 0) or scrollback row (&lt; 0)
     */
    public CellAttributes getAttributesAt(int column, int row) {
        return resolveCell(column, row).getAttributes();
    }

    /**
     * Returns the character at ({@code column}, {@code row}).
     * Empty cells return {@code ' '}.
     *
     * @param row screen row (&gt;= 0) or scrollback row (&lt; 0; {@code -1} = most recent)
     */
    public char getCharacterAt(int column, int row) {
        Cell cell = resolveCell(column, row);

        return cell.isEmpty() ? ' ' : cell.getCharacter();
    }

    /**
     * Sets the cursor column directly.
     * Value is clamped to valid screen bounds.
     */
    public void setCursorColumn(int column) {
        cursorColumn = clamp(column, 0, width - 1);
    }

    /**
     * Sets the cursor row directly.
     * Value is clamped to valid screen bounds.
     */
    public void setCursorRow(int row) {
        cursorRow = clamp(row, 0, height - 1);
    }

    /**
     * Positions the cursor at ({@code column}, {@code row}).
     * Values are clamped to valid screen bounds.
     */
    public void setCursor(int column, int row) {
        cursorColumn = clamp(column, 0, width - 1);
        cursorRow = clamp(row, 0, height - 1);
    }

    /**
     * Sets only the foreground color for subsequent writes.
     */
    public void setForeground(TerminalColor foregroundColor) {
        currentAttributes = currentAttributes.withForeground(
                Objects.requireNonNull(foregroundColor, "foregroundColor must be not null")
        );
    }

    /**
     * Sets only the background color for subsequent writes.
     */
    public void setBackground(TerminalColor backgroundColor) {
        currentAttributes = currentAttributes.withBackground(
                Objects.requireNonNull(backgroundColor, "backgroundColor must be not null")
        );
    }

    /**
     * Replaces the active text style set; pass no arguments to clear all styles.
     */
    public void setStyles(TextStyle... styles) {
        EnumSet<TextStyle> stylesSet = styles.length == 0
                ? EnumSet.noneOf(TextStyle.class)
                : EnumSet.copyOf(Arrays.asList(styles));

        currentAttributes = currentAttributes.withStyles(stylesSet);
    }

    /**
     * Moves the cursor up by {@code n} rows; stops at row 0.
     */
    public void moveCursorUp(int n) {
        cursorRow = clamp(cursorRow - n, 0, height - 1);
    }

    /**
     * Moves the cursor down by {@code n} rows; stops at {@code height - 1}.
     */
    public void moveCursorDown(int n) {
        cursorRow = clamp(cursorRow + n, 0, height - 1);
    }

    /**
     * Moves the cursor left by {@code n} columns; stops at column 0.
     */
    public void moveCursorLeft(int n) {
        cursorColumn = clamp(cursorColumn - n, 0, width - 1);
    }

    /**
     * Moves the cursor right by {@code n} columns; stops at {@code width - 1}.
     */
    public void moveCursorRight(int n) {
        cursorColumn = clamp(cursorColumn + n, 0, width - 1);
    }

    /**
     * Writes {@code text} from the cursor position, overwriting existing content.
     * Wraps to the next row at the right edge and scrolls if already on the last row.
     */
    public void writeText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (char character : text.toCharArray()) {
            screen.get(cursorRow).setCell(cursorColumn, character, currentAttributes);
            cursorColumn++;

            if (cursorColumn >= width) {
                cursorColumn = 0;

                if (cursorRow < height - 1) {
                    cursorRow++;
                } else {
                    insertEmptyLineAtBottom();
                }
            }
        }
    }

    public void insertText(String text) {
        if (text == null || text.isEmpty()) return;

        for (char character : text.toCharArray()) {
            int insertRow = cursorRow;
            TerminalLine line = screen.get(insertRow);

            Cell displaced = line.insertCellAt(cursorColumn, character, currentAttributes);
            cursorColumn++;

            boolean scrolledForDisplaced = false;

            if (!displaced.isEmpty()) {
                if (insertRow >= height - 1) {
                    insertEmptyLineAtBottom();
                    scrolledForDisplaced = true;
                }
                int targetRow = scrolledForDisplaced ? height - 1 : insertRow + 1;
                screen.get(targetRow).insertCellAt(0, displaced.getCharacter(), displaced.getAttributes());
            }

            if (cursorColumn >= width) {
                cursorColumn = 0;
                if (scrolledForDisplaced) {
                    // skip
                } else if (cursorRow < height - 1) {
                    cursorRow++;
                } else {
                    insertEmptyLineAtBottom();
                }
            }
        }
    }

    /**
     * Fills every cell on {@code row} with {@code character} using the current attributes.
     * Does not move the cursor.
     */
    public void fillLine(int row, char character) {
        checkScreenRow(row);
        screen.get(row).fill(character, currentAttributes);
    }

    /**
     * Clears every cell on {@code row}.
     * Does not move the cursor.
     */
    public void clearLine(int row) {
        checkScreenRow(row);
        screen.get(row).clear();
    }

    /**
     * <p> Scrolls the screen up by one line. The top line moves to scrollback and new
     * empty line is appended at the bottom. Does not move the cursor.
     *
     * <p> If scrollback is full the oldest line is discarded.
     */
    public void insertEmptyLineAtBottom() {
        TerminalLine topLine = screen.removeFirst();

        if (maxScrollback > 0) {
            if (scrollback.size() >= maxScrollback) {
                scrollback.removeFirst();
            }

            scrollback.add(topLine.copy());
        }

        screen.add(new TerminalLine(width));
    }

    /**
     * Clears all screen cells and resets the cursor to (0, 0). Scrollback is not affected.
     */
    public void clearScreen() {
        for (TerminalLine line : screen) {
            line.clear();
        }

        cursorColumn = 0;
        cursorRow = 0;
    }

    /**
     * Clears the screen (see {@link #clearScreen()}) and discards all scrollback lines.
     */
    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    /**
     * Resolves a (column, row) pair to the {@link Cell}. Validates column first.
     */
    private Cell resolveCell(int column, int row) {
        checkRowCell(column);

        return resolveLine(row).getCell(column);
    }

    /**
     * Resolves a row to the corresponding {@link TerminalLine}.
     * Screen (row (&gt;= 0)); scrollback (row (&lt; 0)) where {@code -1} = most recent.
     */
    private TerminalLine resolveLine(int row) {
        if (row >= 0) {
            checkScreenRow(row);

            return screen.get(row);
        } else {
            int scrollbackSize = scrollback.size();
            int index = scrollbackSize + row;

            if (index < 0) {
                throw new IndexOutOfBoundsException("Scrollback row " + row + " out of bounds (scrollback size=" + scrollbackSize + ")");
            }

            return scrollback.get(index);
        }
    }

    /**
     * Ensures that the given {@code column} is within the valid range.
     */
    private void checkRowCell(int column) {
        if (column < 0 || column >= width) {
            throw new IndexOutOfBoundsException("Screen column " + column + " out of bounds (width=" + width + ")");
        }
    }

    /**
     * Ensures that the given {@code row} is within the valid range.
     */
    private void checkScreenRow(int row) {
        if (row < 0 || row >= height) {
            throw new IndexOutOfBoundsException("Screen row " + row + " out of bounds (height=" + height + ")");
        }
    }

    /**
     * Clamps {@code value} to [{@code min}, {@code max}].
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Returns the content of {@code row} as a string of exactly {@code width} characters.
     *
     * @param row screen row (&gt;= 0) or scrollback row (&lt; 0)
     */
    public String getLineAsString(int row) {
        return resolveLine(row).toString();
    }

    /**
     * Returns the entire screen as a multi-line string, each row terminated by {@code '\n'}.
     */
    public String getScreenAsString() {
        StringBuilder sb = new StringBuilder(height * (width + 1));

        for (TerminalLine line : screen) {
            sb.append(line.toString()).append('\n');
        }

        return sb.toString();
    }

    /**
     * Returns the full terminal history (at first scrollback, then screen) as a multi-line string,
     * each row terminated by {@code '\n'}.
     */
    public String getAllContentAsString() {
        StringBuilder sb = new StringBuilder();

        for (TerminalLine line : scrollback) {
            sb.append(line.toString()).append('\n');
        }

        for (TerminalLine line : screen) {
            sb.append(line.toString()).append('\n');
        }

        return sb.toString();
    }
}
