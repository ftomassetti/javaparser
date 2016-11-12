package com.github.javaparser.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.observing.AstObserver;
import com.github.javaparser.ast.observing.ObservableProperty;
import com.github.javaparser.ast.observing.PropagatingAstObserver;
import com.github.javaparser.printer.LexicalPreservingPrinter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LexicalPreservationTest {

    @Test
    public void printASimpleCU() {
        String code = "class /*a comment*/ A {\t\t\n int f;\n\n\n         void foo(int p  ) { return  'z'  \t; }}";
        CompilationUnit cu = JavaParser.parse(code);

        AstObserver observer = new PropagatingAstObserver() {
            @Override
            public void concretePropertyChange(Node observedNode, ObservableProperty property, Object oldValue, Object newValue) {

            }
        };
        LexicalPreservingPrinter lpp = new LexicalPreservingPrinter();
        cu.onSubStree(node -> lpp.registerText(node, code));

        assertEquals(code, lpp.print(cu));
        assertEquals(code, lpp.print(cu.getClassByName("A")));
        assertEquals("void foo(int p  ) { return  'z'  \t; }", lpp.print(cu.getClassByName("A").getMethodsByName("foo").get(0)));
    }
}
