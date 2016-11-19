package com.github.javaparser.printer.lexicalpreservation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.StreamProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import static com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter.setup;
import static org.junit.Assert.assertEquals;

public class TransformationsTest {

    private LexicalPreservingPrinter lpp;

    private CompilationUnit parseExample(String exampleName) throws IOException {
        InputStream is = this.getClass().getResourceAsStream("/com/github/javaparser/lexical_preservation_samples/" + exampleName + "_original.java.txt");
        String code = read(is);
        CompilationUnit cu = JavaParser.parse(code);
        lpp = setup(cu, code);
        return cu;
    }

    private void assertTransformed(String exampleName, CompilationUnit cu) throws IOException {
        InputStream is = this.getClass().getResourceAsStream("/com/github/javaparser/lexical_preservation_samples/" + exampleName + "_expected.java.txt");
        String expectedCode = read(is);
        String actualCode = lpp.print(cu);
        assertEquals(expectedCode, actualCode);
    }

    private String read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    @Test
    public void example1() throws IOException {
        CompilationUnit cu = parseExample("Example1");
        cu.getClassByName("A").getFieldByName("a").setModifiers(EnumSet.of(Modifier.STATIC));
        assertTransformed("Example1", cu);
    }

    @Test
    public void example2() throws IOException {
        CompilationUnit cu = parseExample("Example2");
        cu.getClassByName("A").getFieldByName("a").getVariable(0).setInit("10");
        assertTransformed("Example2", cu);
    }

}
