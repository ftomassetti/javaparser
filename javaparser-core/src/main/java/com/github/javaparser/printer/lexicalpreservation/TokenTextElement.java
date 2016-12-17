package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.JavaToken;

class TokenTextElement extends TextElement {

    private int tokenKind;
    private String text;

    TokenTextElement(JavaToken token) {
        this(token.getKind(), token.getText());
    }

    TokenTextElement(int tokenKind, String text) {
        this.tokenKind = tokenKind;
        this.text = text;
    }

    @Override
    String expand() {
        return text;
    }

    // Visible for testing
    String getText() {
        return text;
    }

    public int getTokenKind() {
        return tokenKind;
    }

//    public void removeFromDelimiterToEnd(String substring) {
//        int index = text.lastIndexOf(substring);
//        if (index == -1) {
//            throw new IllegalArgumentException();
//        }
//        text = text.substring(0, index);
//    }
//
//    public void removeUntilDelimiter(String substring, boolean removeSpaceImmediatelyAfter) {
//        int index = text.indexOf(substring);
//        if (index == -1) {
//            throw new IllegalArgumentException("Cannot find '" + substring+ "' in '" + text + "'");
//        }
//        text = text.substring(index + substring.length());
//        while (text.startsWith(" ") || text.startsWith("\t")) {
//            text = text.substring(1);
//        }
//    }
//
//    public NodeTextElement removeString(String string) {
//        return new StringNodeTextElement(text.replaceAll(string, ""));
//    }
}
