package org.example;

import java.util.Objects;

/**
 * A single cell in the terminal grid.
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
     * True when this cell is the right-half placeholder of a wide character.
     * It is displayed as a space and should not be independently edited.
     */
    private boolean wideContinuation;

    /**
     * Creates an empty cell with DEFAULT attributes.
     */
    public Cell() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
        this.wideContinuation = false;
    }

    /**
     * Creates a filled cell with the given character and attributes.
     */
    public Cell(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = Objects.requireNonNull(attributes, "attributes must ne not null");
        this.empty = false;
        this.wideContinuation = false;
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
     * Returns true if this cell is the right-half placeholder of a wide character.
     * The actual character is stored in the cell immediately to the left.
     */
    public boolean isWideContinuation() {
        return wideContinuation;
    }

    /**
     * Sets a character and marks the cell as non-empty.
     */
    public void set(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = Objects.requireNonNull(attributes, "attributes must ne not null");
        this.empty = false;
        this.wideContinuation = false;
    }

    /**
     * Marks this cell as the right-half placeholder of a wide character.
     * The cell renders as a space; the actual character lives in the cell to the left.
     */
    public void setWideContinuation() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = false;
        this.wideContinuation = true;
    }


    /**
     * Resets this cell to the empty state with default attributes.
     */
    public void setEmpty() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
        this.wideContinuation = false;
    }

    /**
     * Returns a deep copy. The copy's state is independent of the origin cell state.
     */
    public Cell copy() {
        Cell copy = new Cell(this.character, this.attributes);
        copy.empty = this.empty;
        copy.wideContinuation = this.wideContinuation;

        return copy;
    }
}
