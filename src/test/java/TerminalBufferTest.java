import org.example.CellAttributes;
import org.example.TerminalBuffer;
import org.example.TerminalColor;
import org.example.TextStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminalBuffer")
class TerminalBufferTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("default constructor creates 80x24 screen with 1000-line scrollback")
        void testDefaultConstructor() {
            TerminalBuffer buf = new TerminalBuffer();

            assertEquals(80, buf.getWidth());
            assertEquals(24, buf.getHeight());
            assertEquals(1000, buf.getMaxScrollback());
        }

        @Test
        @DisplayName("parametric constructor stores all three dimensions")
        void testParametricConstructor() {
            TerminalBuffer buf = new TerminalBuffer(40, 10, 500);

            assertEquals(40, buf.getWidth());
            assertEquals(10, buf.getHeight());
            assertEquals(500, buf.getMaxScrollback());
        }

        @Test
        @DisplayName("cursor starts at (0, 0)")
        void testCursorStartsAtOrigin() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);

            assertEquals(0, buf.getCursorColumn());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("all screen cells start empty")
        void testScreenStartsEmpty() {
            int width = 5;
            int height = 3;

            TerminalBuffer buf = new TerminalBuffer(width, height, 10);

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    assertEquals(' ', buf.getCharacterAt(col, row));
                    assertEquals(CellAttributes.DEFAULT, buf.getAttributesAt(col, row));
                }
            }
        }

        @Test
        @DisplayName("screen has exactly height rows")
        void testScreenHasCorrectNumberOfRows() {
            int height = 7;
            TerminalBuffer buf = new TerminalBuffer(5, height, 10);

            assertEquals(height, buf.getScreen().size());
        }

        @Test
        @DisplayName("scrollback starts empty")
        void testScrollbackStartsEmpty() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);

            assertEquals(0, buf.getScrollback().size());
        }

        @Test
        @DisplayName("initial attributes are DEFAULT")
        void testInitialAttributesAreDefault() {
            TerminalBuffer buf = new TerminalBuffer(10, 5, 100);

            assertEquals(CellAttributes.DEFAULT, buf.getCurrentAttributes());
        }
    }

    @Nested
    @DisplayName("CurrentAttributes")
    class CurrentAttributes {
        private TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Test
        @DisplayName("setForeground changes only the foreground color")
        void testSetForeground() {
            buf.setForeground(TerminalColor.RED);

            assertEquals(TerminalColor.RED, buf.getCurrentAttributes().foregroundColor());
            assertEquals(TerminalColor.DEFAULT, buf.getCurrentAttributes().backgroundColor());
        }

        @Test
        @DisplayName("setBackground changes only the background color")
        void testSetBackground() {
            buf.setBackground(TerminalColor.BLUE);

            assertEquals(TerminalColor.BLUE, buf.getCurrentAttributes().backgroundColor());
            assertEquals(TerminalColor.DEFAULT, buf.getCurrentAttributes().foregroundColor());
        }

        @Test
        @DisplayName("setStyles replaces the full style set")
        void testSetStylesReplacesAll() {
            buf.setStyles(TextStyle.BOLD, TextStyle.UNDERLINE);

            assertTrue(buf.getCurrentAttributes().styles().contains(TextStyle.BOLD));
            assertTrue(buf.getCurrentAttributes().styles().contains(TextStyle.UNDERLINE));
            assertFalse(buf.getCurrentAttributes().styles().contains(TextStyle.ITALIC));
        }

        @Test
        @DisplayName("calling setStyles with no arguments clears all styles")
        void testSetStylesWithNoArgumentsClears() {
            buf.setStyles(TextStyle.BOLD);
            buf.setStyles();

            assertTrue(buf.getCurrentAttributes().styles().isEmpty());
        }

        @Test
        @DisplayName("current attributes at write time are passed into the cell")
        void testCurrentAttributesPassedIntoCell() {
            buf.setForeground(TerminalColor.MAGENTA);
            buf.setStyles(TextStyle.BOLD);
            buf.writeText("A");

            CellAttributes cellAttributes = buf.getAttributesAt(0, 0);

            assertEquals(TerminalColor.MAGENTA, cellAttributes.foregroundColor());
            assertTrue(cellAttributes.styles().contains(TextStyle.BOLD));
        }

        @Test
        @DisplayName("changing attributes after a write does not affect already written cells")
        void testCurrentAttributesChangeDoesNotAffectPast() {
            buf.setForeground(TerminalColor.RED);
            buf.writeText("X");
            buf.setForeground(TerminalColor.BLUE);

            assertEquals(TerminalColor.RED, buf.getAttributesAt(0, 0).foregroundColor());
        }
    }

    @Nested
    @DisplayName("Cursor")
    class Cursor {
        private TerminalBuffer buf;
        private final int width = 10;
        private final int height = 5;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(width, height, 100);
        }

        @Test
        @DisplayName("setCursor positions the cursor exactly")
        void testSetCursorExact() {
            buf.setCursor(3, 2);

            assertEquals(3, buf.getCursorColumn());
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        @DisplayName("setCursor with negative column sets column to 0")
        void testSetCursorNegativeCol() {
            buf.setCursor(-5, 0);

            assertEquals(0, buf.getCursorColumn());
        }

        @Test
        @DisplayName("setCursor with column beyond width-1 sets column to width-1")
        void setCursorOverflowColumn() {
            buf.setCursor(999, 0);

            assertEquals(width - 1, buf.getCursorColumn());
        }

        @Test
        @DisplayName("setCursor with negative row sets row to 0")
        void testSetCursorNegativeRow() {
            buf.setCursor(0, -3);

            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("setCursor clamps row beyond height-1 to height-1")
        void testSetCursorOverflowRow() {
            buf.setCursor(0, 999);

            assertEquals(height - 1, buf.getCursorRow());
        }

        @Test
        @DisplayName("setCursorColumn and setCursorRow set positions independently")
        void testSetCursorColumnRowIndependent() {
            buf.setCursorColumn(7);
            buf.setCursorRow(3);

            assertEquals(7, buf.getCursorColumn());
            assertEquals(3, buf.getCursorRow());
        }

        @Test
        @DisplayName("moveCursorUp by N rows leads to row index decrease")
        void testMoveCursorUp() {
            buf.setCursor(0, 4);
            buf.moveCursorUp(2);

            assertEquals(2, buf.getCursorRow());
        }

        @Test
        @DisplayName("moveCursorUp stops at row 0")
        void testMoveCursorUpOverflow() {
            buf.setCursor(0, 1);
            buf.moveCursorUp(100);

            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("moveCursorDown by N rows leads to row index increase")
        void testMoveCursorDown() {
            buf.setCursor(0, 0);
            buf.moveCursorDown(3);

            assertEquals(3, buf.getCursorRow());
        }

        @Test
        @DisplayName("moveCursorDown stops at height-1")
        void testMoveCursorDownOverflow() {
            buf.setCursor(0, 3);
            buf.moveCursorDown(100);

            assertEquals(height - 1, buf.getCursorRow());
        }

        @Test
        @DisplayName("moveCursorLeft by N columns leads to column index decrease")
        void testMoveCursorLeft() {
            buf.setCursor(6, 0);
            buf.moveCursorLeft(4);

            assertEquals(2, buf.getCursorColumn());
        }

        @Test
        @DisplayName("moveCursorLeft stops at column 0")
        void testMoveCursorLeftClamps() {
            buf.setCursor(2, 0);
            buf.moveCursorLeft(100);

            assertEquals(0, buf.getCursorColumn());
        }

        @Test
        @DisplayName("moveCursorRight by N columns leads to column index increase")
        void testMoveCursorRight() {
            buf.setCursor(0, 0);
            buf.moveCursorRight(5);

            assertEquals(5, buf.getCursorColumn());
        }

        @Test
        @DisplayName("moveCursorRight stops at width-1")
        void testMoveCursorRightClamps() {
            buf.setCursor(7, 0);
            buf.moveCursorRight(100);

            assertEquals(width - 1, buf.getCursorColumn());
        }
    }

    @Nested
    @DisplayName("writeText()")
    class WriteText {
        private TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Test
        @DisplayName("writes characters starting at the cursor position")
        void testWriteFromCursorPosition() {
            buf.setCursor(2, 1);
            buf.writeText("Hello");

            assertEquals('H', buf.getCharacterAt(2, 1));
            assertEquals('e', buf.getCharacterAt(3, 1));
            assertEquals('l', buf.getCharacterAt(4, 1));
            assertEquals('l', buf.getCharacterAt(5, 1));
            assertEquals('o', buf.getCharacterAt(6, 1));
        }

        @Test
        @DisplayName("cursor moves past the last written character")
        void testCursorMovesAfterWrite() {
            buf.setCursor(0, 0);
            buf.writeText("Hello");

            assertEquals(5, buf.getCursorColumn());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("overwrites existing content in place and does not shift")
        void testWriteOverwritesExistingContent() {
            buf.writeText("AAAAAAAAAA");
            buf.setCursor(3, 0);
            buf.writeText("BCD");

            assertEquals('A', buf.getCharacterAt(2, 0));
            assertEquals('B', buf.getCharacterAt(3, 0));
            assertEquals('C', buf.getCharacterAt(4, 0));
            assertEquals('D', buf.getCharacterAt(5, 0));
            assertEquals('A', buf.getCharacterAt(6, 0));
        }

        @Test
        @DisplayName("text wraps to column 0 of the next row when reaching the right edge")
        void testWrapAtRightEdge() {
            buf.setCursor(8, 0);
            buf.writeText("ABCD");

            assertEquals('A', buf.getCharacterAt(8, 0));
            assertEquals('B', buf.getCharacterAt(9, 0));
            assertEquals('C', buf.getCharacterAt(0, 1));
            assertEquals('D', buf.getCharacterAt(1, 1));
        }

        @Test
        @DisplayName("after wrapping the cursor is on the next row")
        void testCursorOnNextRowAfterWrap() {
            buf.setCursor(9, 0);
            buf.writeText("AB");

            assertEquals(1, buf.getCursorColumn());
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        @DisplayName("writing on the last row scrolls the screen and preserves the cursor row")
        void testScrollsWhenWritingOnLastRow() {
            for (int r = 0; r < 5; r++) {
                buf.fillLine(r, (char) ('A' + r));
            }

            buf.setCursor(9, 4);
            buf.writeText("XY");

            assertEquals(1, buf.getScrollback().size());
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        @DisplayName("passing null as text does nothing")
        void testWriteNullDoesNothing() {
            buf.setCursor(3, 2);

            assertDoesNotThrow(() -> buf.writeText(null));
            assertEquals(3, buf.getCursorColumn());
        }

        @Test
        @DisplayName("passing empty string does nothing")
        void testWriteEmptyStringDoesNothing() {
            buf.setCursor(3, 2);
            buf.writeText("");

            assertEquals(3, buf.getCursorColumn());
            assertEquals(2, buf.getCursorRow());
        }
    }

    @Nested
    @DisplayName("insertText()")
    class InsertText {

        private TerminalBuffer buf;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(10, 5, 100);
        }

        @Test
        @DisplayName("shifts existing content to the right")
        void testShiftContentRight() {
            buf.writeText("AAAA");
            buf.setCursor(1, 0);
            buf.insertText("BC");

            assertEquals('A', buf.getCharacterAt(0, 0));
            assertEquals('B', buf.getCharacterAt(1, 0));
            assertEquals('C', buf.getCharacterAt(2, 0));
            assertEquals('A', buf.getCharacterAt(3, 0));
            assertEquals('A', buf.getCharacterAt(4, 0));
        }

        @Test
        @DisplayName("cursor ends after the last inserted character")
        void testCursorMovesForward() {
            buf.setCursor(2, 0);
            buf.insertText("AB");

            assertEquals(4, buf.getCursorColumn());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("overflow wraps to the next row all displaced cells")
        void testDisplacedCellsWrapToNextRow() {
            buf.fillLine(0, 'X');
            buf.setCursor(0, 0);
            buf.insertText("A");

            assertEquals('A', buf.getCharacterAt(0, 0));
            assertEquals('X', buf.getCharacterAt(0, 1));
        }

        @Test
        @DisplayName("scrolls the screen when wrapping on the last row")
        void testScrollWhenWrappingOnLastRow() {
            buf.fillLine(4, 'Z');
            buf.setCursor(0, 4);
            buf.insertText("W");

            assertFalse(buf.getScrollback().isEmpty());
            assertTrue(buf.getCursorRow() < buf.getHeight());
        }

        @Test
        @DisplayName("passing null as text does nothing")
        void testInsertNullDoesNothing() {
            buf.setCursor(1, 0);

            assertDoesNotThrow(() -> buf.insertText(null));
            assertEquals(1, buf.getCursorColumn());
        }

        @Test
        @DisplayName("passing empty string does nothing")
        void testEmptyStringDoesNothing() {
            buf.setCursor(1, 0);
            buf.insertText("");

            assertEquals(1, buf.getCursorColumn());
        }
    }

    @Nested
    @DisplayName("fillLine() / clearLine()")
    class FillAndEmpty {
        private TerminalBuffer buf;
        private final int height = 3;
        private final int width = 5;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(width, height, 10);
        }

        @Test
        @DisplayName("fillLine() sets passed character to every cell in the row")
        void testFillLineEveryCell() {
            buf.fillLine(1, '*');

            for (int col = 0; col < width; col++) {
                assertEquals('*', buf.getCharacterAt(col, 1));
            }
        }

        @Test
        @DisplayName("fillLine() uses the current attributes")
        void testFillLineUsesCurrentAttributes() {
            buf.setForeground(TerminalColor.YELLOW);
            buf.fillLine(0, '-');

            for (int col = 0; col < width; col++) {
                assertEquals(TerminalColor.YELLOW, buf.getAttributesAt(col, 0).foregroundColor());
                assertEquals(TerminalColor.DEFAULT, buf.getAttributesAt(col, 0).backgroundColor());
                assertTrue(buf.getAttributesAt(col, 0).styles().isEmpty());
            }
        }

        @Test
        @DisplayName("fillLine() does not affect other rows")
        void testFillLineDoesNotAffectOtherRows() {
            buf.fillLine(1, 'X');

            for (int col = 0; col < width; col++) {
                assertEquals(' ', buf.getCharacterAt(col, 0));
                assertEquals('X', buf.getCharacterAt(col, 1));
                assertEquals(' ', buf.getCharacterAt(col, 2));
            }
        }

        @Test
        @DisplayName("fillLine() with negative row throws IndexOutOfBoundsException")
        void testFillLineNegativeRowThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.fillLine(-1, 'X'));
        }

        @Test
        @DisplayName("fillLine() at height throws IndexOutOfBoundsException")
        void testFillLineAtHeightThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.fillLine(height, 'X'));
        }

        @Test
        @DisplayName("clearLine() clears every cell in the row")
        void testClearLineClearsEveryCell() {
            buf.fillLine(2, 'Z');
            buf.clearLine(2);

            for (int col = 0; col < width; col++) {
                assertEquals(' ', buf.getCharacterAt(col, 2));
                assertEquals(CellAttributes.DEFAULT, buf.getAttributesAt(col, 2));
            }
        }

        @Test
        @DisplayName("clearLine() does not affect other rows")
        void testClearLineDoesNotAffectOtherRows() {
            buf.fillLine(0, 'A');
            buf.fillLine(1, 'B');
            buf.clearLine(0);

            assertEquals('B', buf.getCharacterAt(0, 1));
        }
    }

    @Nested
    @DisplayName("insertEmptyLineAtBottom()")
    class InsertEmptyLine {
        private TerminalBuffer buf;
        private final int width = 5;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(width, 3, 10);
        }

        @Test
        @DisplayName("top screen line moves to scrollback at index -1")
        void testTopLinePushedToScrollback() {
            buf.fillLine(0, 'A');
            buf.insertEmptyLineAtBottom();

            assertEquals(1, buf.getScrollback().size());
            assertEquals("AAAAA", buf.getLineAsString(-1));
        }

        @Test
        @DisplayName("remaining screen rows shift up by one")
        void testScreenRowsShiftUp() {
            buf.fillLine(0, 'A');
            buf.fillLine(1, 'B');
            buf.fillLine(2, 'C');
            buf.insertEmptyLineAtBottom();

            assertEquals('B', buf.getCharacterAt(0, 0));
            assertEquals('C', buf.getCharacterAt(0, 1));
        }

        @Test
        @DisplayName("new empty row appears at the bottom")
        void testNewRowAtBottomIsEmpty() {
            buf.insertEmptyLineAtBottom();

            for (int col = 0; col < width; col++) {
                assertEquals(' ', buf.getCharacterAt(col, 2));
            }
        }

        @Test
        @DisplayName("screen always stays at exactly height rows")
        void testScreenAlwaysHasHeightRows() {
            for (int i = 0; i < 20; i++) {
                buf.insertEmptyLineAtBottom();
            }

            assertEquals(3, buf.getScreen().size());
        }

        @Test
        @DisplayName("scrollback grows up to maxScrollback then stops")
        void testScrollbackDoesNotGrowPastMaxScrollback() {
            TerminalBuffer b = new TerminalBuffer(5, 3, 5);

            for (int i = 0; i < 20; i++) {
                b.insertEmptyLineAtBottom();
            }

            assertEquals(5, b.getScrollback().size());
        }

        @Test
        @DisplayName("oldest scrollback line is removed when the limit is reached")
        void testOldestLineRemoved() {
            TerminalBuffer b = new TerminalBuffer(5, 2, 2);

            b.fillLine(0, '1');
            b.insertEmptyLineAtBottom();
            b.fillLine(0, '2');
            b.insertEmptyLineAtBottom();
            b.fillLine(0, '3');
            b.insertEmptyLineAtBottom();

            assertEquals(2, b.getScrollback().size());
            assertEquals("22222", b.getLineAsString(-2));
            assertEquals("33333", b.getLineAsString(-1));
        }

        @Test
        @DisplayName("no scrollback is kept when maxScrollback is 0")
        void testNoScrollbackWhenDisabled() {
            TerminalBuffer b = new TerminalBuffer(5, 3, 0);
            b.insertEmptyLineAtBottom();

            assertEquals(0, b.getScrollback().size());
        }
    }

    @Nested
    @DisplayName("clearScreen() / clearScreenAndScrollback()")
    class ClearOperations {
        private TerminalBuffer buf;
        private final int height = 3;
        private final int width = 5;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(width, height, 10);
        }

        @Test
        @DisplayName("clearScreen() clears all screen cells")
        void testClearScreenClearsAllCells() {
            buf.fillLine(0, 'A');
            buf.fillLine(1, 'B');
            buf.fillLine(2, 'C');
            buf.clearScreen();

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    assertEquals(' ', buf.getCharacterAt(col, row));
                    assertEquals(CellAttributes.DEFAULT, buf.getAttributesAt(col, row));
                }
            }
        }

        @Test
        @DisplayName("clearScreen() resets moves to (0, 0)")
        void testClearScreenResetsCursor() {
            buf.setCursor(3, 2);
            buf.clearScreen();

            assertEquals(0, buf.getCursorColumn());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        @DisplayName("clearScreen() does not touch scrollback")
        void testClearScreenPreservesScrollback() {
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.clearScreen();

            assertEquals(2, buf.getScrollback().size());
        }

        @Test
        @DisplayName("clearScreenAndScrollback() removes all scrollback lines")
        void testClearScreenAndScrollbackRemovesScrollback() {
            buf.insertEmptyLineAtBottom();
            buf.insertEmptyLineAtBottom();
            buf.clearScreenAndScrollback();

            assertEquals(0, buf.getScrollback().size());
        }

        @Test
        @DisplayName("clearScreenAndScrollback() also clears screen cells")
        void testClearScreenAndScrollbackEmptiesScreen() {
            buf.fillLine(0, 'X');
            buf.clearScreenAndScrollback();

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    assertEquals(' ', buf.getCharacterAt(col, row));
                    assertEquals(CellAttributes.DEFAULT, buf.getAttributesAt(col, row));
                }
            }
        }
    }

    @Nested
    @DisplayName("Content Access")
    class ContentAccess {
        private TerminalBuffer buf;
        private final int height = 3;
        private final int width = 5;

        @BeforeEach
        void setUp() {
            buf = new TerminalBuffer(width, height, 20);
        }

        @Test
        @DisplayName("getCharacterAt returns space for an empty screen cell")
        void testGetCharAtEmptyCell() {
            assertEquals(' ', buf.getCharacterAt(0, 0));
        }

        @Test
        @DisplayName("getCharacterAt returns the written character")
        void testGetCharAtFilledCell() {
            buf.writeText("Z");

            assertEquals('Z', buf.getCharacterAt(0, 0));
        }

        @Test
        @DisplayName("getCharacterAt with negative column throws")
        void testGetCharAtNegativeColThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharacterAt(-1, 0));
        }

        @Test
        @DisplayName("getCharacterAt with column >= width throws")
        void testGetCharAtOverflowColumnThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharacterAt(width, 0));
        }

        @Test
        @DisplayName("getCharacterAt with row >= height throws")
        void testGetCharAtOverflowRowThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharacterAt(0, height));
        }

        @Test
        @DisplayName("getCharacterAt with row=-1 returns the most recent scrollback line")
        void testGetCharAtScrollbackMostRecent() {
            buf.fillLine(0, 'S');
            buf.insertEmptyLineAtBottom();

            assertEquals('S', buf.getCharacterAt(0, -1));
        }

        @Test
        @DisplayName("getCharacterAt with row=-2 accesses the second most recent scrollback line")
        void testGetCharAtScrollbackOlderLine() {
            buf.fillLine(0, 'A');
            buf.insertEmptyLineAtBottom();
            buf.fillLine(0, 'B');
            buf.insertEmptyLineAtBottom();

            assertEquals('A', buf.getCharacterAt(0, -2));
            assertEquals('B', buf.getCharacterAt(0, -1));
        }

        @Test
        @DisplayName("getCharacterAt with out-of-bounds scrollback row throws")
        void testGetCharAtScrollbackOutOfBoundsThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> buf.getCharacterAt(0, -1));
        }

        @Test
        @DisplayName("getAttributesAt returns attributes from screen correctly")
        void testGetAttributesAtScreen() {
            buf.setForeground(TerminalColor.GREEN);
            buf.writeText("G");

            assertEquals(TerminalColor.GREEN, buf.getAttributesAt(0, 0).foregroundColor());
        }

        @Test
        @DisplayName("getAttributesAt returns attributes from scrollback correctly")
        void testGetAttributesAtScrollback() {
            buf.setForeground(TerminalColor.MAGENTA);
            buf.fillLine(0, 'M');
            buf.insertEmptyLineAtBottom();

            assertEquals(TerminalColor.MAGENTA, buf.getAttributesAt(0, -1).foregroundColor());
        }

        @Test
        @DisplayName("getLineAsString returns a string of exactly width characters")
        void testGetLineAsStringIsFullWidth() {
            buf.writeText("Hi");
            String line = buf.getLineAsString(0);

            assertEquals(width, line.length());
            assertEquals("Hi   ", line);
        }

        @Test
        @DisplayName("getLineAsString accesses scrollback with a negative row")
        void testGetLineAsStringScrollback() {
            buf.fillLine(0, 'X');
            buf.insertEmptyLineAtBottom();

            assertEquals("XXXXX", buf.getLineAsString(-1));
        }

        @Test
        @DisplayName("getScreenAsString contains height lines each ending in newline")
        void testGetScreenAsStringFormat() {
            buf.fillLine(0, 'A');
            buf.fillLine(1, 'B');
            buf.fillLine(2, 'C');

            String s = buf.getScreenAsString();
            String[] lines = s.split("\n");

            assertEquals(3, lines.length);
            assertEquals("AAAAA", lines[0]);
            assertEquals("BBBBB", lines[1]);
            assertEquals("CCCCC", lines[2]);
        }

        @Test
        @DisplayName("getAllContentAsString includes scrollback before screen (oldest first)")
        void testGetAllContentAsStringOrder() {
            buf.fillLine(0, '1');
            buf.insertEmptyLineAtBottom();
            buf.fillLine(0, '2');

            String all = buf.getAllContentAsString();
            String[] lines = all.split("\n");

            assertTrue(lines[0].startsWith("1"), "First line should be the oldest scrollback line");
            assertTrue(lines[1].startsWith("2"), "Second line should be the current screen row 0");
        }
    }
}
