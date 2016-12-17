package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;

class ChildTextElement extends TextElement {
    private LexicalPreservingPrinter lexicalPreservingPrinter;
    private Node child;

    ChildTextElement(LexicalPreservingPrinter lexicalPreservingPrinter, Node child) {
        this.lexicalPreservingPrinter = lexicalPreservingPrinter;
        this.child = child;
    }

    String expand() {
        return lexicalPreservingPrinter.print(child);
    }

    Node getChild() {
        return child;
    }
}
