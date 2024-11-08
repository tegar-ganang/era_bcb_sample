package com.google.gson.functional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonStreamParser;
import com.google.gson.common.TestTypes.BagOfPrimitives;
import junit.framework.TestCase;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Functional tests for the support of {@link Reader}s and {@link Writer}s.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public class ReadersWritersTest extends TestCase {

    private Gson gson;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gson = new Gson();
    }

    public void testWriterForSerialization() throws Exception {
        Writer writer = new StringWriter();
        BagOfPrimitives src = new BagOfPrimitives();
        gson.toJson(src, writer);
        assertEquals(src.getExpectedJson(), writer.toString());
    }

    public void testReaderForDeserialization() throws Exception {
        BagOfPrimitives expected = new BagOfPrimitives();
        Reader json = new StringReader(expected.getExpectedJson());
        BagOfPrimitives actual = gson.fromJson(json, BagOfPrimitives.class);
        assertEquals(expected, actual);
    }

    public void testTopLevelNullObjectSerializationWithWriter() {
        StringWriter writer = new StringWriter();
        gson.toJson(null, writer);
        assertEquals("", writer.toString());
    }

    public void testTopLevelNullObjectDeserializationWithReader() {
        StringReader reader = new StringReader("null");
        Integer nullIntObject = gson.fromJson(reader, Integer.class);
        assertNull(nullIntObject);
    }

    public void testTopLevelNullObjectSerializationWithWriterAndSerializeNulls() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        StringWriter writer = new StringWriter();
        gson.toJson(null, writer);
        assertEquals("null", writer.toString());
    }

    public void testTopLevelNullObjectDeserializationWithReaderAndSerializeNulls() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        StringReader reader = new StringReader("null");
        Integer nullIntObject = gson.fromJson(reader, Integer.class);
        assertNull(nullIntObject);
    }

    public void testReadWriteTwoStrings() throws IOException {
        Gson gson = new Gson();
        CharArrayWriter writer = new CharArrayWriter();
        writer.write(gson.toJson("one").toCharArray());
        writer.write(gson.toJson("two").toCharArray());
        CharArrayReader reader = new CharArrayReader(writer.toCharArray());
        JsonStreamParser parser = new JsonStreamParser(reader);
        String actualOne = gson.fromJson(parser.next(), String.class);
        assertEquals("one", actualOne);
        String actualTwo = gson.fromJson(parser.next(), String.class);
        assertEquals("two", actualTwo);
    }

    public void testReadWriteTwoObjects() throws IOException {
        Gson gson = new Gson();
        CharArrayWriter writer = new CharArrayWriter();
        BagOfPrimitives expectedOne = new BagOfPrimitives(1, 1, true, "one");
        writer.write(gson.toJson(expectedOne).toCharArray());
        BagOfPrimitives expectedTwo = new BagOfPrimitives(2, 2, false, "two");
        writer.write(gson.toJson(expectedTwo).toCharArray());
        CharArrayReader reader = new CharArrayReader(writer.toCharArray());
        JsonStreamParser parser = new JsonStreamParser(reader);
        BagOfPrimitives actualOne = gson.fromJson(parser.next(), BagOfPrimitives.class);
        assertEquals("one", actualOne.stringValue);
        BagOfPrimitives actualTwo = gson.fromJson(parser.next(), BagOfPrimitives.class);
        assertEquals("two", actualTwo.stringValue);
        assertFalse(parser.hasNext());
    }
}
