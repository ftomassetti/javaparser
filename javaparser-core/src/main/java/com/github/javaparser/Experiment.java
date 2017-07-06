package com.github.javaparser;

import com.github.javaparser.GeneratedJavaParser.CustomToken;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.printer.PrettyPrintVisitor;

import java.util.*;

import static com.github.javaparser.GeneratedJavaParserConstants.*;

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
                System.out.println("NEXT TOKEN: " + tokens.get(position));
                return tokens.get(position++);
            } else {
                return new Token(GeneratedJavaParserConstants.EOF);
            }
        }

        @Override
        List<JavaToken> getTokens() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ReInit(JavaCharStream stream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ReInit(JavaCharStream stream, int lexState) {
            throw new UnsupportedOperationException();
        }
    }

    static class PlaceholderCustomJavaToken extends JavaToken {
        private String placeholderId;

        PlaceholderCustomJavaToken(Token token, String placeholderId) {
            super(token, Collections.emptyList());
            this.placeholderId = placeholderId;
        }

        public String getPlaceholderId() {
            return placeholderId;
        }
    }

    public static class TokensProviderBuilder {
        private List<Token> tokens = new ArrayList<>();

        public TokensProviderBuilder addToken(Token token) {
            tokens.add(token);
            return this;
        }

        public TokensProviderBuilder addToken(int kind, String image) {
            return addToken(createToken(kind, image));
        }

        private void addPlaceholder(int kind, String image, String id) {
            tokens.add(createToken(kind, image, new PlaceholderCustomJavaToken(new CustomToken(kind, image), id)));
        }

        public <T extends Node> TokensProviderBuilder addPlaceholderFor(Class<T> nodeClass, String id) {
            if (nodeClass.equals(Expression.class)) {
                addPlaceholder(INTEGER_LITERAL, "1", id);
            } else {
                throw new UnsupportedOperationException();
            }
            return this;
        }

        public MemorizedTokensProvider build() {
            return new MemorizedTokensProvider(tokens.toArray(new Token[]{}));
        }
    }

    private static CustomToken createToken(int kind, String image) {
        CustomToken customToken = new CustomToken(kind, image);
        customToken.javaToken = new JavaToken(customToken, Collections.emptyList());
        return customToken;
    }

    private static CustomToken createToken(int kind, String image, JavaToken javaToken) {
        CustomToken customToken = new CustomToken(kind, image);
        customToken.javaToken = javaToken;
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

    private static int FAKE_TOKEN = 999;

    static class TemplatableGeneratedJavaParser extends GeneratedJavaParser {

        public TemplatableGeneratedJavaParser(Provider stream) {
            super(stream);
        }


    }

    public static Expression generateMockExpression() {
        return new Expression() {
            @Override
            public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
                throw new UnsupportedOperationException(v.getClass().getCanonicalName());
            }

            @Override
            public <A> void accept(VoidVisitor<A> v, A arg) {
                if (v instanceof PrettyPrintVisitor) {
                    PrettyPrintVisitor prettyPrintVisitor = (PrettyPrintVisitor)v;
                    ((PrettyPrintVisitor) v).printer.print(this.toString());
                }
            }

            @Override
            public String toString() {
                return "<EXPRESSION PLACEHOLDER>";
            }
        };
    }

    static class MyTemplateableGeneratedJavaParser extends GeneratedJavaParser {

        public MyTemplateableGeneratedJavaParser(GeneratedJavaParserTokenManager tm) {
            super(tm);
        }

        private boolean nextIsFakeToken() {
            int tokenKind = (jj_ntk==-1)?jj_ntk_f():jj_ntk;
            return tokenKind == FAKE_TOKEN;
        }

        private int nextTokenType() {
            int tokenKind = (jj_ntk==-1)?jj_ntk_f():jj_ntk;
            return tokenKind;
        }
        @Override
        public Expression Expression() throws ParseException {
            System.out.println("EXPRESSION when facing " + nextTokenType());
            if (nextIsFakeToken() && token.next.image.equals("Expression")) {
                jj_consume_token(FAKE_TOKEN);
                return generateMockExpression();
            }
            return super.Expression();
        }

        @Override
        public Expression UnaryExpression() throws ParseException {
            System.out.println("UNARY EXPRESSION when facing " + nextTokenType());
            if (nextIsFakeToken() && token.next.image.equals("Expression")) {
                jj_consume_token(FAKE_TOKEN);
                return generateMockExpression();
            }
            return super.UnaryExpression();
        }

        @Override
        public Expression PrimaryExpression() throws ParseException {
            System.out.println("PRIMARY EXPRESSION when facing " + nextTokenType());
            if (nextIsFakeToken() && token.next.image.equals("Expression")) {
                jj_consume_token(FAKE_TOKEN);
                return generateMockExpression();
            }
            return super.PrimaryExpression();
        }
    }

    public static void examine(Node node) {
        examineHelper(node);
        for (Node descendant : node.getChildNodesByType(Node.class)) {
            examineHelper(descendant);
        }
    }

    private static void examineHelper(Node node) {
        System.out.println(node.getClass().getCanonicalName());
        TokenRange tokenRange = node.getTokenRange().get();
        if (tokenRange.getBegin() instanceof PlaceholderCustomJavaToken && tokenRange.getEnd() instanceof PlaceholderCustomJavaToken) {
            PlaceholderCustomJavaToken placeholderCustomJavaTokenA = (PlaceholderCustomJavaToken)tokenRange.getBegin();
            PlaceholderCustomJavaToken placeholderCustomJavaTokenB = (PlaceholderCustomJavaToken)tokenRange.getEnd();
            if (placeholderCustomJavaTokenA.getPlaceholderId() == placeholderCustomJavaTokenB.getPlaceholderId()) {
                System.out.println("PLACEHOLDER " + placeholderCustomJavaTokenA.getPlaceholderId() + " of type " + node.getClass().getSimpleName());
            }
        }

    }

    public static void main(String[] args) throws ParseException {
        MemorizedTokensProvider tokensProvider ;
        GeneratedJavaParser generatedJavaParser;
        Expression expression;

//        // This part demonstrates that I can parse using as source a list of tokens that I could build manually
//        tokensProvider = new MemorizedTokensProvider(createToken(INTEGER_LITERAL, "1"),
//                createToken(PLUS, "+"),
//                createToken(INTEGER_LITERAL, "2"));
//        generatedJavaParser = new GeneratedJavaParser(tokensProvider);
//        expression = generatedJavaParser.Expression();
//        System.out.println("Expression: " + expression);
//
//        // Now I want to show that I can recognize tokens and fill a MemorizedTokensProvider
//        tokensProvider = codeToMemorizedTokensProvider("1 + 2");
//        generatedJavaParser = new GeneratedJavaParser(tokensProvider);
//        expression = generatedJavaParser.Expression();
//        System.out.println("Expression: " + expression);
//
//        // Now I can build a MemorizedTokensProvider either by recognizing tokens from a string or building them
//        // programmatically. In a template a do that: "1 + `INTEGER_LITERAL:myInt`"
//
//        // "1 + `EXPRESSION:myRightExpr`"
//        // TODO: I would like to override the "Expression" method of the parser, however it is final
//        // I could do awful things using reflecton & javassist... because I am an horrible, horrible person
//        tokensProvider = new MemorizedTokensProvider(createToken(INTEGER_LITERAL, "1"),
//                createToken(PLUS, "+"),
//                createToken(FAKE_TOKEN, "Expression"));
//        generatedJavaParser = new MyTemplateableGeneratedJavaParser(tokensProvider);
//        expression = generatedJavaParser.Expression();
//
//        System.out.println("Expression: " + expression);

        tokensProvider = new TokensProviderBuilder().
                addToken(LPAREN, "(").
                addPlaceholderFor(Expression.class, "exp1").
                addToken(RPAREN, ")").
                build();
        //generatedJavaParser = new MyTemplateableGeneratedJavaParser(tokensProvider);
        generatedJavaParser = new GeneratedJavaParser(tokensProvider);
        expression = generatedJavaParser.Expression();

        System.out.println("Expression: " + expression);
        examine(expression);


//        tokensProvider = new MemorizedTokensProvider(
//                createToken(LPAREN, "("),
//                createToken(FAKE_TOKEN, "Expression"),
//                createToken(RPAREN, ")"),
//                createToken(PLUS, "+"),
//                createToken(FAKE_TOKEN, "Expression"));
//        generatedJavaParser = new MyTemplateableGeneratedJavaParser(tokensProvider);
//        expression = generatedJavaParser.Expression();
//
//        System.out.println("Expression: " + expression);
    }
}
