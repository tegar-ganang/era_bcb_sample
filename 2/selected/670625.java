package org.htmlparser.tests.lexerTests;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Remark;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.tests.ParserTestCase;
import org.htmlparser.util.EncodingChangeException;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class LexerTests extends ParserTestCase {

    static {
        System.setProperty("org.htmlparser.tests.lexerTests.LexerTests", "LexerTests");
    }

    /**
     * Test the Lexer class.
     */
    public LexerTests(String name) {
        super(name);
    }

    /**
     * Test operation without tags.
     */
    public void testPureText() throws ParserException {
        String reference;
        Lexer lexer;
        Text node;
        reference = "Hello world";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
    }

    /**
     * Test operation with Unix line endings.
     */
    public void testUnixEOL() throws ParserException {
        String reference;
        Lexer lexer;
        Text node;
        reference = "Hello\nworld";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
    }

    /**
     * Test operation with Dos line endings.
     */
    public void testDosEOL() throws ParserException {
        String reference;
        Lexer lexer;
        Text node;
        reference = "Hello\r\nworld";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
        reference = "Hello\rworld";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
    }

    /**
     * Test operation with line endings near the end of input.
     */
    public void testEOF_EOL() throws ParserException {
        String reference;
        Lexer lexer;
        Text node;
        reference = "Hello world\n";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
        reference = "Hello world\r";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
        reference = "Hello world\r\n";
        lexer = new Lexer(reference);
        node = (Text) lexer.nextNode();
        assertEquals("Text contents wrong", reference, node.getText());
    }

    /**
     * Test that tags stop string nodes.
     */
    public void testTagStops() throws ParserException {
        String[] references = { "Hello world", "Hello world\n", "Hello world\r\n", "Hello world\r" };
        String[] suffixes = { "<head>", "</head>", "<%=head%>", "<?php ?>", "<!--head-->" };
        Lexer lexer;
        Text node;
        for (int i = 0; i < references.length; i++) {
            for (int j = 0; j < suffixes.length; j++) {
                lexer = new Lexer(references[i] + suffixes[j]);
                node = (Text) lexer.nextNode();
                assertEquals("Text contents wrong", references[i], node.getText());
            }
        }
    }

    /**
     * Test operation with only tags.
     */
    public void testPureTag() throws ParserException {
        String reference;
        String suffix;
        Lexer lexer;
        Node node;
        reference = "<head>";
        lexer = new Lexer(reference);
        node = lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        reference = "<head>";
        suffix = "<body>";
        lexer = new Lexer(reference + suffix);
        node = lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        node = lexer.nextNode();
        assertEquals("Tag contents wrong", suffix, node.toHtml());
    }

    /**
     * Test operation with attributed tags.
     */
    public void testAttributedTag() throws ParserException {
        String reference;
        Lexer lexer;
        Node node;
        reference = "<head lang='en_US' dir=ltr\nprofile=\"http://htmlparser.sourceforge.org/dictionary.html\">";
        lexer = new Lexer(reference);
        node = lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
    }

    /**
     * Test operation with comments.
     */
    public void testRemark() throws ParserException {
        String reference;
        Lexer lexer;
        Remark node;
        String suffix;
        reference = "<!-- This is a comment -->";
        lexer = new Lexer(reference);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        reference = "<!-- This is a comment --  >";
        lexer = new Lexer(reference);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        reference = "<!-- This is a\nmultiline comment -->";
        lexer = new Lexer(reference);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        suffix = "<head>";
        reference = "<!-- This is a comment -->";
        lexer = new Lexer(reference + suffix);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        reference = "<!-- This is a comment --  >";
        lexer = new Lexer(reference + suffix);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
        reference = "<!-- This is a\nmultiline comment -->";
        lexer = new Lexer(reference + suffix);
        node = (Remark) lexer.nextNode();
        assertEquals("Tag contents wrong", reference, node.toHtml());
    }

    /**
     * Test the fidelity of the toHtml() method.
     */
    public void testFidelity() throws ParserException, IOException {
        Lexer lexer;
        Node node;
        int position;
        StringBuffer buffer;
        String string;
        char[] ref;
        char[] test;
        URL url = new URL("http://sourceforge.net");
        lexer = new Lexer(url.openConnection());
        position = 0;
        buffer = new StringBuffer(80000);
        while (null != (node = lexer.nextNode())) {
            string = node.toHtml();
            if (position != node.getStartPosition()) fail("non-contiguous" + string);
            buffer.append(string);
            position = node.getEndPosition();
            if (buffer.length() != position) fail("text length differed after encountering node " + string);
        }
        ref = lexer.getPage().getText().toCharArray();
        test = new char[buffer.length()];
        buffer.getChars(0, buffer.length(), test, 0);
        assertEquals("different amounts of text", ref.length, test.length);
        for (int i = 0; i < ref.length; i++) if (ref[i] != test[i]) fail("character differs at position " + i + ", expected <" + ref[i] + "> but was <" + test[i] + ">");
    }

    static final HashSet mAcceptable;

    static {
        mAcceptable = new HashSet();
        mAcceptable.add("A");
        mAcceptable.add("BODY");
        mAcceptable.add("BR");
        mAcceptable.add("CENTER");
        mAcceptable.add("FONT");
        mAcceptable.add("HEAD");
        mAcceptable.add("HR");
        mAcceptable.add("HTML");
        mAcceptable.add("IMG");
        mAcceptable.add("P");
        mAcceptable.add("TABLE");
        mAcceptable.add("TD");
        mAcceptable.add("TITLE");
        mAcceptable.add("TR");
        mAcceptable.add("META");
        mAcceptable.add("STRONG");
        mAcceptable.add("FORM");
        mAcceptable.add("INPUT");
        mAcceptable.add("!DOCTYPE");
        mAcceptable.add("TBODY");
        mAcceptable.add("B");
        mAcceptable.add("DIV");
        mAcceptable.add("SCRIPT");
        mAcceptable.add("NOSCRIPT");
        mAcceptable.add("STYLE");
        mAcceptable.add("SPAN");
        mAcceptable.add("UL");
        mAcceptable.add("LI");
        mAcceptable.add("IFRAME");
        mAcceptable.add("LINK");
        mAcceptable.add("H1");
        mAcceptable.add("H3");
        mAcceptable.add("OBJECT");
        mAcceptable.add("PARAM");
        mAcceptable.add("EMBED");
    }

    /**
     * Test case for bug #789439 Japanese page causes OutOfMemory Exception
     * No exception is thrown in the current version of the parser,
     * however, the problem is that ISO-2022-JP (aka JIS) encoding sometimes
     * causes spurious tags.
     * The root cause is characters bracketed by [esc]$B and [esc](J (contrary
     * to what is indicated in then j_s_nightingale analysis of the problem) that
     * sometimes have an angle bracket (&lt; or 0x3c) embedded in them. These
     * are taken to be tags by the parser, instead of being considered strings.
     * <p>
     * The URL refrenced has an ISO-8859-1 encoding (the default), but
     * Japanese characters intermixed on the page with English, using the JIS
     * encoding. We detect failure by looking for weird tag names which were
     * not correctly handled as string nodes.
     * <p>
     * Here is a partial dump of the page with escape sequences:
     * <pre>
     * 0002420 1b 24 42 3f 79 4a 42 25 47 25 38 25 2b 25 61 43
     * 0002440 35 44 65 43 44 1b 28 4a 20 77 69 74 68 20 43 61
     * ..
     * 0002720 6c 22 3e 4a 53 6b 79 1b 24 42 42 50 31 7e 25 5a
     * 0002740 21 3c 25 38 1b 28 4a 3c 2f 41 3e 3c 50 3e 0a 3c
     * ..
     * 0003060 20 69 1b 24 42 25 62 21 3c 25 49 42 50 31 7e 25
     * 0003100 5a 21 3c 25 38 1b 28 4a 3c 2f 41 3e 3c 50 3e 0a
     * ..
     * 0003220 1b 24 42 25 2d 25 3f 25 5e 25 2f 25 69 24 4e 25
     * 0003240 5b 21 3c 25 60 25 5a 21 3c 25 38 1b 28 4a 3c 2f
     * ..
     * 0003320 6e 65 31 2e 70 6c 22 3e 1b 24 42 3d 60 48 77 43
     * 0003340 66 1b 28 4a 3c 2f 41 3e 3c 50 3e 0a 2d 2d 2d 2d
     * ..
     * 0004400 46 6f 72 75 6d 20 30 30 39 20 28 1b 24 42 3e 21
     * 0004420 3c 6a 24 4b 31 4a 4a 21 44 2e 24 4a 24 49 1b 28
     * 0004440 4a 29 3c 2f 41 3e 3c 49 4d 47 20 53 52 43 3d 22
     * </pre>
     * <p>
     * The fix proposed by j_s_nightingale is implemented to swallow JIS
     * escape sequences in the string parser.
     * Apparently the fix won't help EUC-JP and Shift-JIS though, so this may
     * still be a problem.
     * It's theoretically possible that JIS encoding, or another one,
     * could be used as attribute names or values within tags as well,
     * but this is considered improbable and is therefore not handled in
     * the tag parser state machine.
     */
    public void testJIS() throws ParserException {
        Parser parser;
        NodeIterator iterator;
        parser = new Parser("http://www.009.com/");
        try {
            iterator = parser.elements();
            while (iterator.hasMoreNodes()) checkTagNames(iterator.nextNode());
        } catch (EncodingChangeException ece) {
            parser.reset();
            iterator = parser.elements();
            while (iterator.hasMoreNodes()) checkTagNames(iterator.nextNode());
        }
    }

    /**
     * Check the tag name for one of the ones expected on the page.
     * Recursively check the children.
     */
    public void checkTagNames(Node node) {
        Tag tag;
        String name;
        NodeList children;
        if (node instanceof Tag) {
            tag = (Tag) node;
            name = tag.getTagName();
            if (!mAcceptable.contains(name)) fail("unrecognized tag name \"" + name + "\"");
            children = tag.getChildren();
            if (null != children) for (int i = 0; i < children.size(); i++) checkTagNames(children.elementAt(i));
        }
    }

    /**
     * See bug #825820 Words conjoined
     */
    public void testConjoined() throws ParserException {
        StringBuffer buffer;
        NodeIterator iterator;
        Node node;
        String expected;
        expected = "The Title\nThis is the body.";
        String html1 = "<html><title>The Title\n</title>" + "<body>This is <a href=\"foo.html\">the body</a>.</body></html>";
        createParser(html1);
        buffer = new StringBuffer();
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toPlainTextString();
            buffer.append(text);
        }
        assertStringEquals("conjoined text", expected, buffer.toString());
        String html2 = "<html><title>The Title</title>\n" + "<body>This is <a href=\"foo.html\">the body</a>.</body></html>";
        createParser(html2);
        buffer = new StringBuffer();
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toPlainTextString();
            buffer.append(text);
        }
        assertStringEquals("conjoined text", expected, buffer.toString());
        String html3 = "<html><title>The Title</title>" + "<body>\nThis is <a href=\"foo.html\">the body</a>.</body></html>";
        createParser(html3);
        buffer = new StringBuffer();
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toPlainTextString();
            buffer.append(text);
        }
        assertStringEquals("conjoined text", expected, buffer.toString());
    }

    /**
     * Check for StackOverflow error.
     */
    public void testStackOverflow() throws ParserException {
        NodeIterator iterator;
        Node node;
        String html;
        html = "<a href = \"http://test.com\" />";
        createParser(html);
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toHtml();
            assertStringEquals("no overflow", html, text);
        }
        html = "<a href=\"http://test.com\"/>";
        createParser(html);
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toHtml();
            assertStringEquals("no overflow", html, text);
        }
        html = "<a href = \"http://test.com\"/>";
        createParser(html);
        for (iterator = parser.elements(); iterator.hasMoreNodes(); ) {
            node = iterator.nextNode();
            String text = node.toHtml();
            assertStringEquals("no overflow", html, text);
        }
    }

    /**
     * See bug #880283 Character "&gt;" erroneously inserted by Lexer
     */
    public void testJsp() throws ParserException {
        String html;
        Lexer lexer;
        Node node;
        html = "<% out.urlEncode('abc') + \"<br>\" + out.urlEncode('xyz') %>";
        lexer = new Lexer(html);
        node = lexer.nextNode();
        if (node == null) fail("too few nodes"); else assertStringEquals("bad html", html, node.toHtml());
        assertNull("too many nodes", lexer.nextNode());
    }

    /**
     * Unit test for new PI parsing code.
     */
    public void testPI() throws ParserException {
        String html;
        Lexer lexer;
        Node node;
        html = "<?php print(\"<p>Hello World!</p>\"); ?>";
        lexer = new Lexer(html);
        node = lexer.nextNode();
        if (node == null) fail("too few nodes"); else assertStringEquals("bad html", html, node.toHtml());
        assertNull("too many nodes", lexer.nextNode());
    }

    /**
     * See bug #899413 bug in javascript end detection.
     */
    public void testEscapedQuote() throws ParserException {
        String string;
        String html;
        Lexer lexer;
        Node node;
        string = "\na='\\'';\n";
        html = string + "</script>";
        lexer = new Lexer(html);
        node = lexer.nextNode(true);
        if (node == null) fail("too few nodes"); else assertStringEquals("bad string", string, node.toHtml());
        assertNotNull("too few nodes", lexer.nextNode(true));
        assertNull("too many nodes", lexer.nextNode(true));
    }

    /**
     * See bug #1227213 Particular SCRIPT tags close too late.
     */
    public void testCommentInScript() throws ParserException {
        String tag;
        String cdata;
        String endtag;
        String html;
        Parser parser;
        NodeIterator iterator;
        Node node;
        tag = "<script>";
        cdata = "<!--document.write(\"en\");// -->";
        endtag = "</script>";
        html = tag + cdata + endtag;
        parser = new Parser();
        parser.setInputHTML(html);
        iterator = parser.elements();
        node = iterator.nextNode();
        if (node == null) fail("too few nodes"); else assertStringEquals("bad parse", html, node.toHtml());
        assertTrue(node instanceof ScriptTag);
        assertStringEquals("bad cdata", cdata, ((ScriptTag) node).getScriptCode());
        assertNull("too many nodes", iterator.nextNode());
    }

    /**
     * See bug #1227213 Particular SCRIPT tags close too late.
     * This was actually working prior to the patch, since the
     * ScriptScanner didn't use smartquote processing.
     * I'm not sure why jwilsonsprings1 said the patch worked
     * for him. I can only assume he was mistaken in thinking
     * it was the URL that caused the failure.
     */
    public void testUrlInStyle() throws ParserException {
        String tag;
        String cdata;
        String endtag;
        String html;
        Parser parser;
        NodeIterator iterator;
        Node node;
        tag = "<style>";
        cdata = ".eSDot {background-image:" + "url(http://di.image.eshop.msn.com/img/sys/dot.gif)}";
        endtag = "</style>";
        html = tag + cdata + endtag;
        parser = new Parser();
        parser.setInputHTML(html);
        iterator = parser.elements();
        node = iterator.nextNode();
        if (node == null) fail("too few nodes"); else assertStringEquals("bad parse", html, node.toHtml());
        assertTrue(node instanceof StyleTag);
        assertStringEquals("bad cdata", cdata, ((StyleTag) node).getStyleCode());
        assertNull("too many nodes", iterator.nextNode());
    }

    /**
     * See bug #1493884 Lexer returns a TagNode with a 'null' name
     */
    public void testDosLineEndingInName() throws ParserException {
        String html;
        NodeIterator iterator;
        Node node;
        html = "<!\r\nMSIE->";
        parser = new Parser();
        parser.setInputHTML(html);
        iterator = parser.elements();
        node = iterator.nextNode();
        if (node == null) fail("too few nodes"); else {
            assertNotNull("null node", node);
            assertTrue(node instanceof Tag);
            assertNotNull("null name", ((Tag) node).getTagName());
            assertStringEquals("bad parse", "!", ((Tag) node).getTagName());
        }
    }
}
