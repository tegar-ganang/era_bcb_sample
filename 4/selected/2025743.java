package be.lassi.web;

import static org.testng.Assert.assertEquals;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ParseTestCase {

    @Test
    public void test1() throws IOException, SAXException {
        String string = "<?xml version=\"1.0\"?>\n" + "<html>\n" + "<a name=\"anchor1\"/><h1>chapter1</h1>\n" + "<a name=\"anchor1.1\"/><h2>section1.1</h2>\n" + "<a name=\"anchor1.2\"/><h2>section1.2</h2>\n" + "<a name=\"anchor1.2.1\"/><h3>section1.2.1</h3>\n" + "<a name=\"anchor1.2.2\"/><h3>section1.2.2</h3>\n" + "<a href=\"to-be-ingored\">link</a>\n" + "<a name=\"anchor2\"/><h1>chapter2</h1>\n" + "<a name=\"anchor2.1\"/><h2>section2.1</h2>\n" + "<a name=\"anchor2.2\"/><h2>section2.2</h2>\n" + "</html>\n";
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        Reader reader = new StringReader(string);
        new Parse(writer).parse("filename", reader);
        writer.close();
        String result = sw.toString();
        String expected = "" + "<div class=\"toc-1\"><span class=\"toc-1-nr\">1</span><a href=\"filename#anchor1\">chapter1</a></div>\n" + "<div class=\"toc-2\"><span class=\"toc-2-nr\">1.1</span><a href=\"filename#anchor1.1\">section1.1</a></div>\n" + "<div class=\"toc-2\"><span class=\"toc-2-nr\">1.2</span><a href=\"filename#anchor1.2\">section1.2</a></div>\n" + "<div class=\"toc-3\"><span class=\"toc-3-nr\">1.2.1</span><a href=\"filename#anchor1.2.1\">section1.2.1</a></div>\n" + "<div class=\"toc-3\"><span class=\"toc-3-nr\">1.2.2</span><a href=\"filename#anchor1.2.2\">section1.2.2</a></div>\n" + "<div class=\"toc-1\"><span class=\"toc-1-nr\">2</span><a href=\"filename#anchor2\">chapter2</a></div>\n" + "<div class=\"toc-2\"><span class=\"toc-2-nr\">2.1</span><a href=\"filename#anchor2.1\">section2.1</a></div>\n" + "<div class=\"toc-2\"><span class=\"toc-2-nr\">2.2</span><a href=\"filename#anchor2.2\">section2.2</a></div>\n";
        System.out.println(result);
        assertEquals(result, expected);
    }

    @Test
    public void test2() {
        String directory = "/lassi/workspace/lassi/src/help/pages/manual/ui/";
        String[] filenames = { "sheet.html", "groups.html", "log.html", "patch.html" };
        try {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);
            Parse parse = new Parse(writer);
            for (int i = 0; i < filenames.length; i++) {
                String filename = directory + filenames[i];
                System.out.println("Processing " + filename);
                Reader reader = new FileReader(filename);
                try {
                    parse.parse(filename, reader);
                } finally {
                    reader.close();
                }
            }
            writer.close();
            System.out.println(sw.toString());
        } catch (SAXParseException e) {
            System.out.println("XML error");
            System.out.println("  message=" + e.getMessage());
            System.out.println("  line=" + e.getLineNumber());
            System.out.println("  column=" + e.getColumnNumber());
            System.out.println("  public id=" + e.getPublicId());
            System.out.println("  system id=" + e.getSystemId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
