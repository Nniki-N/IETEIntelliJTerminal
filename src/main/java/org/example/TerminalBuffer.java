package org.example;

import java.util.*;

public final class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    private final List<TerminalLine> screen;
    private final List<TerminalLine> scrollback;

    private int cursorColumn;
    private int cursorRow;

    private CellAttributes currentAttributes;

    public TerminalBuffer(int width, int height, int maxScrollback) {
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

    public TerminalBuffer() {
        this(80, 24, 1000);
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

    public void setCursorColumn(int col) {
        cursorColumn = col;
    }

    public void setCursorRow(int row) {
        cursorRow = row;
    }

    public void setCursor(int col, int row) {
        cursorColumn = Math.max(0, Math.min(width - 1, col));
        cursorRow = Math.max(0, Math.min(height - 1, row));
    }

    public void setForeground(TerminalColor foregroundColor) {
        currentAttributes = new CellAttributes(
                foregroundColor,
                currentAttributes.getBackgroundColor(),
                currentAttributes.getStyles()
        );
    }

    public void setBackground(TerminalColor backgroundColor) {
        currentAttributes = new CellAttributes(
                currentAttributes.getForegroundColor(),
                backgroundColor,
                currentAttributes.getStyles()
        );
    }

    public void setStyles(TextStyle... styles) {
        currentAttributes = new CellAttributes(
                currentAttributes.getForegroundColor(),
                currentAttributes.getBackgroundColor(),
                new HashSet<>(Arrays.asList(styles))
        );
    }

    public void moveCursorUp(int n) {
        cursorRow = Math.max(0, cursorRow - n);
    }

    public void moveCursorDown(int n) {
        cursorRow = Math.min(height - 1, cursorRow + n);
    }

    public void moveCursorLeft(int n) {
        cursorColumn = Math.max(0, cursorColumn - n);
    }

    public void moveCursorRight(int n) {
        cursorColumn = Math.min(width - 1, cursorColumn + n);
    }

    public void writeText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        for (char ch : text.toCharArray()) {
            screen.get(cursorRow).setCell(cursorColumn, ch, currentAttributes);
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

        for (char ch : text.toCharArray()) {
            int insertRow = cursorRow;
            TerminalLine line = screen.get(insertRow);
            Cell displaced = line.getCell(width - 1).copy();

            line.insertCellAt(cursorColumn, ch, currentAttributes);
            cursorColumn++;

            boolean scrolledForDisplaced = false;

            if (!displaced.isEmpty()) {
                if (insertRow >= height - 1) {
                    insertEmptyLineAtBottom();
                    scrolledForDisplaced = true;
                }
                int targetRow = scrolledForDisplaced ? height - 1 : insertRow + 1;
                screen.get(targetRow).insertCellAt(
                        0, displaced.getCharacter(), displaced.getAttributes());
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

    public void fillLine(int row, char ch) {
        checkScreenRow(row);
        screen.get(row).fill(ch, currentAttributes);
    }

    public void clearLine(int row) {
        checkScreenRow(row);
        screen.get(row).clear();
    }

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

    public void clearScreen() {
        for (TerminalLine line : screen) {
            line.clear();
        }

        cursorColumn = 0;
        cursorRow = 0;
    }

    public void clearScreenAndScrollback() {
        clearScreen();
        scrollback.clear();
    }

    public char getCharAt(int col, int row) {
        Cell cell = resolveCell(col, row);

        return cell.isEmpty() ? ' ' : cell.getCharacter();
    }

    public CellAttributes getAttributesAt(int col, int row) {
        return resolveCell(col, row).getAttributes();
    }


    public String getLineAsString(int row) {
        return resolveLine(row).toString();
    }

    public String getScreenAsString() {
        StringBuilder sb = new StringBuilder(height * (width + 1));

        for (TerminalLine line : screen) {
            sb.append(line.toString()).append('\n');
        }

        return sb.toString();
    }

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

    private Cell resolveCell(int col, int row) {
        checkRowCell(col);

        return resolveLine(row).getCell(col);
    }

    private TerminalLine resolveLine(int row) {
        if (row >= 0) {
            checkScreenRow(row);

            return screen.get(row);
        } else {
            int scrollbackSize = scrollback.size();
            int index = scrollbackSize + row;

            if (index < 0 || index >= scrollbackSize) {
                throw new IndexOutOfBoundsException("Scrollback row " + row + " out of bounds (scrollback size=" + scrollbackSize + ")");
            }

            return scrollback.get(index);
        }
    }

    private void checkRowCell(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException("Screen col " + col + " out of bounds (width=" + width + ")");
        }
    }

    private void checkScreenRow(int row) {
        if (row < 0 || row >= height) {
            throw new IndexOutOfBoundsException("Screen row " + row + " out of bounds (height=" + height + ")");
        }
    }
}
