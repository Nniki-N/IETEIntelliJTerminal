package org.example;

import java.util.HashSet;
import java.util.Set;

public final class CellAttributes {
    private final TerminalColor foreground;
    private final TerminalColor background;
    private final Set<TextStyle> styles;

    public static final CellAttributes DEFAULT = new CellAttributes(
            TerminalColor.DEFAULT,
            TerminalColor.DEFAULT,
            new HashSet<>()
    );

    public CellAttributes(TerminalColor foreground, TerminalColor background, Set<TextStyle> styles) {
        this.background = background;
        this.foreground = foreground;
        this.styles = styles;
    }

    public TerminalColor getForeground() {
        return foreground;
    }

    public TerminalColor getBackground() {
        return background;
    }

    public Set<TextStyle> getStyles() {
        return styles;
    }

}
