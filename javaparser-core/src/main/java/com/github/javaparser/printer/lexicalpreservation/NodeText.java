package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;

import java.util.LinkedList;
import java.util.List;

class NodeText {
    private List<NodeTextElement> elements = new LinkedList<>();

    public String expand() {
        StringBuffer sb = new StringBuffer();
        elements.forEach(e -> sb.append(e.expand()));
        return sb.toString();
    }

    public void addElement(NodeTextElement nodeTextElement) {
        this.elements.add(nodeTextElement);
    }

    public void removeElementsForChild(Node child) {
        elements.removeIf(e -> e instanceof ChildNodeTextElement && ((ChildNodeTextElement)e).getChild() == child);
    }

    // Visible for testing
    int numberOfElements() {
        return elements.size();
    }

    // Visible for testing
    NodeTextElement getTextElement(int index) {
        return elements.get(index);
    }
}
