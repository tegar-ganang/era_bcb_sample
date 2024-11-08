package org.gguth;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.Token;
import org.antlr.runtime.RecognitionException;
import java.util.List;
import java.util.ArrayList;
import org.xj4.parameterized.Parameterized;
import org.xj4.parameterized.ParameterSet;
import org.xj4.XJ4Runner;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: Nov 14, 2008
 * Time: 3:36:49 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(XJ4Runner.class)
public class GguthTest {

    @ParameterSet.As
    public static final String[] parameters = { "{ frank }", "{ @Test\n" + "  @Parameterized\n" + "  public void outputTree(String input) throws Exception {\n" + "    CharStream inStream = new ANTLRStringStream(input);\n" + "    GUnit2a lexer = new GUnit2a(inStream);\n" + "    TokenSource source = lexer;\n" + "    Token token;\n" + "    System.out.println(\"Hi! {\");\n" + "    while((token = source.nextToken()).getType() != lexer.EOF) {\n" + "      System.out.format(\"%s:%s%n\", token.getType(), token.getText());\n" + "    }\n" + "  }\n" + "}", " /* frank */ ", " // stuff \n blah", " // stuff" };

    @Test
    @Parameterized
    public void outputTree(String input) throws Exception {
        CharStream inStream = new ANTLRStringStream(input);
        final List<RecognitionError> lexerErrors = new ArrayList<RecognitionError>();
        GguthLexer lexer = new GguthLexer(inStream) {

            @Override
            public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
                String hdr = getErrorHeader(e);
                String msg = getErrorMessage(e, tokenNames);
                throw new AssertionError(hdr + " " + msg);
            }
        };
        TokenSource source = lexer;
        Token token;
        while ((token = source.nextToken()).getType() != lexer.EOF) {
            int type = token.getType();
            String tokenName = tokenType(type);
            System.out.format("%s[%d,%d:%d]:%s%n", tokenName, token.getLine(), token.getCharPositionInLine(), token.getText().length(), token.getText());
        }
        assertTrue("Lexer errors were encountered.", lexerErrors.isEmpty());
    }

    private String tokenType(int type) {
        String tokenName = null;
        if (type >= 0) {
            tokenName = GguthParser.tokenNames[type];
        } else if (type == -1) {
            tokenName = "EOF";
        } else {
            throw new IllegalArgumentException(String.format("I don't recognize token type %d.", type));
        }
        return tokenName;
    }

    private class RecognitionError {

        private String[] tokenNames;

        private RecognitionException e;

        private RecognitionError(String[] tokenNames, RecognitionException e) {
            this.tokenNames = tokenNames;
            this.e = e;
        }

        public String[] getTokenNames() {
            return tokenNames;
        }

        public RecognitionException getE() {
            return e;
        }
    }

    private class GUnit2aLexerExtended extends GguthLexer {
    }

    private class ExpectedTokenStream {

        private List<ExpectedToken> expectedTokens = new ArrayList<ExpectedToken>();

        int[] ignoreChannels;

        private ExpectedTokenStream(int... ignoreChannels) {
            this.ignoreChannels = ignoreChannels;
        }

        public ExpectedTokenStream addExpectedToken(ExpectedToken expectedToken) {
            expectedTokens.add(expectedToken);
            return this;
        }

        public ExpectedTokenStream addExpectedToken(int type, String text) {
            return addExpectedToken(new ExpectedToken(type, text));
        }

        public void assertMatches(TokenSource tokenSource) {
            for (ExpectedToken token : expectedTokens) {
                token.assertMatches(nextApplicableToken(tokenSource));
            }
            Token eof = nextApplicableToken(tokenSource);
            if (eof.getType() >= 0) {
                StringBuffer sb = new StringBuffer(eof.toString());
                while ((eof = nextApplicableToken(tokenSource)).getType() >= 0) {
                    sb.append(eof.toString());
                }
                throw new AssertionError(String.format("Extraneous tokens remaining after all expected tokens matched: %s", sb.toString()));
            }
        }

        private Token nextApplicableToken(TokenSource tokenSource) {
            Token token = null;
            while (keepSearching(token = tokenSource.nextToken())) ;
            return token;
        }

        private boolean keepSearching(Token token) {
            if (token.getType() < 0) {
                return false;
            }
            for (int ignoreChannel : ignoreChannels) {
                if (ignoreChannel == token.getChannel()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class ExpectedToken {

        public static final String WILDCARD = "*";

        public static final int WILDCARD_TYPE = -99;

        private int type;

        private String text;

        private ExpectedToken(int type) {
            this(type, null);
        }

        private ExpectedToken(String text) {
            this(WILDCARD_TYPE, text);
        }

        private ExpectedToken(int type, String text) {
            this.type = type;
            this.text = text == null ? WILDCARD : text;
        }

        public void assertMatches(Token token) {
            if (type != WILDCARD_TYPE && type != token.getType()) {
                throw new AssertionError(String.format("Expected token of type %s, received token of type %s", tokenType(type), tokenType(token.getType())));
            }
            if (!WILDCARD.equals(text) && !text.equals(token.getText())) {
                throw new AssertionError(String.format("Expected %s with text %s, received text %s", tokenType(type), text, token.getText()));
            }
        }
    }
}
