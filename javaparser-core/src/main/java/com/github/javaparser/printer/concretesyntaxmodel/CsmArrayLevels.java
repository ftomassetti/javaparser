package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.printer.SourcePrinter;

public class CsmArrayLevels implements CsmElement {
    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        Type commonType = node.getAncestorOfType(NodeWithVariables.class).get().getMaximumCommonType();
        Type type = ((VariableDeclarator)node).getType();
        ArrayType arrayType = null;

        for (int i = commonType.getArrayLevel(); i < type.getArrayLevel(); i++) {
            if (arrayType == null) {
                arrayType = (ArrayType) type;
            } else {
                arrayType = (ArrayType) arrayType.getComponentType();
            }
            printAnnotations(arrayType.getAnnotations(), true, printer);
            printer.print("[]");
        }
    }

    private void printAnnotations(final NodeList<AnnotationExpr> annotations, boolean prefixWithASpace, SourcePrinter printer) {
        if (annotations.isEmpty()) {
            return;
        }
        if (prefixWithASpace) {
            printer.print(" ");
        }
        for (AnnotationExpr annotation : annotations) {
            ConcreteSyntaxModel.genericPrettyPrint(annotation, printer);
            printer.print(" ");
        }
    }
}
