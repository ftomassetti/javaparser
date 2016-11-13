package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class LexicalPreservingPrinter {

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

        NodeText nodeText = new NodeText();

        int start = findIndex(documentCode, range.begin);
        int caret = start;
        for (Node child : children) {
            int childStartIndex = findIndex(documentCode, child.getBegin());
            int childEndIndex = findIndex(documentCode, child.getEnd());
            int fromStart = childStartIndex - caret;
            if (fromStart > 0) {
                nodeText.addElement(new StringNodeTextElement(text.substring(caret - start, childStartIndex - start)));
                caret += fromStart;
            }
            nodeText.addElement(new ChildNodeTextElement(this, child));
            int lengthOfOriginalCode = getRangeFromDocument(child.getRange(), documentCode).length();
            caret += lengthOfOriginalCode;
            //int lengthOfOriginalCode = getRangeFromDocument(child.getRange(), documentCode).length();
            //String newSubstring = pmap.get(child);
            //int index = fromStart + delta;
            //text = replaceSubstring(text, index, lengthOfOriginalCode, newSubstring);
            //delta += newSubstring.length() - lengthOfOriginalCode;
        }
        // last string
        int endOfNode = findIndex(documentCode, range.end) + 1;
        if (caret < endOfNode) {
            nodeText.addElement(new StringNodeTextElement(text.substring(caret - start)));
        }

        return nodeText;
    }

    private String replaceSubstring(String original, int index, int lengthOfOldSubstring, String newSubstring) {
        return original.substring(0, index) + newSubstring + original.substring(index + lengthOfOldSubstring);
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
}
