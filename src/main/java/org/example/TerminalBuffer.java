package org.example;

import java.util.*;

public final class TerminalBuffer {
    private static final int DEFAULT_WIDTH = 80;
    private static final int DEFAULT_MIN_WIDTH = 2;
    private static final int DEFAULT_HEIGHT = 24;
    private static final int DEFAULT_MAX_SCROLLBACK = 1000;

    private int width;
    private int height;
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
        if (width < DEFAULT_MIN_WIDTH) {
            throw new IllegalArgumentException("width must be >= " + DEFAULT_MIN_WIDTH);
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
     * Creates a buffer with default dimensions (80x24, 1000-line scrollback).
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

        return cell.isEmpty() || cell.isWideContinuation() ? ' ' : cell.getCharacter();
    }

    /**
     * Sets the cursor column directly. Value is clamped to valid screen bounds.
     */
    public void setCursorColumn(int column) {
        cursorColumn = clamp(column, 0, width - 1);
    }

    /**
     * Sets the cursor row directly. Value is clamped to valid screen bounds.
     */
    public void setCursorRow(int row) {
        cursorRow = clamp(row, 0, height - 1);
    }

    /**
     * Positions the cursor at ({@code column}, {@code row}). Values are clamped to valid screen bounds.
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
     * Replaces the active text style set. Pass no arguments to clear all styles.
     */
    public void setStyles(TextStyle... styles) {
        EnumSet<TextStyle> stylesSet = styles.length == 0
                ? EnumSet.noneOf(TextStyle.class)
                : EnumSet.copyOf(Arrays.asList(styles));

        currentAttributes = currentAttributes.withStyles(stylesSet);
    }

    /**
     * Moves the cursor up by {@code n} rows. Stops at row 0.
     */
    public void moveCursorUp(int n) {
        cursorRow = clamp(cursorRow - n, 0, height - 1);
    }

    /**
     * Moves the cursor down by {@code n} rows. Stops at {@code height - 1}.
     */
    public void moveCursorDown(int n) {
        cursorRow = clamp(cursorRow + n, 0, height - 1);
    }

    /**
     * Moves the cursor left by {@code n} columns. Stops at column 0.
     * Landing on a continuation cell steps back one more to reach the wide character start.
     */
    public void moveCursorLeft(int n) {
        for (int i = 0; i < n && cursorColumn > 0; i++) {
            cursorColumn--;

            if (cursorColumn > 0 && screen.get(cursorRow).getCell(cursorColumn).isWideContinuation()) {
                cursorColumn--;
            }
        }
    }

    /**
     * Moves the cursor right by {@code n} logical positions. Stops at {@code width - 1}.
     * Landing on a continuation cell steps forward one more to reach the wide character end.
     */
    public void moveCursorRight(int n) {
        for (int i = 0; i < n && cursorColumn < width - 1; i++) {
            if (!screen.get(cursorRow).getCell(cursorColumn).isWideContinuation()
                    && cursorColumn + 1 < width
                    && screen.get(cursorRow).getCell(cursorColumn + 1).isWideContinuation()) {
                cursorColumn += 2;
            } else {
                cursorColumn++;
            }

            cursorColumn = Math.min(cursorColumn, width - 1);
        }
    }

    /**
     * Writes {@code text} from the cursor position, overwriting existing content.
     *
     * <p> Wide characters (CJK, emoji) occupy 2 columns. If only 1 column remains on
     * the current row, the row is padded with a space and the wide character is
     * written at the start of the next row.
     *
     * <p> Lines that wrap automatically are marked soft-wrapped for resize reflow.
     */
    public void writeText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (isWideCharacter(codePoint)) {
                if (cursorColumn >= width - 1) {
                    screen.get(cursorRow).setSoftWrapped(true);
                    cursorColumn = 0;
                    advanceRowOrScroll();
                }

                screen.get(cursorRow).setWideCell(cursorColumn, (char) codePoint, currentAttributes);
                cursorColumn += 2;
            } else {
                screen.get(cursorRow).setCell(cursorColumn, (char) codePoint, currentAttributes);
                cursorColumn++;
            }

            if (cursorColumn >= width) {
                screen.get(cursorRow).setSoftWrapped(true);
                cursorColumn = 0;
                advanceRowOrScroll();
            }
        }
    }

    /**
     * Inserts {@code text} at the cursor position, shifting existing content rightward.
     *
     * <p> Each call to {@link TerminalLine#insertCellsAt} inserts as many cells as fit
     * and returns {@code [overflow | displaced]}. The loop continues until nothing remains.
     *
     * <p> Wide characters are never split: if one would start at the last column of a
     * row, it wraps to the next row (same rule as {@link #writeText}).
     *
     * <p> The cursor moves only for the original text.
     *
     * <p> Lines that wrap automatically are marked soft-wrapped for resize reflow.
     */
    public void insertText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        Cell[] pending = toCells(text, currentAttributes);

        int textColumns = pending.length;
        int textPlaced = 0;

        while (pending.length > 0) {
            Cell[] returned = screen.get(cursorRow).insertCellsAt(cursorColumn, pending);
            returned = trimTrailingEmpty(returned);

            int placed = pending.length - returned.length;
            int textPlacedNow = Math.min(placed, textColumns - textPlaced);

            cursorColumn += textPlacedNow;
            textPlaced += textPlacedNow;
            pending = returned;

            if (cursorColumn >= width || pending.length > 0) {
                screen.get(cursorRow).setSoftWrapped(true);
                cursorColumn = 0;
                advanceRowOrScroll();
            }
        }
    }

    /**
     * Fills every cell on {@code row} with {@code character} using the current attributes.
     * For wide characters, fills in (character, continuation) pairs; if the width is odd,
     * the trailing column is left empty.
     *
     * <p> Does not move the cursor.
     */
    public void fillLine(int row, char character) {
        checkScreenRow(row);
        TerminalLine line = screen.get(row);

        if (isWideCharacter(character)) {
            line.clear();

            for (int column = 0; column + 1 < width; column += 2) {
                line.setWideCell(column, character, currentAttributes);
            }
        } else {
            line.fill(character, currentAttributes);
        }

    }

    /**
     * Clears every cell on {@code row}.
     *
     * <p> Does not move the cursor.
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
     * Resizes the screen to {@code newWidth}x{@code newHeight}.
     *
     * <p> Height-only change: no reflow. Shrinking pushes top lines into scrollback.
     *
     * <p> Width change reflow: reflow. Lines are grouped into logical lines using
     * {@link TerminalLine#isSoftWrapped()} (a sequence of soft-wrapped lines followed by
     * one hard-ended line forms one logical line). Each logical line is flattened to
     * its trimmed cell sequence, then re-wrapped at the new width.
     *
     * <p> The cursor is clamped to the new bounds after resizing.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth < DEFAULT_MIN_WIDTH) {
            throw new IllegalArgumentException("newWidth must be >= " + DEFAULT_MIN_WIDTH);
        }

        if (newHeight <= 0) {
            throw new IllegalArgumentException("newHeight must be positive");
        }

        if (newWidth == width) {
            adjustHeight(newHeight);
        } else {
            List<TerminalLine> allLines = new ArrayList<>(scrollback.size() + height);
            allLines.addAll(scrollback);
            allLines.addAll(screen);

            List<List<TerminalLine>> logicalLines = groupIntoLogicalLines(allLines);
            List<TerminalLine> reflowed = new ArrayList<>();

            for (List<TerminalLine> logicalLine : logicalLines) {
                reflowed.addAll(wrapCells(flattenLogicalLine(logicalLine), newWidth));
            }

            while (reflowed.size() < newHeight) {
                reflowed.addFirst(new TerminalLine(newWidth));
            }

            int screenStart = reflowed.size() - newHeight;

            scrollback.clear();

            if (maxScrollback > 0) {
                int scrollbackStart = Math.max(0, screenStart - maxScrollback);

                for (int i = scrollbackStart; i < screenStart; i++) {
                    scrollback.add(reflowed.get(i));
                }
            }

            screen.clear();

            for (int i = screenStart; i < reflowed.size(); i++) {
                screen.add(reflowed.get(i));
            }

            width = newWidth;
        }

        height = newHeight;
        setCursorColumn(cursorColumn);
        setCursorRow(cursorRow);
    }

    /**
     * For height-only resize: push/pull lines between screen and scrollback.
     */
    private void adjustHeight(int newHeight) {
        int difference = newHeight - height;

        if (difference < 0) {
            for (int i = 0; i < -difference; i++) {
                TerminalLine top = screen.removeFirst();

                if (maxScrollback > 0) {
                    if (scrollback.size() >= maxScrollback) {
                        scrollback.removeFirst();
                    }

                    scrollback.add(top);
                }
            }
        } else {
            for (int i = 0; i < difference; i++) {
                if (!scrollback.isEmpty()) {
                    screen.addFirst(scrollback.removeLast());
                } else {
                    screen.addFirst(new TerminalLine(width));
                }
            }
        }
    }

    /**
     * Groups physical lines into logical lines.
     */
    private List<List<TerminalLine>> groupIntoLogicalLines(List<TerminalLine> physicalLines) {
        List<List<TerminalLine>> groups = new ArrayList<>();
        List<TerminalLine> currentLines = new ArrayList<>();

        for (TerminalLine line : physicalLines) {
            currentLines.add(line);

            if (!line.isSoftWrapped()) {
                groups.add(currentLines);
                currentLines = new ArrayList<>();
            }
        }

        if (!currentLines.isEmpty()) {
            groups.add(currentLines);
        }

        return groups;
    }

    /**
     * Flattens a logical line into a trimmed sequence of {@link CellEntry} values.
     * Continuation cells are skipped because wide characters are represented by a single
     * entry with {@code wide=true}. Trailing default-empty entries are trimmed.
     */
    private List<CellEntry> flattenLogicalLine(List<TerminalLine> logicalLine) {
        List<CellEntry> entries = new ArrayList<>();

        for (TerminalLine physicalLine : logicalLine) {
            for (int column = 0; column < physicalLine.getWidth(); column++) {
                Cell cell = physicalLine.getCell(column);

                if (cell.isWideContinuation()) {
                    continue;
                }

                boolean wide = column + 1 < physicalLine.getWidth() && physicalLine.getCell(column + 1).isWideContinuation();

                entries.add(
                        new CellEntry(
                                cell.isEmpty() ? ' ' : cell.getCharacter(),
                                cell.isEmpty() ? CellAttributes.DEFAULT : cell.getAttributes(),
                                wide
                        )
                );
            }
        }

        int last = entries.size() - 1;

        while (last >= 0) {
            CellEntry entry = entries.get(last);

            if (!entry.wide() && entry.character() == ' ' && entry.attributes() == CellAttributes.DEFAULT) {
                last--;
            } else {
                break;
            }
        }

        return new ArrayList<>(entries.subList(0, last + 1));
    }

    /**
     * Wraps a flat {@link CellEntry} sequence into physical lines at {@code newWidth}.
     * Wide characters that would start at the last column of a row go to the next row.
     */
    private List<TerminalLine> wrapCells(List<CellEntry> entries, int newWidth) {
        List<TerminalLine> lines = new ArrayList<>();
        TerminalLine currentLine = new TerminalLine(newWidth);
        int column = 0;

        for (CellEntry entry : entries) {
            if (entry.wide()) {
                if (column >= newWidth - 1) {
                    currentLine.setSoftWrapped(true);
                    lines.add(currentLine);

                    currentLine = new TerminalLine(newWidth);
                    column = 0;
                }

                currentLine.getCell(column).set(entry.character(), entry.attributes());
                currentLine.getCell(column + 1).setWideContinuation();
                column += 2;
            } else {
                if (column >= newWidth) {
                    currentLine.setSoftWrapped(true);
                    lines.add(currentLine);

                    currentLine = new TerminalLine(newWidth);
                    column = 0;
                }

                currentLine.getCell(column).set(entry.character(), entry.attributes());
                column++;
            }
        }

        currentLine.setSoftWrapped(false);
        lines.add(currentLine);

        return lines;
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
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Moves the cursor to the next row. If already on the last row, scrolls the screen up by one line instead.
     */
    private void advanceRowOrScroll() {
        if (cursorRow < height - 1) {
            cursorRow++;
        } else {
            insertEmptyLineAtBottom();
        }
    }

    /**
     * Converts {@code text} to a {@code Cell[]} where every cell maps to one column.
     * Wide characters produce two cells: (character cell, continuation cell).
     */
    private Cell[] toCells(String text, CellAttributes attributes) {
        int columns = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            columns += isWideCharacter(codePoint) ? 2 : 1;
        }

        Cell[] result = new Cell[columns];
        int idx = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            result[idx++] = new Cell((char) codePoint, attributes);

            if (isWideCharacter(codePoint)) {
                Cell wideContinuation = new Cell();
                wideContinuation.setWideContinuation();

                result[idx++] = wideContinuation;
            }
        }

        return result;
    }

    /**
     * Removes trailing {@link Cell#isEmpty() empty} cells.
     * Non-empty cells (including continuations) are never trimmed.
     */
    private Cell[] trimTrailingEmpty(Cell[] cells) {
        int last = cells.length - 1;

        while (last >= 0 && cells[last].isEmpty()) {
            last--;
        }

        if (last == cells.length - 1) {
            return cells;
        }

        if (last < 0) {
            return new Cell[0];
        }

        return Arrays.copyOf(cells, last + 1);
    }

    /**
     * Returns true if {@code codePoint} occupies 2 terminal columns when rendered.
     *
     * <p> Only CJK ideographs is used to show the point of detecting wide characters.
     * More code points to be added:
     * <a href="https://www.unicode.org/Public/UCD/latest/ucd/EastAsianWidth.txt">unicode</a>
     */
    private boolean isWideCharacter(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }

    /**
     * Carries character, attributes, and wide flag through the reflow pipeline.
     */
    private record CellEntry(char character, CellAttributes attributes, boolean wide) {
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
