package com.github.javaparser.generator.core.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.generator.VisitorGenerator;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.printer.concretesyntaxmodel.*;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.utils.StringEscapeUtils;
import com.github.javaparser.utils.Utils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static com.github.javaparser.utils.CodeGenerationUtils.f;

/**
 * Generates JavaParser's PrettyPrintVisitor.
 */
public class PrettyPrintVisitorGenerator extends VisitorGenerator {
    public PrettyPrintVisitorGenerator(SourceRoot sourceRoot) {
        super(sourceRoot, "com.github.javaparser.printer", "PrettyPrintVisitor_REMOVEME", "void", "Void", true);
    }

    private Class elementType(ParameterizedType parameterizedType) {
        if (parameterizedType.getRawType().getTypeName().equals(Optional.class.getCanonicalName())) {
            return elementType((ParameterizedType)parameterizedType.getActualTypeArguments()[0]);
        } else {
            java.lang.reflect.Type listElementType = parameterizedType.getActualTypeArguments()[0];
            if (listElementType instanceof Class) {
                return ((Class) listElementType);
            } else if (listElementType instanceof ParameterizedType) {
                return (Class) ((ParameterizedType) listElementType).getRawType();
            } else {
                throw new IllegalArgumentException(parameterizedType.toString());
            }
        }
    }

    private String elementTypeName(ParameterizedType parameterizedType) {
        return elementType(parameterizedType).getCanonicalName();
    }

    private boolean isOptional(ParameterizedType parameterizedType) {
        return parameterizedType.getRawType().getTypeName().equals(Optional.class.getCanonicalName());
    }

    private void processCsmElement(BaseNodeMetaModel node, BlockStmt body, CsmElement csmElement) {
        try {
            if (csmElement instanceof CsmSequence) {
                CsmSequence csmSequence = (CsmSequence) csmElement;
                csmSequence.getElements().forEach(e -> processCsmElement(node, body, e));
            } else if (csmElement instanceof CsmToken) {
                CsmToken csmToken = (CsmToken) csmElement;
                String content = StringEscapeUtils.escapeJava(csmToken.getContent(null));
                if (content.equals("\\n")) {
                    body.addStatement("printer.println();");
                } else {
                    body.addStatement("printer.print(\"" + StringEscapeUtils.escapeJava(csmToken.getContent(null)) + "\");");
                }
            } else if (csmElement instanceof CsmSingleReference) {
                CsmSingleReference csmSingleReference = (CsmSingleReference) csmElement;
                String getterName = "get" + Utils.capitalize(csmSingleReference.getProperty().camelCaseName());

                    boolean optional = node.getType().getMethod(getterName).getReturnType().getCanonicalName().equals(Optional.class.getCanonicalName());
                    if (optional) {
                        body.addStatement("if (n." + getterName + " ().isPresent()) { n." + getterName + " ().get().accept(this, arg); }");
                    } else {
                        body.addStatement("n." + getterName + " ().accept(this, arg);");
                    }

            } else if (csmElement instanceof CsmAttribute) {
                CsmAttribute csmAttribute = (CsmAttribute)csmElement;
                // If the thing has a method asString let's call it
                String getterName = "get" + Utils.capitalize(csmAttribute.getProperty().camelCaseName());
                if (node.getType().equals(BooleanLiteralExpr.class)) {
                    body.addStatement("printer.print(String.valueOf(n.getValue()));");
                } else if (csmAttribute.getProperty() == ObservableProperty.OPERATOR || node.getType().equals(PrimitiveType.class)) {
                    body.addStatement("printer.print(n." + getterName + "().asString());");
                } else {
                    body.addStatement("printer.print(n." + getterName + "());");
                }
            } else if (csmElement instanceof CsmComment) {
                // nothing to do
            } else if (csmElement instanceof CsmNone) {
                // nothing to do
            } else if (csmElement instanceof CsmList) {
                CsmList csmList = (CsmList) csmElement;
                String getterName = "get" + Utils.capitalize(csmList.getProperty().camelCaseName());
                Method getter = node.getType().getMethod(getterName);
                boolean option = isOptional((ParameterizedType) getter.getGenericReturnType());
                if (!csmList.getPreceeding().isNone()) {
                    IfStmt ifStmt = new IfStmt();
                    if (option) {
                        ifStmt.setCondition(JavaParser.parseExpression("n." + getterName + "().isPresent() && !n." + getterName + "().get().isEmpty()"));
                    } else {
                        ifStmt.setCondition(JavaParser.parseExpression("!n." + getterName + "().isEmpty()"));
                    }
                    BlockStmt ifBody = new BlockStmt();
                    processCsmElement(node, ifBody, ((CsmList) csmElement).getPreceeding());
                    ifStmt.setThenStmt(ifBody);
                    body.addStatement(ifStmt);
                }
                String countName = csmList.getProperty().camelCaseName()+"Count";
                body.addStatement("int " + countName + " = 0;");
                ForeachStmt loop = new ForeachStmt();
                String iteratorName = ((CsmList) csmElement).getProperty().camelCaseName()+"Item";
                ParameterizedType parameterizedType = (ParameterizedType)getter.getGenericReturnType();
                Class elementType = elementType(parameterizedType);
                loop.setVariable(new VariableDeclarationExpr(new ClassOrInterfaceType(elementType.getCanonicalName()), iteratorName));

                String getListExpr = "n."+ getterName+"()" + (option?".get()":"");
                loop.setIterable(JavaParser.parseExpression(getListExpr));
                BlockStmt loopBody = new BlockStmt();
                if (!csmList.getSeparatorPre().isNone()) {
                    IfStmt preSepIf = new IfStmt();
                    preSepIf.setCondition(JavaParser.parseExpression(countName + " != 0"));
                    BlockStmt preSepBody = new BlockStmt();
                    processCsmElement(node, preSepBody, ((CsmList) csmElement).getSeparatorPre());
                    preSepIf.setThenStmt(preSepBody);
                    loopBody.addStatement(preSepIf);
                }
                if (Node.class.isAssignableFrom(elementType)) {
                    loopBody.addStatement(iteratorName + ".accept(this, arg);");
                } else {
                    loopBody.addStatement("printer.print(" + iteratorName + ".asString());");
                }
                if (!csmList.getSeparatorPost().isNone()) {
                    IfStmt postSepIf = new IfStmt();
                    postSepIf.setCondition(JavaParser.parseExpression(countName + " != "+getListExpr+ ".size() - 1"));
                    BlockStmt postSepBody = new BlockStmt();
                    processCsmElement(node, postSepBody, ((CsmList) csmElement).getSeparatorPost());
                    postSepIf.setThenStmt(postSepBody);
                    loopBody.addStatement(postSepIf);
                }
                loopBody.addStatement(countName + "++;");
                loop.setBody(loopBody);
                body.addStatement(loop);
                if (!csmList.getFollowing().isNone()) {
                    IfStmt ifStmt = new IfStmt();
                    if (option) {
                        ifStmt.setCondition(JavaParser.parseExpression("n." + getterName + "().isPresent() && !n." + getterName + "().get().isEmpty()"));
                    } else {
                        ifStmt.setCondition(JavaParser.parseExpression("!n." + getterName + "().isEmpty()"));
                    }
                    BlockStmt ifBody = new BlockStmt();
                    processCsmElement(node, ifBody, ((CsmList) csmElement).getFollowing());
                    ifStmt.setThenStmt(ifBody);
                    body.addStatement(ifStmt);
                }
            } else {
                //
                System.out.println("IGNORING " + csmElement);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
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
