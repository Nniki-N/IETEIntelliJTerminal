import org.example.CellAttributes;
import org.example.TerminalColor;
import org.example.TerminalLine;
import org.example.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminalLine")
class TerminalLineTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("all cells are empty with default attributes")
        void testAllCellsStartEmpty() {
            int width = 5;
            TerminalLine line = new TerminalLine(width);

            for (int i = 0; i < width; i++) {
                assertTrue(line.getCell(i).isEmpty());
                assertEquals(CellAttributes.DEFAULT, line.getCell(i).getAttributes());
            }
        }
    }

    @Nested
    @DisplayName("setCell() / getCell()")
    class SetGetCell {

        @Test
        @DisplayName("setCell() sets passed character and attributes at the column")
        void testSetCellSavesCharacterCorrectly() {
            TerminalLine line = new TerminalLine(5);
            Set<TextStyle> styles = new HashSet<>();
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.DEFAULT, styles);

            line.setCell(2, 'X', cellAttributes);

            assertEquals('X', line.getCell(2).getCharacter());
            assertFalse(line.getCell(2).isEmpty());
            assertEquals(TerminalColor.RED, line.getCell(2).getAttributes().getForegroundColor());
        }

        @Test
        @DisplayName("setCell() only affects the targeted column")
        void testSetCellDoesNotAffectOtherColumns() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(2, 'X', CellAttributes.DEFAULT);

            assertTrue(line.getCell(0).isEmpty());
            assertTrue(line.getCell(1).isEmpty());
            assertTrue(line.getCell(3).isEmpty());
            assertTrue(line.getCell(4).isEmpty());
        }

        @Test
        @DisplayName("getCell() at negative column throws IndexOutOfBoundsException")
        void testGetCellAtNegativeColumnThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> new TerminalLine(5).getCell(-1));
        }

        @Test
        @DisplayName("getCell() at column equal or higher than width throws IndexOutOfBoundsException")
        void testGetCellAtColumnHigherOrEqualThanWidthThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> new TerminalLine(5).getCell(5));
            assertThrows(IndexOutOfBoundsException.class, () -> new TerminalLine(5).getCell(6));
        }

        @Test
        @DisplayName("setCell() at negative column throws IndexOutOfBoundsException")
        void testSetCellAtNegativeColumnThrows() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> new TerminalLine(5).setCell(-1, 'A', CellAttributes.DEFAULT)
            );
        }

        @Test
        @DisplayName("setCell() at column equal or higher than width throws IndexOutOfBoundsException")
        void testSetCellAtColumnHigherOrEqualThanWidthThrows() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> new TerminalLine(5).setCell(5, 'A', CellAttributes.DEFAULT)
            );
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> new TerminalLine(5).setCell(6, 'A', CellAttributes.DEFAULT)
            );
        }
    }

    @Nested
    @DisplayName("clearCell()")
    class ClearCell {

        @Test
        @DisplayName("clearCell() resets a filled cell at column to empty")
        void testClearEmptyCellResets() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(1, 'A', CellAttributes.DEFAULT);
            line.clearCell(1);

            assertTrue(line.getCell(1).isEmpty());
        }

        @Test
        @DisplayName("emptyCell() does not affect adjacent cells")
        void testClearCellDoesNotAffectNeighbours() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.clearCell(1);

            assertEquals('A', line.getCell(0).getCharacter());
            assertEquals('C', line.getCell(2).getCharacter());
        }
    }

    @Nested
    @DisplayName("fill() / fill()")
    class FillAndClear {

        @Test
        @DisplayName("fill() sets to every cell")
        void testFillAllWithCharacterChangesEveryCell() {
            TerminalLine line = new TerminalLine(4);
            Set<TextStyle> styles = new HashSet<>();
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.YELLOW, TerminalColor.DEFAULT, styles);

            line.fill('-', cellAttributes);

            for (int i = 0; i < 4; i++) {
                assertEquals('-', line.getCell(i).getCharacter());
                assertFalse(line.getCell(i).isEmpty());
                assertEquals(TerminalColor.YELLOW, line.getCell(i).getAttributes().getForegroundColor());
            }
        }

        @Test
        @DisplayName("clear() resets every cell to empty")
        void testClearLineResetsAllCells() {
            TerminalLine line = new TerminalLine(4);
            line.fill('X', CellAttributes.DEFAULT);
            line.clear();

            for (int i = 0; i < 4; i++) {
                assertTrue(line.getCell(i).isEmpty());
            }
        }

        @Test
        @DisplayName("clear() on an already-empty line does not throw")
        void testClearLineOnEmptyDoesNotThrow() {
            TerminalLine line = new TerminalLine(3);

            assertDoesNotThrow(line::clear);

            for (int i = 0; i < 3; i++) {
                assertTrue(line.getCell(i).isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("insertCellAt()")
    class InsertCellAt {

        @Test
        @DisplayName("inserting at column 0 shifts all existing cells right by one")
        void testInsertAtZeroShiftsAll() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);
            line.insertCellAt(0, 'E', CellAttributes.DEFAULT);

            assertEquals('E', line.getCell(0).getCharacter());
            assertEquals('A', line.getCell(1).getCharacter());
            assertEquals('B', line.getCell(2).getCharacter());
            assertEquals('C', line.getCell(3).getCharacter());
            assertEquals('D', line.getCell(4).getCharacter());
        }

        @Test
        @DisplayName("inserting in the middle shifts cells from that column rightward")
        void testInsertInMiddleShiftsRightward() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);
            line.insertCellAt(2, 'E', CellAttributes.DEFAULT);

            assertEquals('A', line.getCell(0).getCharacter());
            assertEquals('B', line.getCell(1).getCharacter());
            assertEquals('E', line.getCell(2).getCharacter());
            assertEquals('C', line.getCell(3).getCharacter());
            assertEquals('D', line.getCell(4).getCharacter());
        }

        @Test
        @DisplayName("inserting when the line is full discards the rightmost cell")
        void testInsertDiscardsRightmostWhenFull() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);
            line.insertCellAt(0, 'E', CellAttributes.DEFAULT);

            assertEquals('E', line.getCell(0).getCharacter());
            assertEquals('A', line.getCell(1).getCharacter());
            assertEquals('B', line.getCell(2).getCharacter());
            assertEquals('C', line.getCell(3).getCharacter());
        }

        @Test
        @DisplayName("empty flags remain when shifting cells")
        void testInsertKeepsEmptyFlags() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.insertCellAt(0, 'E', CellAttributes.DEFAULT);

            assertEquals('E', line.getCell(0).getCharacter());
            assertEquals('A', line.getCell(1).getCharacter());
            assertTrue(line.getCell(2).isEmpty());
            assertTrue(line.getCell(3).isEmpty());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToContentString {

        @Test
        @DisplayName("returns a string of exactly width characters")
        void testReturnsFullWidth() {
            TerminalLine line = new TerminalLine(6);
            line.setCell(0, 'H', CellAttributes.DEFAULT);
            line.setCell(1, 'i', CellAttributes.DEFAULT);
            String s = line.toString();

            assertEquals(6, s.length());
        }

        @Test
        @DisplayName("empty cells are returned as spaces")
        void testEmptyCellsAreSpaces() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);

            assertEquals("A   ", line.toString());
        }

        @Test
        @DisplayName("fully empty line is all spaces")
        void testEmptyLineIsAllSpaces() {
            TerminalLine line = new TerminalLine(3);

            assertEquals("   ", line.toString());
        }
    }

    @Nested
    @DisplayName("copy()")
    class CopyLine {

        @Test
        @DisplayName("copy contains the same characters as the original")
        void testCopySameContent() {
            TerminalLine line = new TerminalLine(2);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            TerminalLine copy = line.copy();

            assertEquals('A', copy.getCell(0).getCharacter());
            assertEquals('B', copy.getCell(1).getCharacter());
        }

        @Test
        @DisplayName("changes the copy does not affect the original")
        void testCopyIsIndependent() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);

            TerminalLine copy = line.copy();
            copy.setCell(0, 'B', CellAttributes.DEFAULT);

            assertEquals('A', line.getCell(0).getCharacter());
            assertEquals('B', copy.getCell(0).getCharacter());
        }

        @Test
        @DisplayName("empty cells in the original remain empty in the copy")
        void testCopyKeepsEmptyCells() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);

            TerminalLine copy = line.copy();

            assertTrue(copy.getCell(1).isEmpty());
            assertTrue(copy.getCell(2).isEmpty());
            assertTrue(copy.getCell(3).isEmpty());
        }
    }
}
