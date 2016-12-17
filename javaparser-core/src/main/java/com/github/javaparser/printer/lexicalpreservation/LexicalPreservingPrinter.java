package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.*;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.observer.PropagatingAstObserver;
import com.github.javaparser.utils.Pair;

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
        /*default void insert(Node parent, Node child) {
            insert(parent, new Separator[]{}, child);
        }*/
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

    public NodeText getTextForNode(Node node) {
        return textForNodes.get(node);
    }

    private int findIndexOfColumn(String documentCode, int indexOfLineStart, int column) {
        // consider tabs
        return indexOfLineStart + column - 1;
    }

    private void updateTextBecauseOfRemovedChild(NodeList nodeList, int index, Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        String key = parent.getClass().getSimpleName() + ":" + findNodeListName(nodeList);

        switch (key) {
            case "MethodDeclaration:Parameters":
                if (index == 0 && nodeList.size() > 1) {
                    // we should remove all the text between the child and the comma
                    textForNodes.get(parent).removeTextBetween(child, ASTParserConstants.COMMA, true);
                }
                if (index != 0) {
                    // we should remove all the text between the child and the comma
                    textForNodes.get(parent).removeTextBetween(ASTParserConstants.COMMA, child);
                }
            default:
                textForNodes.get(parent).removeElementsForChild(child);
        }
    }

    private Separator[] separatorsAtStartList(NodeList nodeList) {
        switch (findNodeListName(nodeList)) {
            case "Members":
                return new Separator[]{Separator.NEWLINE, Separator.TAB};
            default:
                return new Separator[]{};
        }
    }

    private void updateTextBecauseOfAddedChild(NodeList nodeList, int index, Optional<Node> parentNode, Node child) {
        if (!parentNode.isPresent()) {
            return;
        }
        Node parent = parentNode.get();
        String nodeListName = findNodeListName(nodeList);

        if (index == 0) {
            Inserter inserter = getPositionFinder(parent.getClass(), nodeListName);
            inserter.insert(parent, /*separatorsAtStartList(nodeList),*/ child);
        } else {
            Inserter inserter = insertAfterChild(nodeList.get(index - 1), ", ");
            inserter.insert(parent, child);
        }
    }

    private String findNodeListName(NodeList nodeList) {
        Node parent = nodeList.getParentNodeForChildren();
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
                return insertAfter(ASTParserConstants.LBRACE, InsertionMode.ON_ITS_OWN_LINE);
            case "FieldDeclaration:Variables":
                try {
                    return insertAfterChild(FieldDeclaration.class.getMethod("getElementType"), Separator.SPACE);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            case "MethodDeclaration:Parameters":
                return insertAfter(ASTParserConstants.LPAREN, InsertionMode.PLAIN);
            case "BlockStmt:Stmts":
                return insertAfter(ASTParserConstants.LBRACE, InsertionMode.ON_ITS_OWN_LINE);
        }

        throw new UnsupportedOperationException(key);
    }

    private void printModifiers(NodeText nodeText, final EnumSet<Modifier> modifiers) {
        modifiers.forEach(m -> {
            nodeText.addToken(Separator.fromModifier(m));
            nodeText.addToken(Separator.SPACE);
        });
    }

    private NodeText prettyPrintingTextNode(Node node) {
        NodeText nodeText = new NodeText(this);
        if (node instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = (FieldDeclaration)node;
            nodeText.addList(fieldDeclaration.getAnnotations(), true, Separator.NEWLINE);
            printModifiers(nodeText, fieldDeclaration.getModifiers());
            nodeText.addChild(fieldDeclaration.getCommonType());
            //nodeText.addList(fieldDeclaration.getAr(), "", true);
            //nodeText.addString(" ");
            nodeText.addList(fieldDeclaration.getVariables(), false, Separator.COMMA, Separator.SPACE);
            nodeText.addToken(Separator.SEMICOLON);
            nodeText.addToken(Separator.NEWLINE);
            return nodeText;
        }
        throw new UnsupportedOperationException(node.getClass().getCanonicalName());
    }

    // Visible for testing
    NodeText getOrCreateNodeText(Node node) {
        if (!textForNodes.containsKey(node)) {
            textForNodes.put(node, prettyPrintingTextNode(node));
        }
        return textForNodes.get(node);
    }

    private Inserter insertAfterChild(Node childToFollow, String separatorBefore) {
        throw new UnsupportedOperationException();
//        return (parent, child) -> {
//            NodeText nodeText = getOrCreateNodeText(parent);
//            if (childToFollow == null) {
//                nodeText.addElement(0, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
//                return;
//            }
//            for (int i=0; i< nodeText.numberOfElements();i++) {
//                NodeTextElement element = nodeText.getTextElement(i);
//                if (element instanceof ChildNodeTextElement) {
//                    ChildNodeTextElement childElement = (ChildNodeTextElement)element;
//                    if (childElement.getChild() == childToFollow) {
//                        nodeText.addString(i+1, separatorBefore);
//                        nodeText.addElement(i+2, new ChildNodeTextElement(LexicalPreservingPrinter.this, child));
//                        return;
//                    }
//                }
//            }
//            throw new IllegalArgumentException();
//        };
    }

    enum Separator {
        COMMA(ASTParserConstants.COMMA, ","),
        SPACE(0, " "),
        SEMICOLON(ASTParserConstants.SEMICOLON, ";"),
        NEWLINE(0, "\n"),
        TAB(0, "    ");
        private String text;
        private int tokenKind;

        Separator(int tokenKind, String text) {
            this.text = text;
            this.tokenKind = tokenKind;
        }

        public String getText() {
            return text;
        }

        public int getTokenKind() {
            return tokenKind;
        }

        public static Separator fromModifier(Modifier modifier) {
            Separator separator = Separator.valueOf(modifier.name());
            if (separator == null) {
                throw new IllegalArgumentException(modifier.toString());
            }
            return separator;
        }
    }

    private Inserter insertAfterChild(Method method, Separator separator) {
        return (parent, child) -> {
            try {
                NodeText nodeText = getOrCreateNodeText(parent);
                Node childToFollow = (Node) method.invoke(parent);
                if (childToFollow == null) {
                    nodeText.addElement(0, new ChildTextElement(LexicalPreservingPrinter.this, child));
                    return;
                }
                for (int i=0; i< nodeText.numberOfElements();i++) {
                    TextElement element = nodeText.getTextElement(i);
                    if (element instanceof ChildTextElement) {
                        ChildTextElement childElement = (ChildTextElement)element;
                        if (childElement.getChild() == childToFollow) {
                            nodeText.addToken(separator.getTokenKind(), separator.getText());
                            nodeText.addElement(i+2, new ChildTextElement(LexicalPreservingPrinter.this, child));
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

    private enum InsertionMode {
        PLAIN,
        ON_ITS_OWN_LINE
    }

    private Inserter insertAfter(final int tokenKind, InsertionMode insertionMode) {
        return (parent, child) -> {
            NodeText nodeText = textForNodes.get(parent);
            for (int i=0; i< nodeText.numberOfElements();i++) {
                TextElement element = nodeText.getTextElement(i);
                if (element instanceof TokenTextElement) {
                    TokenTextElement tokenTextElement = (TokenTextElement)element;
                    if (tokenTextElement.getTokenKind() == tokenKind) {
                        int it = i+1;
                        if (insertionMode == InsertionMode.ON_ITS_OWN_LINE) {
                            nodeText.addToken(it++, Separator.NEWLINE);
                            nodeText.addToken(it++, Separator.TAB);
                        }
                        nodeText.addElement(it++, new ChildTextElement(LexicalPreservingPrinter.this, child));
                        if (insertionMode == InsertionMode.ON_ITS_OWN_LINE) {
                            nodeText.addToken(it++, Separator.NEWLINE);
                        }
                        return;
                    }
                }
            }
            throw new IllegalArgumentException("I could not find the token of type " + tokenKind);
        };
    }

    public static <T extends Node> LexicalPreservingPrinter setup(ParseResult<T> parseResult) {
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        AstObserver observer = createObserver(lpp);
        Node root = parseResult.getResult().get();

        List<JavaToken> documentTokens = parseResult.getTokens().get();
        Map<Node, List<JavaToken>> tokensByNode = new HashMap<>();

        // Take all nodes and sort them to get the leaves first
        List<Node> nodesDepthFirst = new LinkedList<>();
        root.onSubStreeDepthFirst(n -> nodesDepthFirst.add(n));

        for (JavaToken token : documentTokens) {
            Optional<Node> maybeOwner = nodesDepthFirst.stream().filter(n -> n.getRange().get().contains(token.getRange())).findFirst();
            Node owner = maybeOwner.get();
            if (!tokensByNode.containsKey(owner)) {
                tokensByNode.put(owner, new LinkedList<>());
            }
            tokensByNode.get(owner).add(token);
        }

        root.registerForSubtree(observer);
        root.onSubStree(n -> lpp.registerInitialText(n, tokensByNode.get(n)));
        return lpp;
    }

    private void registerInitialText(Node node, List<JavaToken> nodeTokens) {
        if (nodeTokens == null) {
            nodeTokens = Collections.emptyList();
        }
        List<Pair<Range, TextElement>> elements = new LinkedList<>();
        for (Node child : node.getChildNodes()) {
            elements.add(new Pair<>(child.getRange().get(), new ChildTextElement(this, child)));
        }
        for (JavaToken token : nodeTokens) {
            elements.add(new Pair<>(token.getRange(), new TokenTextElement(token)));
        }
        elements.sort((e1, e2) -> e1.a.begin.compareTo(e2.a.begin));
        textForNodes.put(node, new NodeText(this, elements.stream().map(p -> p.b).collect(Collectors.toList())));
    }

    private static AstObserver createObserver(LexicalPreservingPrinter lpp) {
        return new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                if (oldValue != null && oldValue.equals(newValue)) {
                    return;
                }
                if (true) throw new UnsupportedOperationException();
//                if (oldValue instanceof Node && newValue instanceof Node) {
//                    lpp.getTextForNode(observedNode).replaceChild((Node)oldValue, (Node)newValue);
//                    return;
//                }
//                if (oldValue == null && newValue instanceof Node) {
//                    if (property == ObservableProperty.INITIALIZER) {
//                        lpp.getOrCreateNodeText(observedNode).addString(" = ");
//                        lpp.getOrCreateNodeText(observedNode).addChild((Node)newValue);
//                        return;
//                    }
//                    throw new UnsupportedOperationException("Set property " + property);
//                }
//                if (oldValue instanceof Node && newValue == null) {
//                    if (property == ObservableProperty.INITIALIZER) {
//                        lpp.getOrCreateNodeText(observedNode).removeTextBetween("=", (Node)oldValue);
//                        lpp.getOrCreateNodeText(observedNode).removeElementsForChild((Node)oldValue);
//                        return;
//                    }
//                    throw new UnsupportedOperationException("Unset property " + property);
//                }
//                if ((oldValue instanceof EnumSet) && ObservableProperty.MODIFIERS == property){
//                    EnumSet<Modifier> oldEnumSet = (EnumSet<Modifier>)oldValue;
//                    EnumSet<Modifier> newEnumSet = (EnumSet<Modifier>)newValue;
//                    for (Modifier removedModifier : oldEnumSet.stream().filter(e -> !newEnumSet.contains(e)).collect(Collectors.toList())) {
//                        lpp.getOrCreateNodeText(observedNode).removeString(removedModifier.name().toLowerCase());
//                    }
//                    for (Modifier addedModifier : newEnumSet.stream().filter(e -> !oldEnumSet.contains(e)).collect(Collectors.toList())) {
//                        lpp.getOrCreateNodeText(observedNode).addAtBeginningString(addedModifier.name().toLowerCase() + " ");
//                    }
//                    return;
//                }
                if (property == ObservableProperty.RANGE) {
                    return;
                }
                throw new UnsupportedOperationException(String.format("Property %s. OLD %s (%s) NEW %s (%s)", property, oldValue,
                        oldValue == null ? "": oldValue.getClass(), newValue, newValue == null ? "": newValue.getClass()));
            }

            @Override
            public void concreteListChange(NodeList observedNode, ListChangeType type, int index, Node nodeAddedOrRemoved) {
                if (type == type.REMOVAL) {
                    lpp.updateTextBecauseOfRemovedChild(observedNode, index, observedNode.getParentNode(), nodeAddedOrRemoved);
                } else if (type == type.ADDITION) {
                    lpp.updateTextBecauseOfAddedChild(observedNode, index, observedNode.getParentNode(), nodeAddedOrRemoved);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };
    }
}
