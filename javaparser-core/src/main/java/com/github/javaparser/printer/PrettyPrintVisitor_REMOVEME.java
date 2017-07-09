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
    public void visit(AnnotationDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        printer.print("@");
        printer.print("interface");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.println();
        printer.indent();
        int membersCount = 0;
        for (com.github.javaparser.ast.body.BodyDeclaration membersItem : n.getMembers()) {
            if (membersCount != 0) {
                printer.println();
            }
            membersItem.accept(this, arg);
            membersCount++;
        }
        if (!n.getMembers().isEmpty()) {
            printer.println();
        }
        printer.unindent();
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AnnotationMemberDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print("(");
        printer.print(")");
        if (n.getDefaultValue().isPresent()) {
            printer.print(" ");
            printer.print("default");
            printer.print(" ");
            if (n.getDefaultValue().isPresent()) {
                n.getDefaultValue().get().accept(this, arg);
            }
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayAccessExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayCreationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("new");
        printer.print(" ");
        n.getElementType().accept(this, arg);
        int levelsCount = 0;
        for (com.github.javaparser.ast.ArrayCreationLevel levelsItem : n.getLevels()) {
            levelsItem.accept(this, arg);
            levelsCount++;
        }
        if (n.getInitializer().isPresent()) {
            printer.print(" ");
            if (n.getInitializer().isPresent()) {
                n.getInitializer().get().accept(this, arg);
            }
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayCreationLevel n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        printer.print("[");
        if (n.getDimension().isPresent()) {
            n.getDimension().get().accept(this, arg);
        }
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayInitializerExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("{");
        if (!n.getValues().isEmpty()) {
            printer.print(" ");
        }
        int valuesCount = 0;
        for (com.github.javaparser.ast.expr.Expression valuesItem : n.getValues()) {
            valuesItem.accept(this, arg);
            if (valuesCount != n.getValues().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            valuesCount++;
        }
        if (!n.getValues().isEmpty()) {
            printer.print(" ");
        }
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getComponentType().accept(this, arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        printer.print("[");
        printer.print("]");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AssertStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("assert");
        printer.print(" ");
        n.getCheck().accept(this, arg);
        if (n.getMessage().isPresent()) {
            printer.print(" ");
            printer.print(":");
            printer.print(" ");
            if (n.getMessage().isPresent()) {
                n.getMessage().get().accept(this, arg);
            }
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AssignExpr n, Void arg) {
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
    public void visit(BinaryExpr n, Void arg) {
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
    public void visit(BlockStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("{");
        printer.println();
        if (!n.getStatements().isEmpty()) {
            printer.indent();
        }
        int statementsCount = 0;
        for (com.github.javaparser.ast.stmt.Statement statementsItem : n.getStatements()) {
            statementsItem.accept(this, arg);
            if (statementsCount != n.getStatements().size() - 1) {
                printer.println();
            }
            statementsCount++;
        }
        if (!n.getStatements().isEmpty()) {
            printer.println();
            printer.unindent();
        }
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BooleanLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(String.valueOf(n.getValue()));
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BreakStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("break");
        if (n.getLabel().isPresent()) {
            printer.print(" ");
            if (n.getLabel().isPresent()) {
                n.getLabel().get().accept(this, arg);
            }
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CastExpr n, Void arg) {
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
    public void visit(CatchClause n, Void arg) {
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
    public void visit(CharLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printer.print(".");
        printer.print("class");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.println();
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        if (n.isInterface()) {
            printer.print("interface");
        } else {
            printer.print("class");
        }
        printer.print(" ");
        n.getName().accept(this, arg);
        if (!n.getTypeParameters().isEmpty()) {
            printer.print("<");
        }
        int typeParametersCount = 0;
        for (com.github.javaparser.ast.type.TypeParameter typeParametersItem : n.getTypeParameters()) {
            typeParametersItem.accept(this, arg);
            if (typeParametersCount != n.getTypeParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeParametersCount++;
        }
        if (!n.getTypeParameters().isEmpty()) {
            printer.print(">");
        }
        if (!n.getExtendedTypes().isEmpty()) {
            printer.print(" ");
            printer.print("extends");
            printer.print(" ");
        }
        int extendedTypesCount = 0;
        for (com.github.javaparser.ast.type.ClassOrInterfaceType extendedTypesItem : n.getExtendedTypes()) {
            extendedTypesItem.accept(this, arg);
            if (extendedTypesCount != n.getExtendedTypes().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            extendedTypesCount++;
        }
        if (!n.getImplementedTypes().isEmpty()) {
            printer.print(" ");
            printer.print("implements");
            printer.print(" ");
        }
        int implementedTypesCount = 0;
        for (com.github.javaparser.ast.type.ClassOrInterfaceType implementedTypesItem : n.getImplementedTypes()) {
            implementedTypesItem.accept(this, arg);
            if (implementedTypesCount != n.getImplementedTypes().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            implementedTypesCount++;
        }
        printer.print(" ");
        printer.print("{");
        printer.indent();
        printer.println();
        if (!n.getMembers().isEmpty()) {
            printer.println();
        }
        int membersCount = 0;
        for (com.github.javaparser.ast.body.BodyDeclaration membersItem : n.getMembers()) {
            membersItem.accept(this, arg);
            if (membersCount != n.getMembers().size() - 1) {
                printer.println();
                printer.println();
            }
            membersCount++;
        }
        if (!n.getMembers().isEmpty()) {
            printer.println();
        }
        printer.unindent();
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassOrInterfaceType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            if (n.getScope().isPresent()) {
                n.getScope().get().accept(this, arg);
            }
            printer.print(".");
        }
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        n.getName().accept(this, arg);
        if (n.isUsingDiamondOperator()) {
            printer.print("<");
            printer.print(">");
        } else {
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print("<");
            }
            int typeArgumentsCount = 0;
            for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
                typeArgumentsItem.accept(this, arg);
                if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                    printer.print(",");
                    printer.print(" ");
                }
                typeArgumentsCount++;
            }
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print(">");
            }
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CompilationUnit n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getPackageDeclaration().isPresent()) {
            n.getPackageDeclaration().get().accept(this, arg);
        }
        int importsCount = 0;
        for (com.github.javaparser.ast.ImportDeclaration importsItem : n.getImports()) {
            importsItem.accept(this, arg);
            importsCount++;
        }
        if (!n.getImports().isEmpty()) {
            printer.println();
        }
        int typesCount = 0;
        for (com.github.javaparser.ast.body.TypeDeclaration typesItem : n.getTypes()) {
            if (typesCount != 0) {
                printer.println();
            }
            typesItem.accept(this, arg);
            if (typesCount != n.getTypes().size() - 1) {
                printer.println();
            }
            typesCount++;
        }
        if (!n.getTypes().isEmpty()) {
            printer.println();
        }
        if (n.getModule().isPresent()) {
            n.getModule().get().accept(this, arg);
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ConditionalExpr n, Void arg) {
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
    public void visit(ConstructorDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        if (!n.getTypeParameters().isEmpty()) {
            printer.print("<");
        }
        int typeParametersCount = 0;
        for (com.github.javaparser.ast.type.TypeParameter typeParametersItem : n.getTypeParameters()) {
            typeParametersItem.accept(this, arg);
            if (typeParametersCount != n.getTypeParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeParametersCount++;
        }
        if (!n.getTypeParameters().isEmpty()) {
            printer.print(">");
            printer.print(" ");
        }
        n.getName().accept(this, arg);
        printer.print("(");
        int parametersCount = 0;
        for (com.github.javaparser.ast.body.Parameter parametersItem : n.getParameters()) {
            parametersItem.accept(this, arg);
            if (parametersCount != n.getParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            parametersCount++;
        }
        printer.print(")");
        if (!n.getThrownExceptions().isEmpty()) {
            printer.print(" ");
            printer.print("throws");
            printer.print(" ");
        }
        int thrownExceptionsCount = 0;
        for (com.github.javaparser.ast.type.ReferenceType thrownExceptionsItem : n.getThrownExceptions()) {
            thrownExceptionsItem.accept(this, arg);
            if (thrownExceptionsCount != n.getThrownExceptions().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            thrownExceptionsCount++;
        }
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ContinueStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("continue");
        if (n.getLabel().isPresent()) {
            printer.print(" ");
            if (n.getLabel().isPresent()) {
                n.getLabel().get().accept(this, arg);
            }
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(DoStmt n, Void arg) {
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
    public void visit(DoubleLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EmptyStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EnclosedExpr n, Void arg) {
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
    public void visit(EnumConstantDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        n.getName().accept(this, arg);
        if (!n.getArguments().isEmpty()) {
            printer.print("(");
        }
        int argumentsCount = 0;
        for (com.github.javaparser.ast.expr.Expression argumentsItem : n.getArguments()) {
            argumentsItem.accept(this, arg);
            if (argumentsCount != n.getArguments().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            argumentsCount++;
        }
        if (!n.getArguments().isEmpty()) {
            printer.print(")");
        }
        if (!n.getClassBody().isEmpty()) {
            printer.print(" ");
            printer.print("{");
            printer.println();
            printer.indent();
            printer.println();
            int classBodyCount = 0;
            for (com.github.javaparser.ast.body.BodyDeclaration classBodyItem : n.getClassBody()) {
                if (classBodyCount != 0) {
                    printer.println();
                }
                classBodyItem.accept(this, arg);
                if (classBodyCount != n.getClassBody().size() - 1) {
                    printer.println();
                }
                classBodyCount++;
            }
            if (!n.getClassBody().isEmpty()) {
                printer.println();
            }
            printer.unindent();
            printer.print("}");
            printer.println();
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EnumDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        printer.print("enum");
        printer.print(" ");
        n.getName().accept(this, arg);
        if (!n.getImplementedTypes().isEmpty()) {
            printer.print(" ");
            printer.print("implements");
            printer.print(" ");
        }
        int implementedTypesCount = 0;
        for (com.github.javaparser.ast.type.ClassOrInterfaceType implementedTypesItem : n.getImplementedTypes()) {
            implementedTypesItem.accept(this, arg);
            if (implementedTypesCount != n.getImplementedTypes().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            implementedTypesCount++;
        }
        printer.print(" ");
        printer.print("{");
        printer.println();
        printer.indent();
        printer.println();
        int entriesCount = 0;
        for (com.github.javaparser.ast.body.EnumConstantDeclaration entriesItem : n.getEntries()) {
            entriesItem.accept(this, arg);
            if (entriesCount != n.getEntries().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            entriesCount++;
        }
        if (n.getMembers().isEmpty()) {
            if (!n.getEntries().isEmpty()) {
                printer.println();
            }
        } else {
            printer.print(";");
            printer.println();
            printer.println();
            int membersCount = 0;
            for (com.github.javaparser.ast.body.BodyDeclaration membersItem : n.getMembers()) {
                if (membersCount != 0) {
                    printer.println();
                }
                membersItem.accept(this, arg);
                if (membersCount != n.getMembers().size() - 1) {
                    printer.println();
                }
                membersCount++;
            }
            if (!n.getMembers().isEmpty()) {
                printer.println();
            }
        }
        printer.unindent();
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isThis()) {
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print("<");
            }
            int typeArgumentsCount = 0;
            for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
                typeArgumentsItem.accept(this, arg);
                if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                    printer.print(",");
                    printer.print(" ");
                }
                typeArgumentsCount++;
            }
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print(">");
            }
            printer.print("this");
        } else {
            if (n.getExpression().isPresent()) {
                if (n.getExpression().isPresent()) {
                    n.getExpression().get().accept(this, arg);
                }
                printer.print(".");
            }
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print("<");
            }
            int typeArgumentsCount = 0;
            for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
                typeArgumentsItem.accept(this, arg);
                if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                    printer.print(",");
                    printer.print(" ");
                }
                typeArgumentsCount++;
            }
            if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
                printer.print(">");
            }
            printer.print("super");
        }
        printer.print("(");
        int argumentsCount = 0;
        for (com.github.javaparser.ast.expr.Expression argumentsItem : n.getArguments()) {
            argumentsItem.accept(this, arg);
            if (argumentsCount != n.getArguments().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            argumentsCount++;
        }
        printer.print(")");
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ExpressionStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getExpression().accept(this, arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(FieldAccessExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        printer.print(".");
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(FieldDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        if (!n.getVariables().isEmpty()) {
            n.getMaximumCommonType().accept(this, arg);
        }
        printer.print(" ");
        int variablesCount = 0;
        for (com.github.javaparser.ast.body.VariableDeclarator variablesItem : n.getVariables()) {
            variablesItem.accept(this, arg);
            if (variablesCount != n.getVariables().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            variablesCount++;
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ForStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("for");
        printer.print(" ");
        printer.print("(");
        int initializationCount = 0;
        for (com.github.javaparser.ast.expr.Expression initializationItem : n.getInitialization()) {
            initializationItem.accept(this, arg);
            if (initializationCount != n.getInitialization().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            initializationCount++;
        }
        printer.print(";");
        printer.print(" ");
        if (n.getCompare().isPresent()) {
            n.getCompare().get().accept(this, arg);
        }
        printer.print(";");
        printer.print(" ");
        int updateCount = 0;
        for (com.github.javaparser.ast.expr.Expression updateItem : n.getUpdate()) {
            updateItem.accept(this, arg);
            if (updateCount != n.getUpdate().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            updateCount++;
        }
        printer.print(")");
        printer.print(" ");
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ForeachStmt n, Void arg) {
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
    public void visit(IfStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("if");
        printer.print(" ");
        printer.print("(");
        n.getCondition().accept(this, arg);
        printer.print(")");
        if (n.hasThenBlock()) {
            printer.print(" ");
            n.getThenStmt().accept(this, arg);
            if (n.getElseStmt().isPresent()) {
                printer.print(" ");
            }
        } else {
            printer.println();
            printer.indent();
            n.getThenStmt().accept(this, arg);
            if (n.getElseStmt().isPresent()) {
                printer.println();
            }
            printer.unindent();
        }
        if (n.getElseStmt().isPresent()) {
            printer.print("else");
            if (n.hasElseBlock()) {
                printer.print(" ");
                if (n.getElseStmt().isPresent()) {
                    n.getElseStmt().get().accept(this, arg);
                }
            } else {
                printer.println();
                printer.indent();
                if (n.getElseStmt().isPresent()) {
                    n.getElseStmt().get().accept(this, arg);
                }
                printer.unindent();
            }
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ImportDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("import");
        printer.print(" ");
        if (n.isStatic()) {
            printer.print("static");
            printer.print(" ");
        }
        n.getName().accept(this, arg);
        if (n.isAsterisk()) {
            printer.print(".");
            printer.print("*");
        }
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(InitializerDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isStatic()) {
            printer.print("static");
            printer.print(" ");
        }
        n.getBody().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(InstanceOfExpr n, Void arg) {
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
    public void visit(IntegerLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(IntersectionType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int elementsCount = 0;
        for (com.github.javaparser.ast.type.ReferenceType elementsItem : n.getElements()) {
            elementsItem.accept(this, arg);
            if (elementsCount != n.getElements().size() - 1) {
                printer.print(" ");
                printer.print("&");
                printer.print(" ");
            }
            elementsCount++;
        }
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
    public void visit(LabeledStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getLabel().accept(this, arg);
        printer.print(":");
        printer.print(" ");
        n.getStatement().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LambdaExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isEnclosingParameters()) {
            printer.print("(");
        }
        int parametersCount = 0;
        for (com.github.javaparser.ast.body.Parameter parametersItem : n.getParameters()) {
            parametersItem.accept(this, arg);
            if (parametersCount != n.getParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            parametersCount++;
        }
        if (n.isEnclosingParameters()) {
            printer.print(")");
        }
        printer.print(" ");
        printer.print("->");
        printer.print(" ");
        if (n.getExpressionBody().isPresent()) {
            if (n.getExpressionBody().isPresent()) {
                n.getExpressionBody().get().accept(this, arg);
            }
        } else {
            n.getBody().accept(this, arg);
        }
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
    public void visit(LocalClassDeclarationStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getClassDeclaration().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LongLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getValue());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MarkerAnnotationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MemberValuePair n, Void arg) {
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
    public void visit(MethodCallExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            if (n.getScope().isPresent()) {
                n.getScope().get().accept(this, arg);
            }
            printer.print(".");
        }
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print("<");
        }
        int typeArgumentsCount = 0;
        for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
            typeArgumentsItem.accept(this, arg);
            if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeArgumentsCount++;
        }
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print(">");
        }
        n.getName().accept(this, arg);
        printer.print("(");
        int argumentsCount = 0;
        for (com.github.javaparser.ast.expr.Expression argumentsItem : n.getArguments()) {
            argumentsItem.accept(this, arg);
            if (argumentsCount != n.getArguments().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            argumentsCount++;
        }
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MethodDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (!n.getTypeParameters().isEmpty()) {
            printer.print("<");
        }
        int typeParametersCount = 0;
        for (com.github.javaparser.ast.type.TypeParameter typeParametersItem : n.getTypeParameters()) {
            typeParametersItem.accept(this, arg);
            if (typeParametersCount != n.getTypeParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeParametersCount++;
        }
        if (!n.getTypeParameters().isEmpty()) {
            printer.print(">");
            printer.print(" ");
        }
        n.getType().accept(this, arg);
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print("(");
        int parametersCount = 0;
        for (com.github.javaparser.ast.body.Parameter parametersItem : n.getParameters()) {
            parametersItem.accept(this, arg);
            if (parametersCount != n.getParameters().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            parametersCount++;
        }
        printer.print(")");
        if (!n.getThrownExceptions().isEmpty()) {
            printer.print(" ");
            printer.print("throws");
            printer.print(" ");
        }
        int thrownExceptionsCount = 0;
        for (com.github.javaparser.ast.type.ReferenceType thrownExceptionsItem : n.getThrownExceptions()) {
            thrownExceptionsItem.accept(this, arg);
            if (thrownExceptionsCount != n.getThrownExceptions().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            thrownExceptionsCount++;
        }
        if (n.getBody().isPresent()) {
            printer.print(" ");
            if (n.getBody().isPresent()) {
                n.getBody().get().accept(this, arg);
            }
        } else {
            printer.print(";");
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MethodReferenceExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getScope().accept(this, arg);
        printer.print("::");
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print("<");
        }
        int typeArgumentsCount = 0;
        for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
            typeArgumentsItem.accept(this, arg);
            if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeArgumentsCount++;
        }
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print(">");
        }
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        if (n.isOpen()) {
            printer.print("open");
            printer.print(" ");
        }
        printer.print("module");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(" ");
        printer.print("{");
        printer.println();
        printer.indent();
        int moduleStmtsCount = 0;
        for (com.github.javaparser.ast.modules.ModuleStmt moduleStmtsItem : n.getModuleStmts()) {
            moduleStmtsItem.accept(this, arg);
            moduleStmtsCount++;
        }
        printer.unindent();
        printer.print("}");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleExportsStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("exports");
        printer.print(" ");
        n.getName().accept(this, arg);
        if (!n.getModuleNames().isEmpty()) {
            printer.print(" ");
            printer.print("to");
            printer.print(" ");
        }
        int moduleNamesCount = 0;
        for (com.github.javaparser.ast.expr.Name moduleNamesItem : n.getModuleNames()) {
            moduleNamesItem.accept(this, arg);
            if (moduleNamesCount != n.getModuleNames().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            moduleNamesCount++;
        }
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleOpensStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("opens");
        printer.print(" ");
        n.getName().accept(this, arg);
        if (!n.getModuleNames().isEmpty()) {
            printer.print(" ");
            printer.print("to");
            printer.print(" ");
        }
        int moduleNamesCount = 0;
        for (com.github.javaparser.ast.expr.Name moduleNamesItem : n.getModuleNames()) {
            moduleNamesItem.accept(this, arg);
            if (moduleNamesCount != n.getModuleNames().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            moduleNamesCount++;
        }
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleProvidesStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("provides");
        printer.print(" ");
        n.getType().accept(this, arg);
        if (!n.getWithTypes().isEmpty()) {
            printer.print(" ");
            printer.print("with");
            printer.print(" ");
        }
        int withTypesCount = 0;
        for (com.github.javaparser.ast.type.Type withTypesItem : n.getWithTypes()) {
            withTypesItem.accept(this, arg);
            if (withTypesCount != n.getWithTypes().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            withTypesCount++;
        }
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleRequiresStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("requires");
        printer.print(" ");
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        n.getName().accept(this, arg);
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleUsesStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("uses");
        printer.print(" ");
        n.getType().accept(this, arg);
        printer.print(";");
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NameExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(Name n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getQualifier().isPresent()) {
            if (n.getQualifier().isPresent()) {
                n.getQualifier().get().accept(this, arg);
            }
            printer.print(".");
        }
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.print(" ");
        }
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NormalAnnotationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        int pairsCount = 0;
        for (com.github.javaparser.ast.expr.MemberValuePair pairsItem : n.getPairs()) {
            pairsItem.accept(this, arg);
            if (pairsCount != n.getPairs().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            pairsCount++;
        }
        printer.print(")");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NullLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("null");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ObjectCreationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getScope().isPresent()) {
            if (n.getScope().isPresent()) {
                n.getScope().get().accept(this, arg);
            }
            printer.print(".");
        }
        printer.print("new");
        printer.print(" ");
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print("<");
        }
        int typeArgumentsCount = 0;
        for (com.github.javaparser.ast.type.Type typeArgumentsItem : n.getTypeArguments().get()) {
            typeArgumentsItem.accept(this, arg);
            if (typeArgumentsCount != n.getTypeArguments().get().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            typeArgumentsCount++;
        }
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print(">");
        }
        if (n.getTypeArguments().isPresent() && !n.getTypeArguments().get().isEmpty()) {
            printer.print(" ");
        }
        n.getType().accept(this, arg);
        printer.print("(");
        int argumentsCount = 0;
        for (com.github.javaparser.ast.expr.Expression argumentsItem : n.getArguments()) {
            argumentsItem.accept(this, arg);
            if (argumentsCount != n.getArguments().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            argumentsCount++;
        }
        printer.print(")");
        if (n.getAnonymousClassBody().isPresent()) {
            printer.print(" ");
            printer.print("{");
            printer.println();
            printer.indent();
            if (n.getAnonymousClassBody().isPresent() && !n.getAnonymousClassBody().get().isEmpty()) {
                printer.println();
            }
            int anonymousClassBodyCount = 0;
            for (com.github.javaparser.ast.body.BodyDeclaration anonymousClassBodyItem : n.getAnonymousClassBody().get()) {
                if (anonymousClassBodyCount != 0) {
                    printer.println();
                }
                anonymousClassBodyItem.accept(this, arg);
                if (anonymousClassBodyCount != n.getAnonymousClassBody().get().size() - 1) {
                    printer.println();
                }
                anonymousClassBodyCount++;
            }
            if (n.getAnonymousClassBody().isPresent() && !n.getAnonymousClassBody().get().isEmpty()) {
                printer.println();
            }
            printer.unindent();
            printer.print("}");
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(PackageDeclaration n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        printer.print("package");
        printer.print(" ");
        n.getName().accept(this, arg);
        printer.print(";");
        printer.println();
        printer.println();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(Parameter n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.print(" ");
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        n.getType().accept(this, arg);
        if (n.isVarArgs()) {
            int varArgsAnnotationsCount = 0;
            for (com.github.javaparser.ast.expr.AnnotationExpr varArgsAnnotationsItem : n.getVarArgsAnnotations()) {
                varArgsAnnotationsItem.accept(this, arg);
                if (varArgsAnnotationsCount != n.getVarArgsAnnotations().size() - 1) {
                    printer.print(" ");
                }
                varArgsAnnotationsCount++;
            }
            printer.print("...");
        }
        printer.print(" ");
        n.getName().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(PrimitiveType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            annotationsCount++;
        }
        printer.print(n.getType().asString());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ReturnStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("return");
        if (n.getExpression().isPresent()) {
            printer.print(" ");
            if (n.getExpression().isPresent()) {
                n.getExpression().get().accept(this, arg);
            }
        }
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SimpleName n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(n.getIdentifier());
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SingleMemberAnnotationExpr n, Void arg) {
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
    public void visit(StringLiteralExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SuperExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getClassExpr().isPresent()) {
            if (n.getClassExpr().isPresent()) {
                n.getClassExpr().get().accept(this, arg);
            }
            printer.print(".");
        }
        printer.print("super");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SwitchEntryStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getLabel().isPresent()) {
            printer.print("case");
            printer.print(" ");
            if (n.getLabel().isPresent()) {
                n.getLabel().get().accept(this, arg);
            }
            printer.print(":");
        } else {
            printer.print("default");
            printer.print(":");
        }
        printer.println();
        printer.indent();
        int statementsCount = 0;
        for (com.github.javaparser.ast.stmt.Statement statementsItem : n.getStatements()) {
            statementsItem.accept(this, arg);
            if (statementsCount != n.getStatements().size() - 1) {
                printer.println();
            }
            statementsCount++;
        }
        if (!n.getStatements().isEmpty()) {
            printer.println();
        }
        printer.unindent();
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SwitchStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("switch");
        printer.print("(");
        n.getSelector().accept(this, arg);
        printer.print(")");
        printer.print(" ");
        printer.print("{");
        printer.println();
        if (!n.getEntries().isEmpty()) {
            printer.indent();
        }
        int entriesCount = 0;
        for (com.github.javaparser.ast.stmt.SwitchEntryStmt entriesItem : n.getEntries()) {
            entriesItem.accept(this, arg);
            entriesCount++;
        }
        if (!n.getEntries().isEmpty()) {
            printer.unindent();
        }
        printer.print("}");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SynchronizedStmt n, Void arg) {
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
    public void visit(ThisExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.getClassExpr().isPresent()) {
            if (n.getClassExpr().isPresent()) {
                n.getClassExpr().get().accept(this, arg);
            }
            printer.print(".");
        }
        printer.print("this");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ThrowStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("throw");
        printer.print(" ");
        n.getExpression().accept(this, arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TryStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print("try");
        printer.print(" ");
        if (!n.getResources().isEmpty()) {
            printer.print("(");
            if (!n.getResources().isEmpty()) {
                printer.indent();
            }
            int resourcesCount = 0;
            for (com.github.javaparser.ast.expr.VariableDeclarationExpr resourcesItem : n.getResources()) {
                resourcesItem.accept(this, arg);
                if (resourcesCount != n.getResources().size() - 1) {
                    printer.print(";");
                    printer.println();
                }
                resourcesCount++;
            }
            if (!n.getResources().isEmpty()) {
                printer.unindent();
            }
            printer.print(")");
            printer.print(" ");
        }
        if (n.getTryBlock().isPresent()) {
            n.getTryBlock().get().accept(this, arg);
        }
        int catchClausesCount = 0;
        for (com.github.javaparser.ast.stmt.CatchClause catchClausesItem : n.getCatchClauses()) {
            catchClausesItem.accept(this, arg);
            catchClausesCount++;
        }
        if (n.getFinallyBlock().isPresent()) {
            printer.print(" ");
            printer.print("finally");
            printer.print(" ");
            if (n.getFinallyBlock().isPresent()) {
                n.getFinallyBlock().get().accept(this, arg);
            }
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TypeExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getType().accept(this, arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TypeParameter n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        n.getName().accept(this, arg);
        if (!n.getTypeBound().isEmpty()) {
            printer.print(" ");
            printer.print("extends");
            printer.print(" ");
        }
        int typeBoundCount = 0;
        for (com.github.javaparser.ast.type.ClassOrInterfaceType typeBoundItem : n.getTypeBound()) {
            typeBoundItem.accept(this, arg);
            if (typeBoundCount != n.getTypeBound().size() - 1) {
                printer.print(" ");
                printer.print("&");
                printer.print(" ");
            }
            typeBoundCount++;
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnaryExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        if (n.isPrefix()) {
            printer.print(n.getOperator().asString());
        }
        n.getExpression().accept(this, arg);
        if (n.isPostfix()) {
            printer.print(n.getOperator().asString());
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnionType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        int elementsCount = 0;
        for (com.github.javaparser.ast.type.ReferenceType elementsItem : n.getElements()) {
            elementsItem.accept(this, arg);
            if (elementsCount != n.getElements().size() - 1) {
                printer.print(" ");
                printer.print("|");
                printer.print(" ");
            }
            elementsCount++;
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnknownType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnparsableStmt n, Void arg) {
        printJavaComment(n.getComment(), arg);
        printer.print(";");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VariableDeclarationExpr n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.print(" ");
        }
        int modifiersCount = 0;
        for (com.github.javaparser.ast.Modifier modifiersItem : n.getModifiers()) {
            printer.print(modifiersItem.asString());
            if (modifiersCount != n.getModifiers().size() - 1) {
                printer.print(" ");
            }
            modifiersCount++;
        }
        if (!n.getModifiers().isEmpty()) {
            printer.print(" ");
        }
        n.getMaximumCommonType().accept(this, arg);
        printer.print(" ");
        int variablesCount = 0;
        for (com.github.javaparser.ast.body.VariableDeclarator variablesItem : n.getVariables()) {
            variablesItem.accept(this, arg);
            if (variablesCount != n.getVariables().size() - 1) {
                printer.print(",");
                printer.print(" ");
            }
            variablesCount++;
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VariableDeclarator n, Void arg) {
        printJavaComment(n.getComment(), arg);
        n.getName().accept(this, arg);
        if (n.getInitializer().isPresent()) {
            printer.print(" ");
            printer.print("=");
            printer.print(" ");
            if (n.getInitializer().isPresent()) {
                n.getInitializer().get().accept(this, arg);
            }
        }
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VoidType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        printer.print("void");
        printOrphanCommentsEnding(n);
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(WhileStmt n, Void arg) {
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
    public void visit(WildcardType n, Void arg) {
        printJavaComment(n.getComment(), arg);
        int annotationsCount = 0;
        for (com.github.javaparser.ast.expr.AnnotationExpr annotationsItem : n.getAnnotations()) {
            annotationsItem.accept(this, arg);
            if (annotationsCount != n.getAnnotations().size() - 1) {
                printer.print(" ");
            }
            annotationsCount++;
        }
        if (!n.getAnnotations().isEmpty()) {
            printer.println();
        }
        printer.print("?");
        if (n.getExtendedType().isPresent()) {
            printer.print(" ");
            printer.print("extends");
            printer.print(" ");
            if (n.getExtendedType().isPresent()) {
                n.getExtendedType().get().accept(this, arg);
            }
        }
        if (n.getSuperType().isPresent()) {
            printer.print(" ");
            printer.print("super");
            printer.print(" ");
            if (n.getSuperType().isPresent()) {
                n.getSuperType().get().accept(this, arg);
            }
        }
        printOrphanCommentsEnding(n);
    }
}
