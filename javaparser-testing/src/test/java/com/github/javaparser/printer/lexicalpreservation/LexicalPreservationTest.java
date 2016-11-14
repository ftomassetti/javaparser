package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.observing.AstObserver;
import com.github.javaparser.ast.observing.ObservableProperty;
import com.github.javaparser.ast.observing.PropagatingAstObserver;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LexicalPreservationTest {

    @Test
    public void checkNodeTextCreatedForSimplestClass() {
        String code = "class A {}";
        CompilationUnit cu = JavaParser.parse(code);

        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                throw new UnsupportedOperationException();
            }
        };
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));

        // CU
        assertEquals(1, lpp.getTextForNode(cu).numberOfElements());
        assertEquals(true, lpp.getTextForNode(cu).getTextElement(0) instanceof ChildNodeTextElement);
        assertEquals(cu.getClassByName("A"), ((ChildNodeTextElement)lpp.getTextForNode(cu).getTextElement(0)).getChild());

        // Class
        ClassOrInterfaceDeclaration classA = cu.getClassByName("A");
        assertEquals(3, lpp.getTextForNode(classA).numberOfElements());
        assertEquals(true, lpp.getTextForNode(classA).getTextElement(0) instanceof StringNodeTextElement);
        assertEquals("class ", ((StringNodeTextElement)lpp.getTextForNode(classA).getTextElement(0)).getText());
        assertEquals(true, lpp.getTextForNode(classA).getTextElement(1) instanceof ChildNodeTextElement);
        assertEquals(classA.getName(), ((ChildNodeTextElement)lpp.getTextForNode(classA).getTextElement(1)).getChild());
        assertEquals(" {}", ((StringNodeTextElement)lpp.getTextForNode(classA).getTextElement(2)).getText());

        // SimpleName
        SimpleName aName = classA.getName();
        assertEquals(1, lpp.getTextForNode(aName).numberOfElements());
        assertEquals(true, lpp.getTextForNode(aName).getTextElement(0) instanceof StringNodeTextElement);
        assertEquals("A", ((StringNodeTextElement)lpp.getTextForNode(aName).getTextElement(0)).getText());
    }

    @Test
    public void printASuperSimpleCUWithoutChanges() {
        String code = "class A {}";
        CompilationUnit cu = JavaParser.parse(code);

        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                throw new UnsupportedOperationException();
            }
        };
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));

        assertEquals(code, lpp.print(cu));
    }

    @Test
    public void printASuperSimpleClassWithAFieldAdded() {
        String code = "class A {}";
        CompilationUnit cu = JavaParser.parse(code);

        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(createObserver(lpp));
        cu.onSubStree(node -> lpp.registerText(node, code));

        ClassOrInterfaceDeclaration classA = cu.getClassByName("A");
        classA.addField("int", "myField");
        assertEquals("class A {int myField  ;\n}", lpp.print(classA));
    }

    @Test
    public void printASuperSimpleClassWithoutChanges() {
        String code = "class A {}";
        CompilationUnit cu = JavaParser.parse(code);

        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                throw new UnsupportedOperationException();
            }
        };
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));

        assertEquals(code, lpp.print(cu.getClassByName("A")));
    }

    @Test
    public void printASimpleCUWithoutChanges() {
        String code = "class /*a comment*/ A {\t\t\n int f;\n\n\n         void foo(int p  ) { return  'z'  \t; }}";
        CompilationUnit cu = JavaParser.parse(code);

        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                throw new UnsupportedOperationException();
            }
        };
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));

        assertEquals(code, lpp.print(cu));
        assertEquals(code, lpp.print(cu.getClassByName("A")));
        assertEquals("void foo(int p  ) { return  'z'  \t; }", lpp.print(cu.getClassByName("A").getMethodsByName("foo").get(0)));
    }

    @Test
    public void printASimpleClassRemovingAField() {
        String code = "class /*a comment*/ A {\t\t\n int f;\n\n\n         void foo(int p  ) { return  'z'  \t; }}";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void concreteListChange(NodeList observedNode, ListChangeType type, int index, Node nodeAddedOrRemoved) {
                if (type == type.REMOVAL) {
                    lpp.updateTextBecauseOfRemovedChild(observedNode.getParentNode(), nodeAddedOrRemoved);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };
        cu.registerForSubtree(observer);
        cu.onSubStree(node -> lpp.registerText(node, code));

        ClassOrInterfaceDeclaration c = cu.getClassByName("A");
        c.getMembers().remove(0);
        assertEquals("class /*a comment*/ A {\t\t\n" +
                " \n" +
                "\n" +
                "\n" +
                "         void foo(int p  ) { return  'z'  \t; }}", lpp.print(c));
    }

    private AstObserver createObserver(LexicalPreservingPrinter lpp) {
        return new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {
                if (oldValue.equals(newValue)) {
                    return;
                }
                if (oldValue instanceof Node && newValue instanceof Node) {
                    lpp.getTextForNode(observedNode).replaceChild((Node)oldValue, (Node)newValue);
                    return;
                }
                throw new UnsupportedOperationException(String.format("Property %s. OLD %s (%s) NEW %s (%s)", property, oldValue,
                        oldValue.getClass(), newValue, newValue.getClass()));
            }

            @Override
            public void concreteListChange(NodeList observedNode, ListChangeType type, int index, Node nodeAddedOrRemoved) {
                if (type == type.REMOVAL) {
                    lpp.updateTextBecauseOfRemovedChild(observedNode.getParentNode(), nodeAddedOrRemoved);
                } else if (type == type.ADDITION) {
                    lpp.updateTextBecauseOfAddedChild(observedNode, index, observedNode.getParentNode(), nodeAddedOrRemoved);
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        };
    }

    @Test
    public void printASimpleMethodAddingAParameterToAMethodWithZeroParameters() {
        String code = "class A { void foo() {} }";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(createObserver(lpp));
        cu.onSubStree(node -> lpp.registerText(node, code));

        MethodDeclaration m = cu.getClassByName("A").getMethodsByName("foo").get(0);
        m.addParameter("float", "p1");
        assertEquals("void foo(float p1) {}", lpp.print(m));
    }

    @Test
    public void printASimpleMethodAddingAParameterToAMethodWithOneParameter() {
        String code = "class A { void foo(char p1) {} }";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.registerForSubtree(createObserver(lpp));
        cu.onSubStree(node -> lpp.registerText(node, code));

        MethodDeclaration m = cu.getClassByName("A").getMethodsByName("foo").get(0);
        m.addParameter("float", "p2");
        assertEquals("void foo(char p1, float p2) {}", lpp.print(m));
    }

    @Test
    public void printASimpleMethodRemovingAParameterToAMethodWithOneParameter() {
        String code = "class A { void foo(float p1) {} }";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.register(createObserver(lpp), Node.ObserverRegistrationMode.SELF_PROPAGATING);
        cu.onSubStree(node -> lpp.registerText(node, code));

        MethodDeclaration m = cu.getClassByName("A").getMethodsByName("foo").get(0);
        m.getParameters().remove(0);
        assertEquals("void foo() {}", lpp.print(m));
    }

    @Test
    public void printASimpleMethodRemovingParameterOneFromMethodWithTwoParameters() {
        String code = "class A { void foo(char p1, int p2) {} }";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.register(createObserver(lpp), Node.ObserverRegistrationMode.SELF_PROPAGATING);
        cu.onSubStree(node -> lpp.registerText(node, code));

        MethodDeclaration m = cu.getClassByName("A").getMethodsByName("foo").get(0);
        m.getParameters().remove(0);
        assertEquals("void foo(int p2) {}", lpp.print(m));
    }

    @Test
    public void printASimpleMethodRemovingParameterTwoFromMethodWithTwoParameters() {
        String code = "class A { void foo(char p1, int p2) {} }";
        CompilationUnit cu = JavaParser.parse(code);
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.register(createObserver(lpp), Node.ObserverRegistrationMode.SELF_PROPAGATING);
        cu.onSubStree(node -> lpp.registerText(node, code));

        MethodDeclaration m = cu.getClassByName("A").getMethodsByName("foo").get(0);
        m.getParameters().remove(1);
        assertEquals("void foo(char p1) {}", lpp.print(m));
    }
}
