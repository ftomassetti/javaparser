package com.github.javaparser.ast;

/**
 * This Node can represent a name, as defined in the JLS, Chapter 6.
 * This is not always the case as not all the AST classes have all their instances
 * being names or not names, see for example FieldAccessExpr.
 */
public interface NameNode<N extends Node> {

    /**
     * Return true if this instance represents a proper name.
     */
    boolean isAName();

    default boolean isSimpleName() {
        return !isQualifiedName();
    }

    default boolean isQualifiedName() {
        if (!isAName()) {
            throw new IllegalArgumentException();
        }
        return asString().contains(".");
    }

    String asString();

    default Node asNode() {
        return (Node)this;
    }

}
