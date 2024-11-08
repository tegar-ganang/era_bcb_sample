package org.translationcomponent.api.impl.response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.TestCase;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.translationcomponent.api.ResponseHeader;
import org.translationcomponent.api.impl.test.MockResponseStateObserver;

public class TranslationResponseInMemoryTest extends TestCase {

    public void testHeaders() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        assertEquals("UTF-8", r.getCharacterEncoding());
        assertEquals(-1L, r.getLastModified());
        assertNotNull(r.getHeadersStorage());
        assertNull(r.getHeaders());
        r.setLastModified(100000L);
        assertEquals(100000L, r.getLastModified());
        assertFalse(r.getHeaders().isEmpty());
        {
            Set<ResponseHeader> set = new TreeSet<ResponseHeader>();
            set.add(new ResponseHeaderImpl("Last-Modified", new String[] { DateUtil.formatDate(new Date(200000L)) }));
            r.addHeaders(set);
        }
        assertEquals(1, r.getHeaders().size());
        assertEquals(200000L, r.getLastModified());
        {
            Set<ResponseHeader> set = new TreeSet<ResponseHeader>();
            set.add(new ResponseHeaderImpl("Last-Modified", new String[] { DateUtil.formatDate(new Date(310000L)) }));
            set.add(new ResponseHeaderImpl("User-Agent", new String[] { "Pinoccio" }));
            r.addHeaders(set);
        }
        assertEquals(2, r.getHeaders().size());
        int ii = 0;
        for (ResponseHeader h : r.getHeaders()) {
            ii++;
            if (ii == 1) {
                assertEquals("Last-Modified", h.getName());
                assertEquals(Arrays.toString(new String[] { DateUtil.formatDate(new Date(310000L)) }), Arrays.toString(h.getValues()));
            } else if (ii == 2) {
                assertEquals("User-Agent", h.getName());
                assertEquals(Arrays.toString(new String[] { "Pinoccio" }), Arrays.toString(h.getValues()));
            }
        }
    }

    public void testEndState() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        MockResponseStateObserver observer = new MockResponseStateObserver();
        r.addStateObserver(observer);
        assertFalse(r.hasEnded());
        assertNull(r.getEndState());
        assertEquals(0L, observer.getHits());
        r.setEndState(ResponseStateOk.getInstance());
        assertEquals(ResponseStateOk.getInstance(), r.getEndState());
        assertTrue(r.hasEnded());
        assertEquals(1L, observer.getHits());
        try {
            r.getOutputStream();
            fail("Previous line should throw IOException as result closed.");
        } catch (IOException e) {
        }
        try {
            r.getWriter();
            fail("Previous line should throw IOException as result closed.");
        } catch (IOException e) {
        }
    }

    public void testCharacterEncoding() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        assertEquals("UTF-8", r.getCharacterEncoding());
    }

    public void testTranslationCount() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        assertEquals(0, r.getTranslationCount());
        r.setTranslationCount(10);
        assertEquals(10, r.getTranslationCount());
    }

    public void testStorageString() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        r.addText("This is an example");
        r.addText(" and another one.");
        assertEquals("This is an example and another one.", r.getText());
        InputStream input = r.getInputStream();
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(input, writer, "UTF-8");
        } finally {
            input.close();
            writer.close();
        }
        assertEquals("This is an example and another one.", writer.toString());
        try {
            r.getOutputStream();
            fail("Once addText() is used the text is stored as a String and you cannot use getOutputStream anymore");
        } catch (IOException e) {
        }
        try {
            r.getWriter();
            fail("Once addText() is used the text is stored as a String and you cannot use getOutputStream anymore");
        } catch (IOException e) {
        }
        r.setEndState(ResponseStateOk.getInstance());
        assertTrue(r.hasEnded());
    }

    public void testStorageByteArray() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        {
            OutputStream output = r.getOutputStream();
            output.write("This is an example".getBytes("UTF-8"));
            output.write(" and another one.".getBytes("UTF-8"));
            assertEquals("This is an example and another one.", r.getText());
        }
        {
            InputStream input = r.getInputStream();
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(input, writer, "UTF-8");
            } finally {
                input.close();
                writer.close();
            }
            assertEquals("This is an example and another one.", writer.toString());
        }
        {
            OutputStream output = r.getOutputStream();
            output.write(" and another line".getBytes("UTF-8"));
            assertEquals("This is an example and another one. and another line", r.getText());
        }
        {
            Writer output = r.getWriter();
            output.write(" and write some more");
            assertEquals("This is an example and another one. and another line and write some more", r.getText());
        }
        {
            r.addText(" and even more.");
            assertEquals("This is an example and another one. and another line and write some more and even more.", r.getText());
        }
        assertFalse(r.hasEnded());
        r.setEndState(ResponseStateOk.getInstance());
        assertTrue(r.hasEnded());
        try {
            r.getOutputStream();
            fail("Previous line should throw IOException as result closed.");
        } catch (IOException e) {
        }
        try {
            r.getWriter();
            fail("Previous line should throw IOException as result closed.");
        } catch (IOException e) {
        }
    }

    public void testStorageStringWriter() throws Exception {
        TranslationResponseInMemory r = new TranslationResponseInMemory(2048, "UTF-8");
        {
            Writer w = r.getWriter();
            w.write("This is an example");
            w.write(" and another one.");
            w.flush();
            assertEquals("This is an example and another one.", r.getText());
        }
        {
            InputStream input = r.getInputStream();
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(input, writer, "UTF-8");
            } finally {
                input.close();
                writer.close();
            }
            assertEquals("This is an example and another one.", writer.toString());
        }
        try {
            r.getOutputStream();
            fail("Is not allowed as you already called getWriter().");
        } catch (IOException e) {
        }
        {
            Writer output = r.getWriter();
            output.write(" and another line");
            output.write(" and write some more");
            assertEquals("This is an example and another one. and another line and write some more", r.getText());
        }
        {
            r.addText(" and some more.");
            assertEquals("This is an example and another one. and another line and write some more and some more.", r.getText());
        }
        r.setEndState(ResponseStateOk.getInstance());
        assertEquals(ResponseStateOk.getInstance(), r.getEndState());
        try {
            r.getWriter();
            fail("Previous line should throw IOException as result closed.");
        } catch (IOException e) {
        }
    }
}
