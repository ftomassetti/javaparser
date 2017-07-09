/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package com.github.javaparser.printer;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;
import java.util.*;
import java.util.stream.Collectors;
import static com.github.javaparser.ast.Node.Parsedness.UNPARSABLE;
import static com.github.javaparser.utils.PositionUtils.sortByBeginPosition;
import static com.github.javaparser.utils.Utils.isNullOrEmpty;
import javax.annotation.Generated;

/**
 * Outputs the AST as formatted Java source code.
 *
 * @author Julio Vilmar Gesser
 */
public abstract class PrettyPrintVisitor_REMOVEME implements VoidVisitor<Void> {

    protected final PrettyPrinterConfiguration configuration;

    protected final SourcePrinter printer;

    public PrettyPrintVisitor_REMOVEME(PrettyPrinterConfiguration prettyPrinterConfiguration) {
        configuration = prettyPrinterConfiguration;
        printer = new SourcePrinter(configuration.getIndent(), configuration.getEndOfLineCharacter());
    }

    public String getSource() {
        return printer.getSource();
    }

    private void printModifiers(final EnumSet<Modifier> modifiers) {
        if (modifiers.size() > 0) {
            printer.print(modifiers.stream().map(Modifier::asString).collect(Collectors.joining(" ")) + " ");
        }
    }

    private void printMembers(final NodeList<BodyDeclaration<?>> members, final Void arg) {
        for (final BodyDeclaration<?> member : members) {
            printer.println();
            member.accept(this, arg);
            printer.println();
        }
    }

    private void printMemberAnnotations(final NodeList<AnnotationExpr> annotations, final Void arg) {
        if (annotations.isEmpty()) {
            return;
        }
        for (final AnnotationExpr a : annotations) {
            a.accept(this, arg);
            printer.println();
        }
    }

    private void printAnnotations(final NodeList<AnnotationExpr> annotations, boolean prefixWithASpace, final Void arg) {
        if (annotations.isEmpty()) {
            return;
        }
        if (prefixWithASpace) {
            printer.print(" ");
        }
        for (AnnotationExpr annotation : annotations) {
            annotation.accept(this, arg);
            printer.print(" ");
        }
    }

    private void printTypeArgs(final NodeWithTypeArguments<?> nodeWithTypeArguments, final Void arg) {
        NodeList<Type> typeArguments = nodeWithTypeArguments.getTypeArguments().orElse(null);
        if (!isNullOrEmpty(typeArguments)) {
            printer.print("<");
            for (final Iterator<Type> i = typeArguments.iterator(); i.hasNext(); ) {
                final Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printTypeParameters(final NodeList<TypeParameter> args, final Void arg) {
        if (!isNullOrEmpty(args)) {
            printer.print("<");
            for (final Iterator<TypeParameter> i = args.iterator(); i.hasNext(); ) {
                final TypeParameter t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printArguments(final NodeList<Expression> args, final Void arg) {
        printer.print("(");
        if (!isNullOrEmpty(args)) {
            for (final Iterator<Expression> i = args.iterator(); i.hasNext(); ) {
                final Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    private void printPrePostFixOptionalList(final NodeList<? extends Visitable> args, final Void arg, String prefix, String separator, String postfix) {
        if (!args.isEmpty()) {
            printer.print(prefix);
            for (final Iterator<? extends Visitable> i = args.iterator(); i.hasNext(); ) {
                final Visitable v = i.next();
                v.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(separator);
                }
            }
            printer.print(postfix);
        }
    }

    private void printPrePostFixRequiredList(final NodeList<? extends Visitable> args, final Void arg, String prefix, String separator, String postfix) {
        printer.print(prefix);
        if (!args.isEmpty()) {
            for (final Iterator<? extends Visitable> i = args.iterator(); i.hasNext(); ) {
                final Visitable v = i.next();
                v.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(separator);
                }
            }
        }
        printer.print(postfix);
    }

    private void printJavaComment(final Optional<Comment> javacomment, final Void arg) {
        javacomment.ifPresent(c -> c.accept(this, arg));
    }

    private void printOrphanCommentsBeforeThisChildNode(final Node node) {
        if (node instanceof Comment)
            return;
        Node parent = node.getParentNode().orElse(null);
        if (parent == null)
            return;
        List<Node> everything = new LinkedList<>();
        everything.addAll(parent.getChildNodes());
        sortByBeginPosition(everything);
        int positionOfTheChild = -1;
        for (int i = 0; i < everything.size(); i++) {
            if (everything.get(i) == node)
                positionOfTheChild = i;
        }
        if (positionOfTheChild == -1) {
            throw new AssertionError("I am not a child of my parent.");
        }
        int positionOfPreviousChild = -1;
        for (int i = positionOfTheChild - 1; i >= 0 && positionOfPreviousChild == -1; i--) {
            if (!(everything.get(i) instanceof Comment))
                positionOfPreviousChild = i;
        }
        for (int i = positionOfPreviousChild + 1; i < positionOfTheChild; i++) {
            Node nodeToPrint = everything.get(i);
            if (!(nodeToPrint instanceof Comment))
                throw new RuntimeException("Expected comment, instead " + nodeToPrint.getClass() + ". Position of previous child: " + positionOfPreviousChild + ", position of child " + positionOfTheChild);
            nodeToPrint.accept(this, null);
        }
    }

    private void printOrphanCommentsEnding(final Node node) {
        List<Node> everything = new LinkedList<>();
        everything.addAll(node.getChildNodes());
        sortByBeginPosition(everything);
        if (everything.isEmpty()) {
            return;
        }
        int commentsAtEnd = 0;
        boolean findingComments = true;
        while (findingComments && commentsAtEnd < everything.size()) {
            Node last = everything.get(everything.size() - 1 - commentsAtEnd);
            findingComments = (last instanceof Comment);
            if (findingComments) {
                commentsAtEnd++;
            }
        }
        for (int i = 0; i < commentsAtEnd; i++) {
            everything.get(everything.size() - commentsAtEnd + i).accept(this, null);
        }
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@386f9a54
    void visit(AnnotationDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        printer.print("interface");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.print("\n");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@2d2380a
    void visit(AnnotationMemberDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@37cac7d8
    void visit(ArrayAccessExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@39dc4692
    void visit(ArrayCreationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("new");
        printer.print(" ");
        n.getElementType().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7da5c6c7
    void visit(ArrayCreationLevel n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("[");
        if (n.getDimension().isPresent()) {
            n.getDimension().get().accept(this, arg);
        }
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@6ea4a311
    void visit(ArrayInitializerExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("{");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@f314d1a
    void visit(ArrayType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getComponentType().accept(this, arg);
        printer.print("[");
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@540262b8
    void visit(AssertStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("assert");
        printer.print(" ");
        n.getCheck().accept(this, arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7420501
    void visit(AssignExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getTarget().accept(this, arg);
        printer.print(" ");
        printer.print(n.getOperator().asString());
        printer.print(" ");
        n.getValue().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@4c3582d3
    void visit(BinaryExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getLeft().accept(this, arg);
        printer.print(" ");
        printer.print(n.getOperator().asString());
        printer.print(" ");
        n.getRight().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BlockComment n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@49694fd2
    void visit(BlockStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("{");
        printer.print("\n");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@e39f3e5
    void visit(BooleanLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(String.valueOf(n.getValue()));
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@19c4952e
    void visit(BreakStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("break");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7907bfdf
    void visit(CastExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("(");
        n.getType().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        n.getExpression().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7a1378f2
    void visit(CatchClause n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(" ");
        printer.print("catch");
        printer.print(" ");
        printer.print("(");
        n.getParameter().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@3b740a8e
    void visit(CharLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@6a4952b
    void visit(ClassExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(".");
        printer.print("class");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@42192b67
    void visit(ClassOrInterfaceDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.print("\n");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@232c1f3e
    void visit(ClassOrInterfaceType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@381751af
    void visit(CompilationUnit n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getPackageDeclaration().isPresent()) {
            n.getPackageDeclaration().get().accept(this, arg);
        }
        if (n.getModule().isPresent()) {
            n.getModule().get().accept(this, arg);
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1a38864b
    void visit(ConditionalExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getCondition().accept(this, arg);
        printer.print(" ");
        printer.print("?");
        printer.print(" ");
        n.getThenExpr().accept(this, arg);
        printer.print(" ");
        printer.print(":");
        printer.print(" ");
        n.getElseExpr().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@11f1c478
    void visit(ConstructorDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@52e7fc97
    void visit(ContinueStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("continue");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@507f554d
    void visit(DoStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("do");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printer.print(" ");
        printer.print("while");
        printer.print(" ");
        printer.print("(");
        n.getCondition().accept(this, arg);
        printer.print(")");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@e2b9bd7
    void visit(DoubleLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@11665c6e
    void visit(EmptyStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7c881991
    void visit(EnclosedExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("(");
        if (n.getInner().isPresent()) {
            n.getInner().get().accept(this, arg);
        }
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5677ee70
    void visit(EnumConstantDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@450224a
    void visit(EnumDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("enum");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.print("\n");
        printer.print("\n");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@27f68b82
    void visit(ExplicitConstructorInvocationStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("(");
        printer.print(")");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1b0e5a7c
    void visit(ExpressionStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@72632303
    void visit(FieldAccessExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        printer.print(".");
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@77a40d77
    void visit(FieldDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(" ");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@649d329a
    void visit(ForStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("for");
        printer.print(" ");
        printer.print("(");
        printer.print(";");
        printer.print(" ");
        if (n.getCompare().isPresent()) {
            n.getCompare().get().accept(this, arg);
        }
        printer.print(";");
        printer.print(" ");
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@da949cf
    void visit(ForeachStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("for");
        printer.print(" ");
        printer.print("(");
        n.getVariable().accept(this, arg);
        printer.print(" ");
        printer.print(":");
        printer.print(" ");
        n.getIterable().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@66390842
    void visit(IfStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("if");
        printer.print(" ");
        printer.print("(");
        n.getCondition().accept(this, arg);
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@31a16e47
    void visit(ImportDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("import");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@3ca5930b
    void visit(InitializerDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7b055261
    void visit(InstanceOfExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printer.print(" ");
        printer.print("instanceof");
        printer.print(" ");
        n.getType().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@55502e5b
    void visit(IntegerLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@6f39b1e0
    void visit(IntersectionType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(JavadocComment n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7f944ac7
    void visit(LabeledStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getLabel().accept(this, arg);
        printer.print(":");
        printer.print(" ");
        n.getStatement().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@2ed3990e
    void visit(LambdaExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(" ");
        printer.print("->");
        printer.print(" ");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LineComment n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7aa54879
    void visit(LocalClassDeclarationStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getClassDeclaration().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@58b1407c
    void visit(LongLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@52eebdf
    void visit(MarkerAnnotationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        printer.print(n.getName());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@3787099d
    void visit(MemberValuePair n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("=");
        printer.print(" ");
        n.getValue().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@67316994
    void visit(MethodCallExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@35d177ae
    void visit(MethodDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@475312d8
    void visit(MethodReferenceExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        printer.print("::");
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@4897204e
    void visit(ModuleDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("module");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.print("\n");
        printer.print("}");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@24068d01
    void visit(ModuleExportsStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("exports");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1ad4e228
    void visit(ModuleOpensStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("opens");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@454ee678
    void visit(ModuleProvidesStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("provides");
        printer.print(" ");
        n.getType().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@34aa3bcf
    void visit(ModuleRequiresStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("requires");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@24d4c6de
    void visit(ModuleUsesStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("uses");
        printer.print(" ");
        n.getType().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@192cb5ce
    void visit(NameExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@2be3fdb5
    void visit(Name n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@4e72cf45
    void visit(NormalAnnotationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@4c920263
    void visit(NullLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("null");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@30ec5cfa
    void visit(ObjectCreationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("new");
        printer.print(" ");
        n.getType().accept(this, arg);
        printer.print("(");
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@4d60107
    void visit(PackageDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("package");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.print("\n");
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5c86ee3f
    void visit(Parameter n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5b7ed6
    void visit(PrimitiveType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getType().asString());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5202f9ab
    void visit(ReturnStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("return");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmAttribute@666814d5
    void visit(SimpleName n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@df4e931
    void visit(SingleMemberAnnotationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        n.getMemberValue().accept(this, arg);
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@56829b97
    void visit(StringLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1308ff4d
    void visit(SuperExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("super");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@35608f45
    void visit(SwitchEntryStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("\n");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1ff745b9
    void visit(SwitchStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("switch");
        printer.print("(");
        n.getSelector().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        printer.print("{");
        printer.print("\n");
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5deb2bf3
    void visit(SynchronizedStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("synchronized");
        printer.print(" ");
        printer.print("(");
        n.getExpression().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@94d5594
    void visit(ThisExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("this");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@38e6f95f
    void visit(ThrowStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("throw");
        printer.print(" ");
        n.getExpression().accept(this, arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1d19a977
    void visit(TryStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("try");
        printer.print(" ");
        if (n.getTryBlock().isPresent()) {
            n.getTryBlock().get().accept(this, arg);
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@1929ad94
    void visit(TypeExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7a2baad0
    void visit(TypeParameter n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@750888cb
    void visit(UnaryExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@258ac50c
    void visit(UnionType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmNone@711b9924
    void visit(UnknownType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@5641c0fd
    void visit(UnparsableStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@673e542e
    void visit(VariableDeclarationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getMaximumCommonType().accept(this, arg);
        printer.print(" ");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@146f5f60
    void visit(VariableDeclarator n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@39102670
    void visit(VoidType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("void");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@610e4f87
    void visit(WhileStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("while");
        printer.print(" ");
        printer.print("(");
        n.getCondition().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public //com.github.javaparser.printer.concretesyntaxmodel.CsmSequence@7404f8ac
    void visit(WildcardType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("?");
        printOrphanCommentsEnding(n);
    }
}
