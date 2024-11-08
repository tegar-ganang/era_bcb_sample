package lang4j.parser;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import junit.framework.TestCase;
import lang4j.generator.Generator;
import lang4j.generator.Transformer;
import lang4j.parser.generated.Lang4jGrammar;
import lang4j.parser.generated.Lang4jLexer;
import lang4j.parser.generated.Lang4jParser;
import lang4j.parser.generated.Production;
import org.antlr.runtime.*;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 */
public class ParserTest extends TestCase {

    public void testParsing() throws Exception, RecognitionException {
        InputStream is = getClass().getResourceAsStream("test.lang4j");
        Lang4jParser parser = getParser(is);
        List<Production> productions = parser.lang4jGrammar().getProductions();
        assertEquals(21, productions.size());
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    public void testParsingUnhappyPath() throws Exception, RecognitionException {
        Lang4jParser parser = getParser("invalid code");
        List<Production> productions = parser.lang4jGrammar().getProductions();
        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

    public void testTransformer() throws Exception, RecognitionException {
        InputStream is = getClass().getResourceAsStream("test.lang4j");
        Lang4jParser parser = getParser(is);
        Lang4jGrammar grammar = parser.lang4jGrammar();
        Transformer Transformer = new Transformer(grammar);
    }

    public void testGenerator() throws Exception, RecognitionException {
        InputStream is = getClass().getResourceAsStream("test.lang4j");
        Lang4jParser parser = getParser(is);
        Lang4jGrammar grammar = parser.lang4jGrammar();
        Transformer transformer = new Transformer(grammar);
        Generator gen = new Generator(grammar, transformer, new File("gen-src"));
    }

    public void testGeneratorMeta() throws Exception, org.antlr.runtime.RecognitionException {
        InputStream is = getClass().getResourceAsStream("lang4j.lang4j");
        Lang4jParser parser = getParser(is);
        Lang4jGrammar grammar = parser.lang4jGrammar();
        Transformer transformer = new Transformer(grammar);
        Generator gen = new Generator(grammar, transformer, new File("gen-src"));
    }

    public void testParsingGrammar() throws Exception, RecognitionException, org.antlr.runtime.RecognitionException {
        InputStream is = getClass().getResourceAsStream("test.lang4j");
        Lang4jParser parser = getParser(is);
        Lang4jGrammar grammar = parser.lang4jGrammar();
    }

    public void testLexer() throws TokenStreamException, RecognitionException {
        Lang4jLexer lexer = new Lang4jLexer(new ANTLRStringStream("'X' \"X\" '{'"));
        org.antlr.runtime.Token token = lexer.nextToken();
        assertEquals(Lang4jLexer.CHAR_LITERAL, token.getType());
        token = lexer.nextToken();
        assertEquals(Lang4jLexer.WS, token.getType());
        assertEquals(Token.HIDDEN_CHANNEL, token.getChannel());
        token = lexer.nextToken();
        assertEquals(Lang4jLexer.STRING_LITERAL, token.getType());
        token = lexer.nextToken();
        assertEquals(Lang4jLexer.WS, token.getType());
        token = lexer.nextToken();
        assertEquals(Lang4jLexer.CHAR_LITERAL, token.getType());
    }

    private Lang4jParser getParser(String source) throws Exception {
        return new Lang4jParser(new BufferedTokenStream(new Lang4jLexer(new ANTLRStringStream(source))));
    }

    private Lang4jParser getParser(InputStream source) throws Exception {
        return new Lang4jParser(new CommonTokenStream(new Lang4jLexer(new ANTLRInputStream(source))));
    }
}
