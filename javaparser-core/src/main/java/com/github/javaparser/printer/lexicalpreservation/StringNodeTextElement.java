package com.github.javaparser.printer.lexicalpreservation;

class StringNodeTextElement extends NodeTextElement {
    private String text;

    public StringNodeTextElement(String text) {
        this.text = text;
    }

    @Override
    String expand() {
        return text;
    }

    public String getText() {
        return text;
    }
}
