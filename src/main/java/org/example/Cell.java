package org.example;

public final class Cell {
    private char character;
    private CellAttributes attributes;
    private boolean empty;

    public Cell() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
    }

    public Cell(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = attributes;
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

    public void set(char character, CellAttributes attributes) {
        this.character = character;
        this.attributes = attributes;
        this.empty = false;
    }

    public void setEmpty() {
        this.character = ' ';
        this.attributes = CellAttributes.DEFAULT;
        this.empty = true;
    }
}
