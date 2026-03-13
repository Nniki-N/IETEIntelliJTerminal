package org.example;

import java.util.HashSet;
import java.util.Set;

public final class CellAttributes {
    private final TerminalColor foregroundColor;
    private final TerminalColor backgroundColor;
    private final Set<TextStyle> styles;

    public static final CellAttributes DEFAULT = new CellAttributes(
            TerminalColor.DEFAULT,
            TerminalColor.DEFAULT,
            new HashSet<>()
    );

    public CellAttributes(TerminalColor foregroundColor, TerminalColor backgroundColor, Set<TextStyle> styles) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.styles = styles;
    }

    public TerminalColor getForegroundColor() {
        return foregroundColor;
    }

    public TerminalColor getBackgroundColor() {
        return backgroundColor;
    }

    public Set<TextStyle> getStyles() {
        return styles;
    }
}
