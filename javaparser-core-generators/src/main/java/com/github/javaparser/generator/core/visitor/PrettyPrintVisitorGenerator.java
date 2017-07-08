package com.github.javaparser.generator.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.printer.concretesyntaxmodel.CsmElement;
import com.github.javaparser.printer.concretesyntaxmodel.CsmSequence;
import com.github.javaparser.printer.concretesyntaxmodel.CsmSingleReference;
import com.github.javaparser.printer.concretesyntaxmodel.CsmToken;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.StringEscapeUtils;
import com.github.javaparser.utils.Utils;

import java.util.Optional;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

/**
 * Generates JavaParser's PrettyPrintVisitor.
 */
public class PrettyPrintVisitorGenerator extends VisitorGenerator {
    public PrettyPrintVisitorGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.printer", "PrettyPrintVisitor_REMOVEME", "void", "Void", true);
    }

    private void processCsmElement(BaseNodeMetaModel node, BlockStmt body, CsmElement csmElement) {
        if (csmElement instanceof CsmSequence) {
            CsmSequence csmSequence = (CsmSequence) csmElement;
            csmSequence.getElements().forEach(e -> processCsmElement(node, body, e));
        } else if (csmElement instanceof CsmToken) {
            CsmToken csmToken = (CsmToken) csmElement;
            body.addStatement("printer.print(\"" + StringEscapeUtils.escapeJava(csmToken.getContent(null)) + "\");");
        } else if (csmElement instanceof CsmSingleReference) {
            CsmSingleReference csmSingleReference = (CsmSingleReference) csmElement;
            String getterName = "get" + Utils.capitalize(csmSingleReference.getProperty().camelCaseName());
            try {
                boolean optional = node.getType().getMethod(getterName).getReturnType().getCanonicalName().equals(Optional.class.getCanonicalName());
                if (optional) {
                    body.addStatement("if (n." + getterName + " ().isPresent()) { n." + getterName + " ().get().accept(this, arg); }");
                } else {
                    body.addStatement("n." + getterName + " ().accept(this, arg);");
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else {
            //
            System.out.println("IGNORING " + csmElement);
        }
    }

    @Override
    protected void generateVisitMethodBody(BaseNodeMetaModel node, MethodDeclaration visitMethod, CompilationUnit compilationUnit) {
        BlockStmt body = visitMethod.getBody().get();
        body.getStatements().clear();

        body.addStatement("printJavaComment(n.getComment(), arg);");
        if (!Comment.class.isAssignableFrom(node.getType())) {
            CsmElement csmElement = ConcreteSyntaxModel.forClass(node.getType());
            processCsmElement(node, body, csmElement);
        }

        body.addStatement("printOrphanCommentsEnding(n);");
    }
}
