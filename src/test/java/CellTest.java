import org.example.Cell;
import org.example.CellAttributes;
import org.example.TerminalColor;
import org.example.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

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
            assertFalse(cell.isWideContinuation());
        }

        @Test
        @DisplayName("char constructor creates a non-empty cell with given attributes")
        void testCharConstructor() {
            EnumSet<TextStyle> styles = EnumSet.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, styles);
            Cell cell = new Cell('A', cellAttributes);

            assertFalse(cell.isEmpty());
            assertEquals('A', cell.getCharacter());
            assertEquals(cellAttributes, cell.getAttributes());
            assertFalse(cell.isWideContinuation());
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
        @DisplayName("set() clears wideContinuation flag if it was set")
        void testSetClearsWideContinuation() {
            Cell cell = new Cell();
            cell.setWideContinuation();
            cell.set('A', CellAttributes.DEFAULT);

            assertFalse(cell.isWideContinuation());
            assertFalse(cell.isEmpty());
        }

        @Test
        @DisplayName("set() stores the provided attributes")
        void testSetSavesAttributes() {
            EnumSet<TextStyle> styles = EnumSet.noneOf(TextStyle.class);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.GREEN, TerminalColor.DEFAULT, styles);

            Cell cell = new Cell();
            cell.set('Q', cellAttributes);

            assertEquals(TerminalColor.GREEN, cell.getAttributes().foregroundColor());
            assertEquals(TerminalColor.DEFAULT, cell.getAttributes().backgroundColor());
            assertTrue(cell.getAttributes().styles().isEmpty());
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
            assertFalse(cell.isWideContinuation());
            assertEquals(' ', cell.getCharacter());
        }

        @Test
        @DisplayName("setEmpty() resets attributes to DEFAULT")
        void testSetEmptyResetsAttributes() {
            EnumSet<TextStyle> styles = EnumSet.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, styles);
            Cell cell = new Cell('X', cellAttributes);
            cell.setEmpty();

            assertEquals(CellAttributes.DEFAULT, cell.getAttributes());
        }

        @Test
        @DisplayName("setEmpty() clears wideContinuation flag")
        void testSetEmptyClearsWideContinuation() {
            Cell cell = new Cell();
            cell.setWideContinuation();
            cell.setEmpty();

            assertFalse(cell.isWideContinuation());
            assertTrue(cell.isEmpty());
        }
    }

    @Nested
    @DisplayName("setWideContinuation()")
    class SetWideContinuation {

        @Test
        @DisplayName("marks cell as continuation placeholder")
        void testSetWideContinuationFlags() {
            Cell cell = new Cell();
            cell.setWideContinuation();

            assertTrue(cell.isWideContinuation());
            assertFalse(cell.isEmpty());
            assertEquals(' ', cell.getCharacter());
            assertEquals(CellAttributes.DEFAULT, cell.getAttributes());
        }

        @Test
        @DisplayName("overwrites a previously filled cell")
        void testSetWideContinuationOverwritesFilled() {
            Cell cell = new Cell('A', CellAttributes.DEFAULT);
            cell.setWideContinuation();

            assertTrue(cell.isWideContinuation());
            assertFalse(cell.isEmpty());
        }
    }

    @Nested
    @DisplayName("copy()")
    class Copy {

        @Test
        @DisplayName("copy has same attributes")
        void testCopyHasSameAttributes() {
            EnumSet<TextStyle> styles = EnumSet.of(TextStyle.BOLD);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.CYAN, TerminalColor.DEFAULT, styles);
            Cell original = new Cell('M', cellAttributes);
            Cell copy = original.copy();

            assertEquals('M', copy.getCharacter());
            assertFalse(copy.isEmpty());
            assertFalse(copy.isWideContinuation());
            assertEquals(TerminalColor.CYAN, copy.getAttributes().foregroundColor());
            assertEquals(TerminalColor.DEFAULT, copy.getAttributes().backgroundColor());
            assertTrue(copy.getAttributes().styles().contains(TextStyle.BOLD));
        }

        @Test
        @DisplayName("copy keeps wideContinuation flag")
        void testCopyPreservesWideContinuation() {
            Cell original = new Cell();
            original.setWideContinuation();
            Cell copy = original.copy();

            assertTrue(copy.isWideContinuation());
            assertFalse(copy.isEmpty());
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

        @Test
        @DisplayName("changing the original does not affect the copy")
        void testOriginalChangeDoesNotAffectCopy() {
            Cell original = new Cell('A', CellAttributes.DEFAULT);
            Cell copy = original.copy();
            original.setEmpty();

            assertFalse(copy.isEmpty());
            assertEquals('A', copy.getCharacter());
        }
    }
}