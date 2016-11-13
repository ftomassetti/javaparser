package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class LexicalPreservingPrinter {

    private interface Inserter {
        void insert(Node parent, Node child);
    }

    private Map<Node, NodeText> textForNodes = new IdentityHashMap<>();

    public String print(Node node) {
        StringWriter writer = new StringWriter();
        try {
            print(node, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException on a StringWriter", e);
        }
        return writer.toString();
    }

    public void print(Node node, Writer writer) throws IOException {
        if (textForNodes.containsKey(node)) {
            final NodeText text = textForNodes.get(node);
            writer.append(text.expand());
        } else {
            writer.append(node.toString());
        }
    }

    public void registerText(Node node, String documentCode) {
        String text = getRangeFromDocument(node.getRange(), documentCode);
        NodeText nodeText = putPlaceholders(documentCode, node.getRange(), text, new ArrayList<>(node.getChildNodes()));
        textForNodes.put(node, nodeText);
    }

    public NodeText getTextForNode(Node node) {
        return textForNodes.get(node);
    }

    private NodeText putPlaceholders(String documentCode, Range range, String text, List<Node> children) {

        children.sort((o1, o2) -> o1.getRange().begin.compareTo(o2.getRange().begin));

        NodeText nodeText = new NodeText(this);

        int start = findIndex(documentCode, range.begin);
        int caret = start;
        for (Node child : children) {
            int childStartIndex = findIndex(documentCode, child.getBegin());
            int fromStart = childStartIndex - caret;
            if (fromStart > 0) {
                nodeText.addElement(new StringNodeTextElement(text.substring(caret - start, childStartIndex - start)));
                caret += fromStart;
            }
            nodeText.addElement(new ChildNodeTextElement(this, child));
            int lengthOfOriginalCode = getRangeFromDocument(child.getRange(), documentCode).length();
            caret += lengthOfOriginalCode;
        }
        // last string
        int endOfNode = findIndex(documentCode, range.end) + 1;
        if (caret < endOfNode) {
            nodeText.addElement(new StringNodeTextElement(text.substring(caret - start)));
        }

        return nodeText;
    }

    private String getRangeFromDocument(Range range, String documentCode) {
        if (range.equals(Range.UNKNOWN)) {
            throw new IllegalArgumentException();
        }
        return documentCode.substring(findIndex(documentCode, range.begin), findIndex(documentCode, range.end) + 1);
    }

    private int findIndex(String documentCode, Position position) {
        int indexOfLineStart = 0;
        for (int i = 1; i < position.line; i++) {
            int indexR = documentCode.indexOf('\r', indexOfLineStart);
            int indexN = documentCode.indexOf('\n', indexOfLineStart);
            int nextIndex = -1;
            if (indexN == -1 && indexR != -1) {
                nextIndex = indexR;
            } else if (indexN != -1 && indexR == -1) {
                nextIndex = indexN;
            } else {
                nextIndex = Math.min(indexR, indexN);
            }
            if (nextIndex == -1) {
                throw new IllegalArgumentException("Searching for line "+position.line);
            }
            if ((documentCode.charAt(nextIndex) == '\r' && documentCode.charAt(nextIndex + 1) == '\n') ||
                    (documentCode.charAt(nextIndex) == '\n' && documentCode.charAt(nextIndex + 1) == '\r')) {
                nextIndex++;
            }
            indexOfLineStart = nextIndex + 1;
        }
        return findIndexOfColumn(documentCode, indexOfLineStart, position.column);
    }

    private int findIndexOfColumn(String documentCode, int indexOfLineStart, int column) {
        // consider tabs
        return indexOfLineStart + column - 1;
    }

    public void updateTextBecauseOfRemovedChild(Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();

        textForNodes.get(parent).removeElementsForChild(child);
    }

    public void updateTextBecauseOfAddedChild(NodeList nodeList, int index, Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        String nodeListName = findNodeListName(nodeList, parent);

        if (index == 0) {
            Inserter inserter = getPositionFinder(parent.getClass(), nodeListName);
            inserter.insert(parent, child);
        } else {
            throw new UnsupportedOperationException(nodeListName);
        }
    }

    private String findNodeListName(NodeList nodeList, Node parent) {
        for (Method m : parent.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().getCanonicalName().equals(NodeList.class.getCanonicalName())) {
                try {
                    NodeList result = (NodeList)m.invoke(parent);
                    if (result == nodeList) {
                        String name = m.getName();
                        if (name.startsWith("get")) {
                            name = name.substring("get".length());
                        }
                        return name;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException();
    }

    private Inserter getPositionFinder(Class<?> parentClass, String nodeListName) {
        String key = String.format("%s:%s", parentClass.getSimpleName(), nodeListName);
        switch (key) {
            case "ClassOrInterfaceDeclaration:Members":
                return insertAfter("{");
            case "FieldDeclaration:Variables":
                try {
                    return insertAfterChild(FieldDeclaration.class.getMethod("getElementType"), " ");
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
        }

        throw new UnsupportedOperationException(key);
    }

    private void printModifiers(NodeText nodeText, final EnumSet<Modifier> modifiers) {
        if (modifiers.size() > 0) {
            nodeText.addElement(new StringNodeTextElement(modifiers.stream().map(Modifier::getLib).collect(Collectors.joining(" ")) + " "));
        }
    }

    private NodeText prettyPrintingTextNode(Node node) {
        NodeText nodeText = new NodeText(this);
        if (node instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration)node;
            nodeText.addList(fieldDeclaration.getAnnotations(), "\n", true);
            printModifiers(nodeText, fieldDeclaration.getModifiers());
            nodeText.addChild(fieldDeclaration.getElementType());
            nodeText.addString(" ");
            nodeText.addList(fieldDeclaration.getArrayBracketPairsAfterElementType(), "", true);
            nodeText.addString(" ");
            nodeText.addList(fieldDeclaration.getVariables(), ", ", false);
            nodeText.addString(";\n");
            return nodeText;
        }
        throw new UnsupportedOperationException(node.getClass().getCanonicalName());
    }

    private NodeText getOrCreateNodeText(Node node) {
        if (!textForNodes.containsKey(node)) {
            textForNodes.put(node, prettyPrintingTextNode(node));
        }
        return textForNodes.get(node);
    }

    private Inserter insertAfterChild(Method method, String separatorBefore) {
        return (parent, child) -> {
            try {
                NodeText nodeText = getOrCreateNodeText(parent);
                Node childToFollow = (Node) method.invoke(parent);
                if (childToFollow == null) {
                    nodeText.addElement(0, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                    return;
                }
                for (int i=0; i< nodeText.numberOfElements();i++) {
                    NodeTextElement element = nodeText.getTextElement(i);
                    if (element instanceof ChildNodeTextElement) {
                        ChildNodeTextElement childElement = (ChildNodeTextElement)element;
                        if (childElement.getChild() == childToFollow) {
                            nodeText.addString(i+1, separatorBefore);
                            nodeText.addElement(i+2, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                            return;
                        }
                    }
                }
                throw new IllegalArgumentException();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Inserter insertAfter(final String subString) {
        return (parent, child) -> {
            NodeText nodeText = textForNodes.get(parent);
            for (int i=0; i< nodeText.numberOfElements();i++) {
                NodeTextElement element = nodeText.getTextElement(i);
                if (element instanceof StringNodeTextElement) {
                    StringNodeTextElement stringElement = (StringNodeTextElement)element;
                    int index = stringElement.getText().indexOf(subString);
                    if (index != -1) {
                        int end = index + subString.length();
                        String textBefore = stringElement.getText().substring(0, end);
                        String textAfter = stringElement.getText().substring(end);
                        if (textAfter.isEmpty()) {
                            nodeText.addElement(i+1, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                        } else {
                            nodeText.replaceElement(i, new StringNodeTextElement(textBefore));
                            nodeText.addElement(i+1, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
                            nodeText.addElement(i+2, new StringNodeTextElement(textAfter));
                        }
                        return;
                    }
                }
            }
            throw new IllegalArgumentException();
        };
    }
}
