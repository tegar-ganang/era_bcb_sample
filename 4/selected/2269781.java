package com.rhythm.commons.csv;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CSVReaderTest {

    private CSVReader csvr;

    @Before
    public void setUp() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("a,b,c").append("\n");
        sb.append("a,\"b,b,b\",c").append("\n");
        sb.append(",,").append("\n");
        sb.append("a,\"PO Box 123,\nKippax,ACT. 2615.\nAustralia\",d.\n");
        sb.append("\"Glen \"\"The Man\"\" Smith\",Athlete,Developer\n");
        sb.append("\"\"\"\"\"\",\"test\"\n");
        sb.append("\"a\nb\",b,\"\nd\",e\n");
        csvr = new CSVReader(new StringReader(sb.toString()));
    }

    @Test
    public void testParseLine() throws IOException {
        String[] nextLine = csvr.readNext();
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        nextLine = csvr.readNext();
        assertEquals("a", nextLine[0]);
        assertEquals("b,b,b", nextLine[1]);
        assertEquals("c", nextLine[2]);
        nextLine = csvr.readNext();
        assertEquals(3, nextLine.length);
        nextLine = csvr.readNext();
        assertEquals(3, nextLine.length);
        nextLine = csvr.readNext();
        assertEquals("Glen \"The Man\" Smith", nextLine[0]);
        nextLine = csvr.readNext();
        assertTrue(nextLine[0].equals("\"\""));
        assertTrue(nextLine[1].equals("test"));
        nextLine = csvr.readNext();
        assertEquals(4, nextLine.length);
        assertEquals(null, csvr.readNext());
    }

    @Test
    public void testParseAll() throws IOException {
        List allElements = csvr.readAll();
        assertEquals(7, allElements.size());
    }

    @Test
    public void testOptionalConstructors() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("a\tb\tc").append("\n");
        sb.append("a\t'b\tb\tb'\tc").append("\n");
        CSVReader c = new CSVReader(new StringReader(sb.toString()), '\t', '\'');
        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
        nextLine = c.readNext();
        assertEquals(3, nextLine.length);
    }

    @Test
    public void testSkippingLines() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("Skip this line\t with tab").append("\n");
        sb.append("And this line too").append("\n");
        sb.append("a\t'b\tb\tb'\tc").append("\n");
        CSVReader c = new CSVReader(new StringReader(sb.toString()), '\t', '\'', 2);
        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
    }

    @Test
    public void testParsedLineWithInternalQuota() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("a,123\"4\"567,c").append("\n");
        CSVReader c = new CSVReader(new StringReader(sb.toString()));
        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
        System.out.println(nextLine[1]);
        assertEquals("123\"4\"567", nextLine[1]);
    }
}
