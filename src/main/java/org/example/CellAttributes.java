package org.example;

import java.util.Set;

public class CellAttributes {
    private final TerminalColor foreground;
    private final TerminalColor background;
    private final Set<TextStyle> styles;

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
