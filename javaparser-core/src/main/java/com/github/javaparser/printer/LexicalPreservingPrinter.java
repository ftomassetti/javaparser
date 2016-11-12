package com.github.javaparser.printer;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class LexicalPreservingPrinter {

    private Map<Node, String> textForNodes = new IdentityHashMap<>();
    private Map<Node, IdentityHashMap<Node, String>> placeholdersMap = new IdentityHashMap<>();

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
            final String text = textForNodes.get(node);
            // Expand children
            List<String> placeholders = new ArrayList<>(placeholdersMap.get(node).values());
            // sort placeholders by position
            placeholders.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return text.indexOf(o2) - text.indexOf(o1);
                }
            });
            int delta = 0;
            String replacedText = text;
            for (String placeholder : placeholders) {
                Node child = placeholdersMap.get(node).entrySet().stream().filter(e -> e.getValue().equals(placeholder)).map(e -> e.getKey()).findFirst().get();
                replacedText = replacedText.replace(placeholder, print(child));
            }

            writer.append(replacedText);
        } else {
            writer.append(node.toString());
        }
    }

    public void registerText(Node node, String documentCode) {
        IdentityHashMap<Node, String> pmap = new IdentityHashMap<>();
        placeholdersMap.put(node, pmap);
        int i = 0;
        for (Node child : node.getChildNodes()) {
            pmap.put(child, "#{CHILD" + (i++) + "}");
        }
        String text = getRangeFromDocument(node.getRange(), documentCode);
        text = putPlaceholders(documentCode, node.getRange(), text, pmap);
        textForNodes.put(node, text);
    }

    private String putPlaceholders(String documentCode, Range range, String text, IdentityHashMap<Node, String> pmap) {
        int initialIndex = findIndex(documentCode, range.begin);
        List<Node> children = new ArrayList<>(pmap.keySet());
        children.sort((o1, o2) -> o1.getRange().begin.compareTo(o2.getRange().begin));

        int delta = 0;
        for (Node child : children) {
            int fromStart = findIndex(documentCode, child.getBegin()) - initialIndex;
            int lengthOfOriginalCode = getRangeFromDocument(child.getRange(), documentCode).length();
            String newSubstring = pmap.get(child);
            int index = fromStart + delta;
            text = replaceSubstring(text, index, lengthOfOriginalCode, newSubstring);
            delta += newSubstring.length() - lengthOfOriginalCode;
        }

        return text;
    }

    private String replaceSubstring(String original, int index, int lengthOfOldSubstring, String newSubstring) {
        return original.substring(0, index) + newSubstring + original.substring(index + lengthOfOldSubstring);
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

    public void updateTextBecauseOfRemovedChild(Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        // find the placeholder
        String placeholder = this.placeholdersMap.get(parent).get(child);
        // remove the placeholder
        String text = textForNodes.get(parent).replace(placeholder, "");
        textForNodes.put(parent, text);
    }
}
