package hu.schmidtsoft.timeboss.server.localfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class UtilDom4j {

    public static List<Element> selectElements(Element e, String path) {
        List<Element> ret = new ArrayList<Element>();
        for (Object o : e.selectNodes(path)) {
            ret.add((Element) o);
        }
        return ret;
    }

    public static void write(File out, Document document) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        try {
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            write(osw, document);
        } finally {
            fos.close();
        }
    }

    public static void write(PrintStream out, Document document) throws IOException {
        PrintWriter pw = new PrintWriter(out);
        write(pw, document);
    }

    public static void write(OutputStream out, Document document) throws IOException {
        XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
        try {
            writer.write(document);
        } finally {
            writer.close();
        }
    }

    public static byte[] write(Document document) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        write(os, document);
        return os.toByteArray();
    }

    public static void write(Writer osw, Document document) throws IOException {
        XMLWriter writer = new XMLWriter(osw, OutputFormat.createPrettyPrint());
        try {
            writer.write(document);
        } finally {
            writer.close();
        }
    }

    public static Document read(File toPatch) throws DocumentException, MalformedURLException {
        SAXReader reader = new SAXReader();
        URL url = toPatch.toURI().toURL();
        return reader.read(url);
    }

    public static Document read(byte[] toPatch) throws DocumentException, MalformedURLException {
        SAXReader reader = new SAXReader();
        return reader.read(new ByteArrayInputStream(toPatch));
    }

    public static void deleteElements(Element root, String selector) {
        for (Element e : UtilDom4j.selectElements(root, selector)) {
            e.detach();
        }
    }

    public static void overWriteAttribute(Element root, String selector, String attName, String newValue) {
        for (Element e : UtilDom4j.selectElements(root, selector)) {
            e.addAttribute(attName, newValue);
        }
    }

    public static void format(File f) throws MalformedURLException, IOException, DocumentException {
        write(f, read(f));
    }

    public static void copy(File src, File trg) throws MalformedURLException, IOException, DocumentException {
        write(trg, read(src));
    }

    public static long getLong(Document doc, String string) {
        for (Element e : UtilDom4j.selectElements(doc.getRootElement(), string)) {
            return Long.parseLong(e.getText());
        }
        return 0;
    }

    public static String getString(Document doc, String string) {
        for (Element e : UtilDom4j.selectElements(doc.getRootElement(), string)) {
            return e.getText();
        }
        return "";
    }

    public static boolean getBoolean(Document doc, String string) {
        for (Element e : UtilDom4j.selectElements(doc.getRootElement(), string)) {
            return Boolean.parseBoolean(e.getText());
        }
        return false;
    }
}
