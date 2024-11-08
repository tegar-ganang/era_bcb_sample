package ru.adv.test.xml.newt;

import ru.adv.http.Query;
import ru.adv.http.QueryFileValue;
import ru.adv.http.QueryValue;
import ru.adv.util.InputOutput;
import ru.adv.util.Stream;
import ru.adv.io.UnknownIOSourceException;
import ru.adv.xml.parser.Parser;
import ru.adv.xml.parser.ParserException;
import ru.adv.xml.parser.XmlDoc;
import ru.adv.xml.newt.NewtException;
import ru.adv.xml.newt.Newt;
import ru.adv.xml.newt.MimeMessageParser;
import ru.adv.io.InputOutputException;
import java.io.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import ru.adv.test.AbstractTest;
import ru.adv.test.TestNewtContext;

/**
 * @version \$Revision\$
 */
public class MimeMessageParserTest extends AbstractTest {

    private String NEWT = "<newt:generic xmlns:newt='http://adv.ru/newt' class='ru.adv.xml.newt.MimeMessageParser' query='message'>" + "</newt:generic>";

    private static final String SESSION_DIR = "tmp";

    @Test
    public void testStringInput() throws NewtException, ParserException, UnknownIOSourceException, IOException, InputOutputException {
        XmlDoc xmlDoc = new Parser().parse(NEWT);
        Newt newt = new MimeMessageParser();
        TestNewtContext ctx = getContext();
        Query query = new Query();
        query.add("message", readReasource("resource:///ru/adv/test/util/mail/test/multipart.msg"));
        ctx.setQuery(query);
        newt.init(xmlDoc.getDocument().getDocumentElement(), xmlDoc, ctx);
        newt.onStartTag();
        newt.onEndTag();
        ctx.getQuery().remove("message");
        assertEquals("тепа такой тест html c картинками", ctx.getQuery().getFirst("header-subject"));
        List values = ctx.getQuery().get("part[0]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        values = ctx.getQuery().get("part[1]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        values = ctx.getQuery().get("part-file[0]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        assertEquals("invalid file name:", "advlogo.gif", ((QueryFileValue) values.get(0)).getOriginalName());
    }

    @Test
    public void testFileInput() throws NewtException, ParserException, UnknownIOSourceException, IOException, InputOutputException {
        XmlDoc xmlDoc = new Parser().parse(NEWT);
        Newt newt = new MimeMessageParser();
        TestNewtContext ctx = getContext();
        Query query = new Query();
        query.add("message", saveReasource("resource:///ru/adv/test/util/mail/test/multipart.msg", "multipart.msg"));
        ctx.setQuery(query);
        newt.init(xmlDoc.getDocument().getDocumentElement(), xmlDoc, ctx);
        newt.onStartTag();
        newt.onEndTag();
        ctx.getQuery().remove("message");
        assertEquals("тепа такой тест html c картинками", ctx.getQuery().getFirst("header-subject"));
        List values = ctx.getQuery().get("part[0]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        values = ctx.getQuery().get("part[1]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        values = ctx.getQuery().get("part-file[0]");
        assertNotNull("values cannot be null", values);
        assertTrue("values cannot be empty", !values.isEmpty());
        assertTrue("first value must be file", ((QueryValue) values.get(0)).isFile());
        assertTrue("file must exist", ((File) ((QueryValue) values.get(0)).getValue()).exists());
        assertTrue("file must be readable", ((File) ((QueryValue) values.get(0)).getValue()).canRead());
        assertEquals("invalid file name:", "advlogo.gif", ((QueryFileValue) values.get(0)).getOriginalName());
    }

    private QueryValue saveReasource(String url, String filename) throws UnknownIOSourceException, IOException, InputOutputException {
        InputOutput io = InputOutput.create(url);
        InputStreamReader reader = new InputStreamReader(io.getBufferedInputStream(), "windows-1251");
        try {
            File file = new File(SESSION_DIR + "/" + filename);
            Writer writer = new BufferedWriter(new FileWriter(file));
            try {
                Stream.readTo(reader, writer);
            } finally {
                writer.close();
            }
            return new QueryFileValue(file);
        } finally {
            reader.close();
        }
    }

    private String readReasource(String systemId) throws UnknownIOSourceException, IOException, InputOutputException {
        InputOutput io = InputOutput.create(systemId);
        Reader reader = new InputStreamReader(io.getBufferedInputStream());
        try {
            StringWriter writer = new StringWriter();
            try {
                Stream.readTo(reader, writer);
                return writer.toString();
            } finally {
                writer.close();
            }
        } finally {
            reader.close();
        }
    }

    private TestNewtContext getContext() {
        TestNewtContext ctx = new TestNewtContext();
        ctx.setQuery(new Query());
        ctx.setSessionDir(SESSION_DIR);
        return ctx;
    }
}
