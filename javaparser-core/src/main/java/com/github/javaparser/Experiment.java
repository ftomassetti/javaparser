package com.github.javaparser;

import com.github.javaparser.GeneratedJavaParser.CustomToken;
import com.github.javaparser.ast.expr.Expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.github.javaparser.GeneratedJavaParserConstants.INTEGER_LITERAL;
import static com.github.javaparser.GeneratedJavaParserConstants.PLUS;

public class Experiment {

    private static class MemorizedTokensProvider extends GeneratedJavaParserTokenManager {

        private List<Token> tokens;
        private int position = 0;

        public MemorizedTokensProvider(List<Token> tokens) {
            super(new JavaCharStream(null, 0, 0, 1024));
            this.tokens = tokens;
        }

        public MemorizedTokensProvider(Token... tokens) {
            this(Arrays.asList(tokens));
        }

        @Override
        public Token getNextToken() {
            if (position < tokens.size()) {
                return tokens.get(position++);
            } else {
                return new Token(GeneratedJavaParserConstants.EOF);
            }
        }
    }

    private static CustomToken createToken(int kind, String image) {
        CustomToken customToken = new CustomToken(kind, image);
        customToken.javaToken = new JavaToken(customToken, Collections.emptyList());
        return customToken;
    }

    private static MemorizedTokensProvider codeToMemorizedTokensProvider(String code) {
        GeneratedJavaParserTokenManager generatedJavaParserTokenManager = new GeneratedJavaParserTokenManager(new JavaCharStream(new StringProvider(code)));
        List<Token> tokens = new LinkedList<>();
        CustomToken t = null;
        do {
            t = (CustomToken)generatedJavaParserTokenManager.getNextToken();
            if (t.javaToken.getKind() != GeneratedJavaParserConstants.EOF) {
                tokens.add(t);
            }
        } while (t.javaToken.getKind() != GeneratedJavaParserConstants.EOF);
        return new MemorizedTokensProvider(tokens);
    }

    public static void main(String[] args) throws ParseException {
        // This part demonstrates that I can parse using as source a list of tokens that I could build manually
        MemorizedTokensProvider tokensProvider = new MemorizedTokensProvider(createToken(INTEGER_LITERAL, "1"),
                createToken(PLUS, "+"),
                createToken(INTEGER_LITERAL, "2"));
        GeneratedJavaParser generatedJavaParser = new GeneratedJavaParser(tokensProvider);
        Expression expression = generatedJavaParser.Expression();
        System.out.println("Expression: " + expression);

        // Now I want to show that I can recognize tokens and fill a MemorizedTokensProvider
        tokensProvider = codeToMemorizedTokensProvider("1 + 2");
        generatedJavaParser = new GeneratedJavaParser(tokensProvider);
        expression = generatedJavaParser.Expression();
        System.out.println("Expression: " + expression);

        // Now I can build a MemorizedTokensProvider either by recognizing tokens from a string or building them
        // programmatically. In a template a do that: "1 + `INTEGER_LITERAL:myInt`"
    }
}
