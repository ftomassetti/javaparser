package com.github.javaparser.ast.validator;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.Test;

import static com.github.javaparser.ParseStart.STATEMENT;
import static com.github.javaparser.Providers.provider;
import static org.junit.Assert.assertEquals;

public class ValidatorTest {
    @Test
    public void noProblemsHere() {
        ParseResult<Statement> result =
                new JavaParser(new ParserConfiguration().setValidator(new NoProblemsValidator()))
                        .parse(STATEMENT, provider("try{}"));
        assertEquals(true, result.isSuccessful());
    }

}
