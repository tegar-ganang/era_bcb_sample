package org.gnomekr.potron.parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 * POParserTest.java
 * @author Xavier Cho
 * @version $Revision 1.1 $ $Date: 2005/08/28 11:47:15 $
 */
public class POParserTest extends TestCase {

    private TestParserCallback callback;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.callback = new TestParserCallback();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        this.callback = null;
    }

    /**
     * @throws IOException 
     * @throws ParseException 
     */
    public void testParseComment() throws ParseException, IOException {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("# SOME DESCRIPTIVE TITLE.");
        out.println("# Copyright (C) YEAR Free Software Foundation, Inc.");
        out.println("# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.");
        out.println("#");
        out.println("#, fuzzy");
        String comment = writer.toString();
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        writer.flush();
        Reader reader = new StringReader(writer.toString());
        POParser parser = new POParser(callback);
        parser.parse(reader);
        assertEquals(callback.comment.trim(), comment.trim());
    }

    /**
     * @throws IOException 
     * @throws ParseException 
     */
    public void testParseHeader() throws ParseException, IOException {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("#");
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        out.println("\"Project-Id-Version: PACKAGE VERSION\\n\"");
        out.println("\"POT-Creation-Date: 2001-02-09 01:25+0100\\n\"");
        writer.flush();
        Reader reader = new StringReader(writer.toString());
        POParser parser = new POParser(callback);
        parser.parse(reader);
        assertEquals(2, callback.headers.size());
        assertTrue(callback.headers.containsKey("Project-Id-Version"));
        assertEquals("PACKAGE VERSION", callback.headers.get("Project-Id-Version"));
        assertTrue(callback.headers.containsKey("POT-Creation-Date"));
        assertEquals("2001-02-09 01:25+0100", callback.headers.get("POT-Creation-Date"));
    }

    /**
     * @throws IOException 
     * @throws ParseException 
     */
    public void testParseEntry() throws ParseException, IOException {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("#");
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        out.println("\"Project-Id-Version: PACKAGE VERSION\\n\"");
        out.println("\"POT-Creation-Date: 2001-02-09 01:25+0100\\n\"");
        out.println();
        out.println("#: gpl.xml:11 gpl.xml:30");
        out.println("#, no-c-format");
        out.println("#. Tag: title");
        out.println("msgid \"GNU General Public License\"");
        out.println("msgstr \"test\"");
        out.println();
        out.println("#: gpl.xml:15");
        out.println("#, no-c-format");
        out.println("#, fuzzy");
        out.println("msgid \"Free Software Foundation, Inc.\"");
        out.println("msgstr \"test2\"");
        writer.flush();
        Reader reader = new StringReader(writer.toString());
        POParser parser = new POParser(callback);
        parser.parse(reader);
        assertEquals(2, callback.entries.size());
        ParserEntry entry = callback.entries.get(0);
        assertFalse(entry.isFuzzy());
        assertNotNull("test", entry.getMsgStr());
        assertEquals("gpl.xml:11 gpl.xml:30", entry.getReferences());
        assertEquals("GNU General Public License", entry.getMsgId());
        assertEquals("test", entry.getMsgStr().get(0));
        entry = callback.entries.get(1);
        assertTrue(entry.isFuzzy());
        assertEquals("gpl.xml:15", entry.getReferences());
        assertEquals("Free Software Foundation, Inc.", entry.getMsgId());
        assertEquals("test2", entry.getMsgStr().get(0));
    }

    /**
     * @throws IOException 
     * @throws ParseException 
     */
    public void testParsePluralEntry() throws ParseException, IOException {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("#");
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        out.println("\"Project-Id-Version: PACKAGE VERSION\\n\"");
        out.println("\"POT-Creation-Date: 2001-02-09 01:25+0100\\n\"");
        out.println("\"Plural-Forms: nplurals=3; plural=(n != 1);\\n\"");
        out.println();
        out.println("#: gpl.xml:11 gpl.xml:30");
        out.println("#, no-c-format");
        out.println("#. Tag: title");
        out.println("msgid \"GNU General Public License\"");
        out.println("msgid_plural \"GNU General Public Licenses\"");
        out.println("msgstr[0] \"test1\"");
        out.println("msgstr[1] \"test2\"");
        out.println("msgstr[2] \"test3\"");
        writer.flush();
        Reader reader = new StringReader(writer.toString());
        POParser parser = new POParser(callback);
        parser.parse(reader);
        assertEquals(3, callback.nplural);
        assertEquals("plural=(n != 1)", callback.pluralExpression);
        assertEquals(1, callback.entries.size());
        ParserEntry entry = callback.entries.get(0);
        assertEquals("GNU General Public Licenses", entry.getMsgIdPlural());
        assertNotNull(entry.getMsgStr());
        assertEquals(3, entry.getMsgStr().size());
        assertEquals("test1", entry.getMsgStr().get(0));
        assertEquals("test2", entry.getMsgStr().get(1));
        assertEquals("test3", entry.getMsgStr().get(2));
    }

    /**
     * @throws IOException 
     * @throws ParseException 
     */
    public void testStringNormalization() throws ParseException, IOException {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("#");
        out.println("msgid \"\"");
        out.println("msgstr \"\"");
        out.println("\"Project-Id-Version: PACKAGE VERSION\\n\"");
        out.println();
        out.println("#: gpl.xml:11 gpl.xml:30");
        out.println("msgid \"GNU \\nGeneral \\tPublic\\n \"License\"\"");
        out.println("msgstr \"test\"");
        writer.flush();
        Reader reader = new StringReader(writer.toString());
        POParser parser = new POParser(callback);
        parser.parse(reader);
        assertFalse(callback.entries.isEmpty());
        ParserEntry entry = callback.entries.get(0);
        assertEquals("GNU \nGeneral \tPublic\n \"License\"", entry.getMsgId());
    }

    private class TestParserCallback implements IPOParserCallback {

        private int nplural;

        private String pluralExpression;

        private String comment;

        private Map<String, String> headers;

        private List<ParserEntry> entries;

        private TestParserCallback() {
            this.headers = new HashMap<String, String>();
            this.entries = new ArrayList<ParserEntry>();
        }

        public void startDocument() {
        }

        public void onComment(String str) {
            this.comment = str;
        }

        public void onHeader(String name, String value) {
            headers.put(name, value);
        }

        public void onHeaderPluralForm(int nplural, String expression) {
            this.nplural = nplural;
            this.pluralExpression = expression;
        }

        public void onEntry(ParserEntry entry) {
            entries.add(entry);
        }

        public void endDocument() {
        }
    }
}
