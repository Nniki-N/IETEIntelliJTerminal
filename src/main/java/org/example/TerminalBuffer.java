package org.example;

import java.util.*;

public final class TerminalBuffer {
    private final int width;
    private int height;
    private final int maxScrollback;

    private final List<TerminalLine> screen;
    private final Deque<TerminalLine> scrollback;

    private int cursorCol;
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

        this.scrollback = new ArrayDeque<>();
        this.cursorCol = 0;
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

    public Deque<TerminalLine> getScrollback() {
        return scrollback;
    }

    public int getCursorCol() {
        return cursorCol;
    }

    public int getCursorRow() {
        return cursorRow;
    }

    public CellAttributes getCurrentAttributes() {
        return currentAttributes;
    }

    public void setCursor(int col, int row) {
        cursorCol = Math.max(0, Math.min(width - 1, col));
        cursorRow = Math.max(0, Math.min(height - 1, row));
    }

    public void setStyles(TextStyle... styles) {
        currentAttributes = new CellAttributes(
                currentAttributes.getForeground(),
                currentAttributes.getBackground(),
                new HashSet<>(Arrays.asList(styles))
        );
    }
}
