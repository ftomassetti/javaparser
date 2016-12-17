package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.utils.Pair;

import java.util.LinkedList;
import java.util.List;

class NodeText {
    private LexicalPreservingPrinter lexicalPreservingPrinter;
    private List<TextElement> elements;

    NodeText(LexicalPreservingPrinter lexicalPreservingPrinter, List<TextElement> elements) {
        this.lexicalPreservingPrinter = lexicalPreservingPrinter;
        this.elements = elements;
    }

    public NodeText(LexicalPreservingPrinter lexicalPreservingPrinter) {
        this(lexicalPreservingPrinter, new LinkedList<>());
    }

    String expand() {
        StringBuffer sb = new StringBuffer();

        elements.forEach(e -> sb.append(e.expand()));
        return sb.toString();
    }

    public void addElement(TextElement nodeTextElement) {
        this.elements.add(nodeTextElement);
    }

    public void addChild(Node child) {
        addElement(new ChildTextElement(lexicalPreservingPrinter, child));
    }

    public void addElement(int index, TextElement nodeTextElement) {
        this.elements.add(index, nodeTextElement);
    }

    public void removeElementsForChild(Node child) {
        elements.removeIf(e -> e instanceof ChildTextElement && ((ChildTextElement)e).getChild() == child);
    }

    // Visible for testing
    int numberOfElements() {
        return elements.size();
    }

    // Visible for testing
    TextElement getTextElement(int index) {
        return elements.get(index);
    }

    public void addToken(int tokenKind, String text) {
        elements.add(new TokenTextElement(tokenKind, text));
    }

    public void addToken(int index, int tokenKind, String text) {
        elements.add(index, new TokenTextElement(tokenKind, text));
    }
//
//    public void replaceElement(int index, NodeTextElement nodeTextElement) {
//        this.elements.remove(index);
//        addElement(index, nodeTextElement);
//    }
//
    public void addList(NodeList<?> children, boolean separatorAfterLast, LexicalPreservingPrinter.Separator... separators) {
        for (int i=0; i<children.size(); i++) {
            Node child = children.get(i);
            addElement(new ChildTextElement(lexicalPreservingPrinter, child));
            if ((i+1)<children.size() || separatorAfterLast) {
                for (LexicalPreservingPrinter.Separator s : separators) {
                    addToken(s);
                }
            }
        }
    }

    public void addToken(LexicalPreservingPrinter.Separator separator) {
        addToken(separator.getTokenKind(), separator.getText());
    }

    public void addToken(int index, LexicalPreservingPrinter.Separator separator) {
        addToken(index, separator.getTokenKind(), separator.getText());
    }
//
//    public void addString(String string) {
//        addElement(new StringNodeTextElement(string));
//    }
//
//    public void replaceChild(Node oldChild, Node newChild) {
//        for (int i=0; i<elements.size(); i++) {
//            NodeTextElement element = elements.get(i);
//            if (element instanceof ChildNodeTextElement) {
//                ChildNodeTextElement childNodeTextElement = (ChildNodeTextElement)element;
//                if (childNodeTextElement.getChild() == oldChild) {
//                    elements.set(i, new ChildNodeTextElement(lexicalPreservingPrinter, newChild));
//                    return;
//                }
//            }
//        }
//        throw new IllegalArgumentException();
//    }
//
//    public void addString(int index, String string) {
//        elements.add(index, new StringNodeTextElement(string));
//    }
//
    public void removeTextBetween(int tokenKind, Node child) {
        int lastTokenFound = -1;
        for (int i=0; i<elements.size(); i++) {
            TextElement element = elements.get(i);
            if (element instanceof ChildTextElement) {
                ChildTextElement childNodeTextElement = (ChildTextElement)element;
                if (childNodeTextElement.getChild() == child) {
                    if (lastTokenFound == -1) {
                        throw new IllegalArgumentException();
                    }
                    while (i > lastTokenFound) {
                        elements.remove(--i);
                    }
                    return;
                }
            } else if (element instanceof TokenTextElement){
                TokenTextElement tokenTextElement = (TokenTextElement)element;
                if (tokenTextElement.getTokenKind() == tokenKind) {
                    lastTokenFound = i;
                }
            }
        }
    }

    public void removeTextBetween(Node child, int tokenKind, boolean removeSpaceImmediatelyAfter) {
       // FIXME
       removeTextBetween(tokenKind, child);
    }
//
//    public void removeString(String string) {
//        for (int i=0; i<elements.size(); i++) {
//            NodeTextElement element = elements.get(i);
//            if (element instanceof StringNodeTextElement) {
//                StringNodeTextElement stringNodeTextElement = (StringNodeTextElement)element;
//                elements.set(i, stringNodeTextElement.removeString(string));
//            }
//        }
//    }
//
//    public void addAtBeginningString(String string) {
//        elements.add(0, new StringNodeTextElement(string));
//    }
}
