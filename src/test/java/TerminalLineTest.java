import org.example.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminalLine")
class TerminalLineTest {

    private static Cell[] narrowCells(String text) {
        Cell[] cells = new Cell[text.length()];

        for (int i = 0; i < text.length(); i++) {
            cells[i] = new Cell(text.charAt(i), CellAttributes.DEFAULT);
        }

        return cells;
    }

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

        @Test
        @DisplayName("softWrapped starts as false")
        void testSoftWrappedStartsFalse() {
            assertFalse(new TerminalLine(4).isSoftWrapped());
        }

        @Test
        @DisplayName("width=0 throws")
        void testZeroWidthThrows() {
            assertThrows(IllegalArgumentException.class, () -> new TerminalLine(0));
        }
    }

    @Nested
    @DisplayName("setCell() / getCell()")
    class SetGetCell {

        @Test
        @DisplayName("setCell() sets passed character and attributes at the column")
        void testSetCellSavesCharacterCorrectly() {
            TerminalLine line = new TerminalLine(5);
            EnumSet<TextStyle> styles = EnumSet.noneOf(TextStyle.class);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.DEFAULT, styles);

            line.setCell(2, 'X', cellAttributes);

            assertEquals('X', line.getCell(2).getCharacter());
            assertFalse(line.getCell(2).isEmpty());
            assertEquals(TerminalColor.RED, line.getCell(2).getAttributes().foregroundColor());
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
        @DisplayName("getCell() at out-of-bounds column throws IndexOutOfBoundsException")
        void testGetCellAtColumnHigherOrEqualThanWidthThrows() {
            assertThrows(IndexOutOfBoundsException.class, () -> new TerminalLine(5).getCell(5));
            assertThrows(IndexOutOfBoundsException.class, () -> new TerminalLine(5).getCell(-1));
        }

        @Test
        @DisplayName("setCell() at out-of-bounds column throws IndexOutOfBoundsException")
        void testSetCellAtNegativeColumnThrows() {
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> new TerminalLine(5).setCell(-1, 'A', CellAttributes.DEFAULT)
            );
            assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> new TerminalLine(5).setCell(5, 'A', CellAttributes.DEFAULT)
            );
        }

        @Test
        @DisplayName("setCell() on a wide char's left half clears the continuation")
        void testSetCellClearsContinuation() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);
            line.setCell(0, 'A', CellAttributes.DEFAULT);

            assertFalse(line.getCell(1).isWideContinuation());
            assertTrue(line.getCell(1).isEmpty());
        }

        @Test
        @DisplayName("setCell() on a continuation cell clears the wide char to its left")
        void testSetCellOnContinuationClearsLeftHalf() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);

            assertEquals('B', line.getCell(1).getCharacter());
            assertTrue(line.getCell(0).isEmpty());
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
    @DisplayName("setWideCell()")
    class SetWideCell {

        @Test
        @DisplayName("takes two columns: left = character, right = continuation")
        void testSetWideCellTwoColumns() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);

            assertEquals((char) 0x4E00, line.getCell(0).getCharacter());
            assertFalse(line.getCell(0).isWideContinuation());
            assertTrue(line.getCell(1).isWideContinuation());
        }

        @Test
        @DisplayName("throws when the second column would be out of bounds")
        void testSetWideCellAtLastColumnThrows() {
            TerminalLine line = new TerminalLine(4);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> line.setWideCell(3, (char) 0x4E00, CellAttributes.DEFAULT)
            );
        }

        @Test
        @DisplayName("clears a prior wide pair before writing")
        void testSetWideCellClearsPriorPair() {
            TerminalLine line = new TerminalLine(6);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);
            line.setWideCell(0, (char) 0x4E01, CellAttributes.DEFAULT);

            assertEquals((char) 0x4E01, line.getCell(0).getCharacter());
            assertTrue(line.getCell(1).isWideContinuation());
        }
    }

    @Nested
    @DisplayName("fill() / fill()")
    class FillAndClear {

        @Test
        @DisplayName("fill() sets every cell to the given character")
        void testFillAllCells() {
            TerminalLine line = new TerminalLine(4);
            EnumSet<TextStyle> styles = EnumSet.noneOf(TextStyle.class);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.YELLOW, TerminalColor.DEFAULT, styles);

            line.fill('-', cellAttributes);

            for (int i = 0; i < 4; i++) {
                assertEquals('-', line.getCell(i).getCharacter());
                assertFalse(line.getCell(i).isEmpty());
                assertEquals(TerminalColor.YELLOW, line.getCell(i).getAttributes().foregroundColor());
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
    @DisplayName("insertCellsAt()")
    class InsertCellsAt {

        @Test
        @DisplayName("inserting at column 0 shifts all existing cells right")
        void testInsertAtZeroShiftsAll() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);

            Cell[] result = line.insertCellsAt(0, narrowCells("E"));

            assertEquals('E', line.getCell(0).getCharacter());
            assertEquals('A', line.getCell(1).getCharacter());
            assertEquals('B', line.getCell(2).getCharacter());
            assertEquals('C', line.getCell(3).getCharacter());
            assertEquals('D', line.getCell(4).getCharacter());

            assertEquals(1, result.length);
            assertTrue(result[0].isEmpty());
        }

        @Test
        @DisplayName("inserting in the middle shifts cells from that column rightward")
        void testInsertInMiddleShiftsRightward() {
            TerminalLine line = new TerminalLine(5);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);

            line.insertCellsAt(2, narrowCells("E"));

            assertEquals('A', line.getCell(0).getCharacter());
            assertEquals('B', line.getCell(1).getCharacter());
            assertEquals('E', line.getCell(2).getCharacter());
            assertEquals('C', line.getCell(3).getCharacter());
            assertEquals('D', line.getCell(4).getCharacter());
        }

        @Test
        @DisplayName("cells that do not fit are returned as displaced, in order")
        void testDisplacedCellsReturned() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);

            Cell[] result = line.insertCellsAt(0, narrowCells("E"));

            assertEquals(1, result.length);
            assertEquals('D', result[0].getCharacter());
        }

        @Test
        @DisplayName("inserting multiple cells at once shifts correctly")
        void testInsertMultipleCells() {
            TerminalLine line = new TerminalLine(6);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);

            line.insertCellsAt(0, narrowCells("XY"));

            assertEquals('X', line.getCell(0).getCharacter());
            assertEquals('Y', line.getCell(1).getCharacter());
            assertEquals('A', line.getCell(2).getCharacter());
            assertEquals('B', line.getCell(3).getCharacter());
        }

        @Test
        @DisplayName("excess toInsert beyond line capacity returned as overflow")
        void testOverflowReturnedWithDisplaced() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);
            line.setCell(1, 'B', CellAttributes.DEFAULT);
            line.setCell(2, 'C', CellAttributes.DEFAULT);
            line.setCell(3, 'D', CellAttributes.DEFAULT);

            Cell[] result = line.insertCellsAt(0, narrowCells("EFGHI"));

            assertEquals(5, result.length);
            assertEquals('I', result[0].getCharacter());
            assertEquals('A', result[1].getCharacter());
            assertEquals('B', result[2].getCharacter());
            assertEquals('C', result[3].getCharacter());
            assertEquals('D', result[4].getCharacter());
        }

        @Test
        @DisplayName("empty flags on existing cells are preserved through the shift")
        void testEmptyCellsFlagPreservedOnShift() {
            TerminalLine line = new TerminalLine(4);
            line.setCell(0, 'A', CellAttributes.DEFAULT);

            line.insertCellsAt(0, narrowCells("E"));

            assertEquals('E', line.getCell(0).getCharacter());
            assertEquals('A', line.getCell(1).getCharacter());
            assertTrue(line.getCell(2).isEmpty());
            assertTrue(line.getCell(3).isEmpty());
        }

        @Test
        @DisplayName("wide pair split: orphaned wide character returned with the wide continuation")
        void testWidePairSplitRecovery() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(2, (char) 0x4E00, CellAttributes.DEFAULT);

            Cell[] result = line.insertCellsAt(0, narrowCells("X"));

            assertEquals(2, result.length);
            assertEquals((char) 0x4E00, result[0].getCharacter());
            assertFalse(result[0].isWideContinuation());
            assertTrue(result[1].isWideContinuation());
            assertTrue(line.getCell(3).isEmpty());
        }

        @Test
        @DisplayName("wide pair in toInsert is not split across the line edge")
        void testWidePairInToInsertNotSplit() {
            TerminalLine line = new TerminalLine(4);

            Cell wideChar = new Cell((char) 0x4E00, CellAttributes.DEFAULT);
            Cell wideContinuation = new Cell();
            wideContinuation.setWideContinuation();
            Cell[] toInsert = {new Cell('X', CellAttributes.DEFAULT), wideChar, wideContinuation};

            Cell[] result = line.insertCellsAt(2, toInsert);

            assertEquals('X', line.getCell(2).getCharacter());

            boolean foundWide = false;

            for (Cell cell : result) {
                if (!cell.isEmpty() && !cell.isWideContinuation() && cell.getCharacter() == (char) 0x4E00) {
                    foundWide = true;
                    break;
                }
            }

            assertTrue(foundWide, "Wide char should be in overflow");
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

        @Test
        @DisplayName("wide continuation cell renders as space")
        void testWideContinuationRendersAsSpace() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);
            String s = line.toString();

            assertEquals(4, s.length());
            assertEquals(' ', s.charAt(1));
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
        @DisplayName("copy keeps softWrapped flag")
        void testCopyKeepsSoftWrapped() {
            TerminalLine line = new TerminalLine(4);
            line.setSoftWrapped(true);
            TerminalLine copy = line.copy();

            assertTrue(copy.isSoftWrapped());
        }

        @Test
        @DisplayName("copy keeps wide pair structure")
        void testCopyKeepsWidePair() {
            TerminalLine line = new TerminalLine(4);
            line.setWideCell(0, (char) 0x4E00, CellAttributes.DEFAULT);
            TerminalLine copy = line.copy();

            assertEquals((char) 0x4E00, copy.getCell(0).getCharacter());
            assertTrue(copy.getCell(1).isWideContinuation());
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
