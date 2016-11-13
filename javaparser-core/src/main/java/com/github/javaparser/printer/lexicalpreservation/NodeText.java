package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

import java.util.LinkedList;
import java.util.List;

class NodeText {
    private LexicalPreservingPrinter lexicalPreservingPrinter;
    private List<NodeTextElement> elements;

    public NodeText(LexicalPreservingPrinter lexicalPreservingPrinter, List<NodeTextElement> elements) {
        this.lexicalPreservingPrinter = lexicalPreservingPrinter;
        this.elements = elements;
    }


    public NodeText(LexicalPreservingPrinter lexicalPreservingPrinter) {
        this(lexicalPreservingPrinter, new LinkedList<>());
    }


    public String expand() {
        StringBuffer sb = new StringBuffer();

        elements.forEach(e -> sb.append(e.expand()));
        return sb.toString();
    }

    public void addElement(NodeTextElement nodeTextElement) {
        this.elements.add(nodeTextElement);
    }

    public void addChild(Node child) {
        addElement(new ChildNodeTextElement(lexicalPreservingPrinter, child));
    }

    public void addElement(int index, NodeTextElement nodeTextElement) {
        this.elements.add(index, nodeTextElement);
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

    public void replaceElement(int index, NodeTextElement nodeTextElement) {
        this.elements.remove(index);
        addElement(index, nodeTextElement);
    }

    public void addList(NodeList<?> children, String separator, boolean separatorAfterLast) {
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            addElement(new ChildNodeTextElement(lexicalPreservingPrinter, child));
            if ((i+1)<children.size() || separatorAfterLast) {
                addElement(new StringNodeTextElement(separator));
            }
        }
    }

    public void addString(String string) {
        addElement(new StringNodeTextElement(string));
    }

    public void replaceChild(Node oldChild, Node newChild) {
        for (int i=0; i<elements.size(); i++) {
            NodeTextElement element = elements.get(i);
            if (element instanceof ChildNodeTextElement) {
                ChildNodeTextElement childNodeTextElement = (ChildNodeTextElement)element;
                if (childNodeTextElement.getChild() == oldChild) {
                    elements.set(i, new ChildNodeTextElement(lexicalPreservingPrinter, newChild));
                    return;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    public void addString(int index, String string) {
        elements.add(index, new StringNodeTextElement(string));
    }
}
