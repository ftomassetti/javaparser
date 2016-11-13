package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;

/**
 * Created by federico on 13/11/16.
 */
class ChildNodeTextElement extends NodeTextElement {
    private LexicalPreservingPrinter lexicalPreservingPrinter;
    private Node child;

    public ChildNodeTextElement(LexicalPreservingPrinter lexicalPreservingPrinter, Node child) {
        this.lexicalPreservingPrinter = lexicalPreservingPrinter;
        this.child = child;
    }

    @Override
    String expand() {
        return lexicalPreservingPrinter.print(child);
    }

    public Node getChild() {
        return child;
    }
}
