import org.example.CellAttributes;
import org.example.TerminalColor;
import org.example.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CellAttributes")
class CellAttributesTest {

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("stores given foreground, background and styles")
        void testConstructor() {
            Set<TextStyle> styles = Set.of(TextStyle.BOLD, TextStyle.UNDERLINE);
            CellAttributes cellAttributes = new CellAttributes(TerminalColor.RED, TerminalColor.BLUE, styles);

            assertEquals(TerminalColor.RED, cellAttributes.getForegroundColor());
            assertEquals(TerminalColor.BLUE, cellAttributes.getBackgroundColor());
            assertTrue(cellAttributes.getStyles().contains(TextStyle.BOLD));
            assertTrue(cellAttributes.getStyles().contains(TextStyle.UNDERLINE));
        }
    }

    @Nested
    @DisplayName("DEFAULT constant")
    class Default {

        @Test
        @DisplayName("DEFAULT has DEFAULT foreground")
        void testDefaultForeground() {
            assertEquals(TerminalColor.DEFAULT, CellAttributes.DEFAULT.getForegroundColor());
        }

        @Test
        @DisplayName("DEFAULT has DEFAULT background")
        void testDefaultBackground() {
            assertEquals(TerminalColor.DEFAULT, CellAttributes.DEFAULT.getBackgroundColor());
        }

        @Test
        @DisplayName("DEFAULT has no styles")
        void testDefaultNoStyles() {
            assertTrue(CellAttributes.DEFAULT.getStyles().isEmpty());
        }
    }
}
