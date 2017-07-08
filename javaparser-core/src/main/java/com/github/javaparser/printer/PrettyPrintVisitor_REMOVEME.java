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
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AnnotationMemberDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayAccessExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayCreationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayCreationLevel n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayInitializerExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ArrayType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AssertStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(AssignExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BinaryExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BlockComment n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BlockStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BooleanLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(BreakStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CastExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CatchClause n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CharLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ClassOrInterfaceType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(CompilationUnit n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ConditionalExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ConstructorDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ContinueStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(DoStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(DoubleLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EmptyStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EnclosedExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EnumConstantDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(EnumDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ExpressionStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(FieldAccessExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(FieldDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ForStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ForeachStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(IfStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ImportDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(InitializerDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(InstanceOfExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(IntegerLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(IntersectionType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(JavadocComment n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LabeledStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LambdaExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LineComment n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LocalClassDeclarationStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(LongLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MarkerAnnotationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MemberValuePair n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MethodCallExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MethodDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(MethodReferenceExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleExportsStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleOpensStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleProvidesStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleRequiresStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ModuleUsesStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NameExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(Name n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NormalAnnotationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(NullLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ObjectCreationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(PackageDeclaration n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(Parameter n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(PrimitiveType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ReturnStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SimpleName n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SingleMemberAnnotationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(StringLiteralExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SuperExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SwitchEntryStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SwitchStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(SynchronizedStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ThisExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(ThrowStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TryStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TypeExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(TypeParameter n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnaryExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnionType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnknownType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(UnparsableStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VariableDeclarationExpr n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VariableDeclarator n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(VoidType n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(WhileStmt n, Void arg) {
    }

    @Override
    @Generated("com.github.javaparser.generator.core.visitor.PrettyPrintVisitorGenerator")
    public void visit(WildcardType n, Void arg) {
    }
}
