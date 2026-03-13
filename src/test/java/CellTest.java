import org.example.Cell;
import org.example.CellAttributes;
import org.example.TerminalColor;
import org.example.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cell")
class CellTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("default constructor creates an empty cell with DEFAULT attributes")
        void testDefaultConstructor() {
            Cell cell = new Cell();

            assertTrue(cell.isEmpty());
            assertEquals(' ', cell.getCharacter());
            assertEquals(CellAttributes.DEFAULT, cell.getAttributes());
        }

        @Test
        @DisplayName("char constructor creates a non-empty cell with given attributes")
        void testCharConstructor() {
            Set<TextStyle> styles = Set.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, styles);
            Cell cell = new Cell('A', cellAttributes);

            assertFalse(cell.isEmpty());
            assertEquals('A', cell.getCharacter());
            assertEquals(cellAttributes, cell.getAttributes());
        }
    }

    @Nested
    @DisplayName("set()")
    class SetCharacter {

        @Test
        @DisplayName("set() saves character and marks cell non-empty")
        void testSetSavesCharacter() {
            Cell cell = new Cell('A', CellAttributes.DEFAULT);
            cell.set('B', CellAttributes.DEFAULT);

            assertFalse(cell.isEmpty());
            assertEquals('B', cell.getCharacter());
        }

        @Test
        @DisplayName("set() stores the provided attributes")
        void testSetSavesAttributes() {
            Set<TextStyle> styles = new HashSet<>();
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.GREEN, TerminalColor.DEFAULT, styles);

            Cell cell = new Cell();
            cell.set('Q', cellAttributes);

            assertEquals(TerminalColor.GREEN, cell.getAttributes().getForegroundColor());
            assertEquals(TerminalColor.DEFAULT, cell.getAttributes().getBackgroundColor());
            assertTrue(cell.getAttributes().getStyles().isEmpty());
        }
    }

    @Nested
    @DisplayName("setEmpty()")
    class SetEmpty {

        @Test
        @DisplayName("setEmpty() marks the cell empty and resets character to space")
        void testSetEmptyMarksCellEmptyAndResetsCharacter() {
            Cell cell = new Cell('A', CellAttributes.DEFAULT);
            cell.setEmpty();

            assertTrue(cell.isEmpty());
            assertEquals(' ', cell.getCharacter());
        }

        @Test
        @DisplayName("setEmpty() resets attributes to DEFAULT")
        void testSetEmptyResetsAttributes() {
            Set<TextStyle> styles = Set.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, styles);
            Cell cell = new Cell('X', cellAttributes);
            cell.setEmpty();

            assertEquals(CellAttributes.DEFAULT, cell.getAttributes());
        }
    }

    @Nested
    @DisplayName("copy()")
    class Copy {

        @Test
        @DisplayName("copy has same attributes")
        void testCopyHasSameAttributes() {
            Set<TextStyle> styles = Set.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.CYAN, TerminalColor.DEFAULT, styles);
            Cell original = new Cell('M', cellAttributes);
            Cell copy = original.copy();

            assertEquals('M', copy.getCharacter());
            assertFalse(copy.isEmpty());
            assertEquals(TerminalColor.CYAN, copy.getAttributes().getForegroundColor());
            assertEquals(TerminalColor.DEFAULT, copy.getAttributes().getBackgroundColor());
            assertTrue(copy.getAttributes().getStyles().contains(TextStyle.BOLD));
        }

        @Test
        @DisplayName("changing the copy does not affect the original")
        void testCopyIsIndependent() {
            Cell original = new Cell('A', CellAttributes.DEFAULT);
            Cell copy = original.copy();
            copy.set('B', CellAttributes.DEFAULT);

            assertEquals('A', original.getCharacter());
            assertEquals('B', copy.getCharacter());
        }
    }
}