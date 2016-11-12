package com.github.javaparser.printer;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.IdentityHashMap;
import java.util.Map;

public class LexicalPreservingPrinter {

    private Map<Node, String> textForNodes = new IdentityHashMap<>();
    private Map<NodeList, String> textForNodeLists = new IdentityHashMap<>();

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
            writer.append(textForNodes.get(node));
        } else {
            writer.append(node.toString());
        }
    }

    public void registerText(Node node, String documentCode) {
        textForNodes.put(node, getRangeFromDocument(node.getRange(), documentCode));
    }

    private String getRangeFromDocument(Range range, String documentCode) {
        if (range.equals(Range.UNKNOWN)) {
            throw new IllegalArgumentException();
        }
        if (range.begin.equals(range.end)) {
            return "";
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
}
