package com.tecacet.jflat;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class DefaultCSVReaderTest {

    CSVReader<String[]> csvr;

    /**
     * Setup the test.
     */
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
        csvr = new DefaultCSVReader(new StringReader(sb.toString()));
    }

    /**
     * Tests iterating over a reader.
     * 
     * @throws IOException
     *             if the reader fails.
     */
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

    /**
     * Test parsing to a list.
     * 
     * @throws IOException
     *             if the reader fails.
     */
    @Test
    public void testParseAll() throws IOException {
        List<String[]> allElements = csvr.readAll();
        assertEquals(7, allElements.size());
    }

    /**
     * Tests constructors with optional delimiters and optional quote char.
     * 
     * @throws IOException
     *             if the reader fails.
     */
    @Test
    public void testOptionalConstructors() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("a\tb\tc").append("\n");
        sb.append("a\t'b\tb\tb'\tc").append("\n");
        CSVReader<String[]> c = new DefaultCSVReader(new StringReader(sb.toString()));
        c.setQuotechar('\'');
        c.setSeparator('\t');
        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
        nextLine = c.readNext();
        assertEquals(3, nextLine.length);
    }

    /**
     * Tests option to skip the first few lines of a file.
     * 
     * @throws IOException
     *             if bad things happen
     */
    @Test
    public void testSkippingLines() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("Skip this line\t with tab").append("\n");
        sb.append("And this line too").append("\n");
        sb.append("a\t'b\tb\tb'\tc").append("\n");
        CSVReader<String[]> reader = new DefaultCSVReader(new StringReader(sb.toString()));
        reader.setQuotechar('\'');
        reader.setSeparator('\t');
        reader.setSkipLines(2);
        List<String[]> lines = reader.readAll();
        String[] nextLine = lines.get(0);
        assertEquals(3, nextLine.length);
        assertEquals("a", nextLine[0]);
    }

    /**
     * Tests quotes in the middle of an element.
     * 
     * @throws IOException
     *             if bad things happen
     */
    @Test
    public void testParsedLineWithInternalQuota() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("a,123\"4\"567,c").append("\n");
        CSVReader<String[]> c = new DefaultCSVReader(new StringReader(sb.toString()));
        String[] nextLine = c.readNext();
        assertEquals(3, nextLine.length);
        System.out.println(nextLine[1]);
        assertEquals("123\"4\"567", nextLine[1]);
    }

    @Test
    public void testLineCount() throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("a,b,c").append("\n");
        sb.append("a,\"b,b,b\",c").append("\n");
        sb.append(",,").append("\n");
        CSVReader<String[]> reader = new CSVReader<String[]>(new StringReader(sb.toString()), new ReaderRowMapper<String[]>() {

            int i = 1;

            public String[] getRow(String[] row, int rowNumber) {
                assertEquals(i, rowNumber);
                i++;
                return row;
            }
        });
        reader.readAll();
    }
}
