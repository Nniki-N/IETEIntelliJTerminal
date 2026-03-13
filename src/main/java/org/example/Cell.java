package org.example;

import java.util.Objects;

/**
 * <p> A single cell in the terminal grid.
 *
 * <p> A cell is either empty (no character written) or holds one character with
 * its attributes. Empty cells are spaces and have DEFAULT attributes.
 * The empty cell can be checked with {@code isEmpty() == true}.
 *
 * <p> Cells are mutable so that lines can be edited in place without creating a new object.
 */
public final class Cell {
    private char character;
    private CellAttributes attributes;
    private boolean empty;

    /**
     * Creates an empty cell with DEFAULT attributes.
     */
    public Cell() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
    }

    /**
     * Creates a filled cell with the given character and attributes.
     */
    public Cell(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = Objects.requireNonNull(attributes, "attributes must ne not null");
        this.empty = false;
    }

    public char getCharacter() {
        return character;
    }

    public CellAttributes getAttributes() {
        return attributes;
    }

    public boolean isEmpty() {
        return empty;
    }

    /**
     * Sets a character and marks the cell as non-empty.
     */
    public void set(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = Objects.requireNonNull(attributes, "attributes must ne not null");
        this.empty = false;
    }

    /**
     * Resets this cell to the empty state with default attributes.
     */
    public void setEmpty() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
    }

    /**
     * Returns a deep copy. The copy's state is independent of the origin cell state.
     */
    public Cell copy() {
        Cell copy = new Cell(this.character, this.attributes);
        copy.empty = this.empty;

        return copy;
    }
}
