package org.norecess.antlr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.Tree;

/**
 * Implements various assertion methods for testing lexers, parsers, and tree
 * parsers. The best way to access these methods is with a static import:
 * 
 * <pre>
 * import static org.norecess.antlr.Assert.*;
 * </pre>
 * 
 * <p>
 * Testing methods come in two flavors: testing ANTLR structures directly and
 * testing through a {@link ANTLRTester} (and the objects it creates).
 * 
 * 
 * <h2>Testing a Lexer</h2>
 * 
 * <p>
 * The easiest way to test the output of a lexer is with
 * {@link #assertToken(int, String, PostScan)} and
 * {@link #assertToken(String, int, String, PostScan)}. The ANTLR tester does
 * all the work:
 * 
 * <pre>
 * assertToken(MyOwnLexer.IDENTIFIER, &quot;foo&quot;, myTester.scanInput(&quot;foo&quot;));
 * </pre>
 * 
 * This asserts the type and text of the token. Each of the assertion methods
 * has an optional message parameter:
 * 
 * <pre>
 * assertToken(&quot;should scan 'foo' as an identifier&quot;, MyOwnLexer.IDENTIFIER, &quot;foo&quot;,
 * 		myTester.scanInput(&quot;foo&quot;));
 * </pre>
 * 
 * When testing this way, {@link PostScan#getSingleToken()} is called, and so
 * the input is checked to make sure that there is only one token returned by
 * the input.
 * 
 * <p>
 * {@link #assertToken(int, String, Token)} will directly assert the type and
 * text of an {@link org.antlr.runtime.Token}.
 * 
 * <p>
 * To test the <em>failure</em> of a scan, use
 * {@link #refuteToken(int, PostScan)}. It asserts that the provided scan does
 * not or cannot produce the &quot;expected&quot; token type. For example, you
 * may want to assert that
 * <code>abcd<code> is never recognized as an integer, even though
 * <code>0xabcd</code> <em>is</em> a valid integer and <code>abcd</code>
 * <em>is</em> a valid identifier.
 * 
 * 
 * <h2>Testing a Parser</h2>
 * 
 * <p>
 * The easiest way to test the output of a parser is with
 * {@link #assertTree(int, String, PostParse)} and
 * {@link #assertTree(String, int, String, PostParse)}.
 * 
 * <pre>
 * assertTree(MyOwnParser.EXPRESSION, &quot;(+(1)(x))&quot;, myTester.scanInput(&quot;1 + x&quot;)
 * 		.parseAs(&quot;expression&quot;));
 * </pre>
 * 
 * The first argument is the type of the AST returned by the parser; the second
 * argument is a preorder traversal using the text of the tokens in the AST,
 * fully parenthesized. You again use an {@link ANTLRTester} to generate the
 * AST, going through {@link ANTLRTester#scanInput(String)} and
 * {@link PostScan#parseAs(String)}. The argument given to
 * {@link PostScan#parseAs(String)} is the name of the rule to use from the
 * parser.
 * 
 * <p>
 * {@link #assertTree(int, String, Tree)} and
 * {@link #assertTree(String, int, String, Tree)} let you test directly against
 * an ANTLR tree.
 * 
 * <p>
 * You can get your own preorder traversal (without any assertions) with
 * {@link #preorder(Tree)}.
 * 
 * <p>
 * To assert that a parse should fail, use
 * {@link #refuteParse(String, PostScan)}. Provide a scan and the name of the
 * parser product that should fail to process it.
 * 
 * <h2>Testing a Tree Parser</h2>
 * 
 * <p>
 * Since the output of a tree parser is usally <em>not</em> an ANTLR object (or
 * some other common object), you should use the normal
 * <code>assertEquals()</code> methods of JUnit. To do the tree parse, use
 * {@link PostParse#treeParseAs(String)}:
 * 
 * <pre>
 * assertEquals(new MyOwnIdentifier(&quot;foo&quot;), myTester.scanInput(&quot;foo&quot;).parseAs(
 * 		&quot;expression&quot;).treeParseAs(&quot;program&quot;));
 * </pre>
 * 
 * <p>
 * <code>"expression"</code> is the name of the production from the parser that
 * you want to use; <code>"program"</code> is the name of the production from
 * the tree parser that you want to use.
 * 
 * @author Jeremy D. Frens
 * 
 */
public class Assert {

    private Assert() {
    }

    /**
	 * Asserts the token produced by an ANTLR tester.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param postScan
	 *            the result of {@link ANTLRTester#scanInput(String)} which will
	 *            produce the token to assert.
	 */
    public static void assertToken(String message, int expectedType, String expectedText, PostScan postScan) {
        assertToken(message, expectedType, expectedText, postScan.getSingleToken());
    }

    /**
	 * Asserts the token produced by an ANTLR tester.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param expectedChannel
	 *            the channel the token should appear on.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param postScan
	 *            the result of {@link ANTLRTester#scanInput(String)} which will
	 *            produce the token to assert.
	 */
    public static void assertToken(String message, int expectedChannel, int expectedType, String expectedText, PostScan postScan) {
        assertToken(message, expectedChannel, expectedType, expectedText, postScan.getSingleToken());
    }

    /**
	 * Asserts the token produced by an ANTLR tester.
	 * 
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param postScan
	 *            the result of {@link ANTLRTester#scanInput(String)} which will
	 *            produce the token to assert.
	 */
    public static void assertToken(int expectedType, String expectedText, PostScan postScan) {
        assertToken(expectedType, expectedText, postScan.getSingleToken());
    }

    /**
	 * Asserts the token produced by an ANTLR tester.
	 * 
	 * @param expectedChannel
	 *            the channel the token should appear on.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param postScan
	 *            the result of {@link ANTLRTester#scanInput(String)} which will
	 *            produce the token to assert.
	 */
    public static void assertToken(int expectedChannel, int expectedType, String expectedText, PostScan postScan) {
        assertToken(expectedChannel, expectedType, expectedText, postScan.getSingleToken());
    }

    /**
	 * Asserts properties of a token.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param token
	 *            the token to assert.
	 */
    public static void assertToken(String message, int expectedType, String expectedText, Token token) {
        assertToken(message, BaseRecognizer.DEFAULT_TOKEN_CHANNEL, expectedType, expectedText, token);
    }

    /**
	 * Asserts properties of a token.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param expectedChannel
	 *            the channel the token should appear on.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param token
	 *            the token to assert.
	 */
    public static void assertToken(String message, int expectedChannel, int expectedType, String expectedText, Token token) {
        assertEquals(message + " (channel check)", expectedChannel, token.getChannel());
        assertEquals(message + " (type check)", expectedType, token.getType());
        assertEquals(message + " (text check)", expectedText, token.getText());
    }

    /**
	 * Asserts properties of a token.
	 * 
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param token
	 *            the token to assert.
	 */
    public static void assertToken(int expectedType, String expectedText, Token token) {
        assertToken(BaseRecognizer.DEFAULT_TOKEN_CHANNEL, expectedType, expectedText, token);
    }

    /**
	 * Asserts properties of a token.
	 * 
	 * @param expectedChannel
	 *            the channel the token should appear on.
	 * @param expectedType
	 *            the expected type of the token.
	 * @param expectedText
	 *            the expected text of the token.
	 * @param token
	 *            the token to assert.
	 */
    public static void assertToken(int expectedChannel, int expectedType, String expectedText, Token token) {
        assertEquals(expectedChannel, token.getChannel());
        assertEquals("failed to match token types,", expectedType, token.getType());
        assertEquals("failed to match token text,", expectedText, token.getText());
    }

    /**
	 * To "refute" a token means to assert that it cannot be of the specified
	 * type. This is useful, for example, when you want to assert that "x" will
	 * not be recognized as an integer. Eventually, it may be recognized as an
	 * identifier or some other token, but you just want to assert that it's
	 * definitely not an integer.
	 * 
	 * <pre>
	 * refuteToken(MyLexer.INTEGER, myTester.scanInput(&quot;x&quot;));
	 * </pre>
	 * 
	 * @param refutedType
	 *            the type the token should <em>not</em> be.
	 * @param postScan
	 *            the result of scanning input with the tester.
	 */
    public static void refuteToken(int refutedType, PostScan postScan) {
        try {
            if (refutedType == postScan.getSingleToken().getType()) {
                fail("scanned successfully as specified type");
            }
        } catch (AssertionError e) {
            if (checkMessage(e.getMessage())) {
            } else {
                throw e;
            }
        }
    }

    /**
	 * To "refute" a parse means that the scan cannot be parsed with the
	 * specified production.
	 * 
	 * <pre>
	 * refuteParse(&quot;program&quot;, myTester.scanInput(&quot;5 / * 8&quot;));
	 * </pre>
	 * 
	 * @param production
	 *            the production to apply from the parser.
	 * @param postScan
	 *            the result of scanning input with the tester.
	 */
    public static void refuteParse(String production, PostScan postScan) {
        try {
            postScan.parseAs(production);
            fail("parsed as " + production);
        } catch (AssertionError e) {
            if (checkMessage(e.getMessage())) {
            } else {
                throw e;
            }
        }
    }

    private static boolean checkMessage(String message) {
        return message.startsWith("failed to match EOF") || message.startsWith("unexpected error output") || message.startsWith("parsing does not consume all tokens");
    }

    /**
	 * Asserts a parse tree.
	 * 
	 * @param rootType
	 *            the type of the root of the tree.
	 * @param preorder
	 *            the preorder traversal of the tree.
	 * @param postParse
	 *            a helper class when using {@link ANTLRTester}.
	 */
    public static void assertTree(int rootType, String preorder, PostParse postParse) {
        assertTree(rootType, preorder, postParse.getTree());
    }

    /**
	 * Asserts a parse tree.
	 * 
	 * @param rootType
	 *            the type of the root of the tree.
	 * @param preorder
	 *            the preorder traversal of the tree.
	 * @param tree
	 *            an ANTLR tree to assert on.
	 */
    public static void assertTree(int rootType, String preorder, Tree tree) {
        assertNotNull("tree should be non-null", tree);
        assertEquals(preorder, preorder(tree));
        assertEquals(rootType, tree.getType());
    }

    /**
	 * Asserts a parse tree.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param rootType
	 *            the type of the root of the tree.
	 * @param preorder
	 *            the preorder traversal of the tree.
	 * @param postParse
	 *            a helper class when using {@link ANTLRTester}.
	 */
    public static void assertTree(String message, int rootType, String preorder, PostParse postParse) {
        assertTree(message, rootType, preorder, postParse.getTree());
    }

    /**
	 * Asserts a parse tree.
	 * 
	 * @param message
	 *            the message to display on failure.
	 * @param rootType
	 *            the type of the root of the tree.
	 * @param preorder
	 *            the preorder traversal of the tree.
	 * @param tree
	 *            an ANTLR tree to assert on.
	 */
    public static void assertTree(String message, int rootType, String preorder, Tree tree) {
        assertNotNull("tree should be non-null", tree);
        assertEquals(message + " (asserting type of root)", rootType, tree.getType());
        assertEquals(message + " (asserting preorder)", preorder, preorder(tree));
    }

    /**
	 * Generates a preorder traversal of an ANTLR tree. In general, each tree
	 * and subtree in the preorder output is parenthesized (even leaves). The
	 * text of the AST (or token) is used to get the string to add to the
	 * preorder traversal.
	 * 
	 * <p>
	 * There are two degenerate cases:
	 * <ul>
	 * <li> <code>"<NULL!!!!>"</code> is returned when <code>tree</code> itself
	 * is <code>null</code>.</li>
	 * <li> <code>"<nil>"</code> is returned when <code>tree</code> is nil.</li>
	 * </ul>
	 * 
	 * 
	 * @param tree
	 *            the tree to traverse.
	 * @return the preorder representation of the tree.
	 */
    public static String preorder(Tree tree) {
        if (tree == null) {
            return "<NULL!!!!>";
        } else if (tree.isNil()) {
            return "<nil>";
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            builder.append(tree.getText());
            for (int i = 0; i < tree.getChildCount(); i++) {
                builder.append(preorder(tree.getChild(i)));
            }
            builder.append(")");
            return builder.toString();
        }
    }
}
