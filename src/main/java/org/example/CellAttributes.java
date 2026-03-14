package org.example;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Attributes applied to a cell: foreground color, background color,
 * and a set of default styles (bold, italic, underline).
 *
 * <p> Use the {@code with*()} builder methods to derive new instances.
 */
public record CellAttributes(TerminalColor foregroundColor, TerminalColor backgroundColor, EnumSet<TextStyle> styles) {
    public static final CellAttributes DEFAULT = new CellAttributes(
            TerminalColor.DEFAULT,
            TerminalColor.DEFAULT,
            EnumSet.noneOf(TextStyle.class)
    );

    public CellAttributes(TerminalColor foregroundColor, TerminalColor backgroundColor, EnumSet<TextStyle> styles) {
        this.foregroundColor = Objects.requireNonNull(foregroundColor, "foregroundColor must be not null");
        this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor must be not null");
        this.styles = EnumSet.copyOf(Objects.requireNonNull(styles, "styles must be not null"));
    }

    /**
     * Returns a new instance with the foreground color replaced.
     */
    public CellAttributes withForeground(TerminalColor color) {
        return new CellAttributes(color, backgroundColor, styles);
    }

    /**
     * Returns a new instance with the background color replaced.
     */
    public CellAttributes withBackground(TerminalColor color) {
        return new CellAttributes(foregroundColor, color, styles);
    }

    /**
     * Returns a new instance with the style set replaced.
     */
    public CellAttributes withStyles(EnumSet<TextStyle> newStyles) {
        return new CellAttributes(foregroundColor, backgroundColor, newStyles);
    }
}
