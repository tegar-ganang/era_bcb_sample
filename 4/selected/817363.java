package org.dita.dost;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.custommonkey.xmlunit.XMLUnit;
import org.dita.dost.log.DITAOTLogger;
import org.dita.dost.util.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

public class TestUtils {

    /**
     * Create temporary directory based on test class.
     * 
     * @param testClass test class
     * @return temporary directory
     * @throws IOException if creating directory failed
     */
    public static File createTempDir(final Class<?> testClass) throws IOException {
        final File tempDir = new File(System.getProperty("java.io.tmpdir"), testClass.getName());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("Unable to create temporary directory " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    /**
     * Read file contents into a string.
     * 
     * @param file file to read
     * @return contents of the file
     * @throws IOException if reading file failed
     */
    public static String readFileToString(final File file) throws IOException {
        return readFileToString(file, false);
    }

    /**
     * Read file contents into a string.
     * 
     * @param file file to read
     * @param ignoreHead ignore first row
     * @return contents of the file
     * @throws IOException if reading file failed
     */
    public static String readFileToString(final File file, final boolean ignoreHead) throws IOException {
        final StringBuilder std = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            boolean firstLine = true;
            if (ignoreHead) {
                in.readLine();
            }
            String str;
            while ((str = in.readLine()) != null) {
                if (!firstLine) {
                    std.append("\n");
                } else {
                    firstLine = false;
                }
                std.append(str);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return std.toString();
    }

    /**
     * Read XML file contents into a string.
     * 
     * @param file XML file to read
     * @param normalize normalize whitespace
     * @return contents of the file
     * @throws Exception if parsing the file failed
     */
    public static String readXmlToString(final File file, final boolean normalize, final boolean clean) throws Exception {
        final Writer std = new CharArrayWriter();
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            final Transformer t = TransformerFactory.newInstance().newTransformer();
            XMLReader p = XMLReaderFactory.createXMLReader();
            if (normalize) {
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                p = new NormalizingXMLFilterImpl(p);
            }
            if (clean) {
                p = new CleaningXMLFilterImpl(p);
            }
            t.transform(new SAXSource(p, new InputSource(in)), new StreamResult(std));
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return std.toString();
    }

    private static class NormalizingXMLFilterImpl extends XMLFilterImpl {

        NormalizingXMLFilterImpl(final XMLReader parent) {
            super(parent);
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            final char[] res = new String(ch, start, length).replaceAll("\\s+", " ").trim().toCharArray();
            getContentHandler().characters(res, 0, res.length);
        }

        @Override
        public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
        }
    }

    private static class CleaningXMLFilterImpl extends XMLFilterImpl {

        CleaningXMLFilterImpl(final XMLReader parent) {
            super(parent);
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
            final AttributesImpl attsBuf = new AttributesImpl(atts);
            for (final String a : new String[] { "class", "domains", "xtrf", "xtrc" }) {
                final int i = attsBuf.getIndex(a);
                if (i != -1) {
                    attsBuf.removeAttribute(i);
                }
            }
            getContentHandler().startElement(uri, localName, qName, attsBuf);
        }

        @Override
        public void processingInstruction(final String target, final String data) throws SAXException {
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * 
     * @param file file or directory to delete, must not be null
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(final File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (final File c : file.listFiles()) {
                    forceDelete(c);
                }
            }
            if (!file.delete()) {
                throw new IOException("Failed to delete " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Copy directories recursively.
     * 
     * @param src source directory
     * @param dst destination directory
     * @throws IOException if copying failed
     */
    public static void copy(final File src, final File dst) throws IOException {
        if (src.isFile()) {
            FileUtils.copyFile(src, dst);
        } else {
            if (!dst.exists() && !dst.mkdirs()) {
                throw new IOException("Failed to create directory " + dst);
            }
            for (final File s : src.listFiles()) {
                copy(s, new File(dst, s.getName()));
            }
        }
    }

    /**
     * Normalize XML file.
     * 
     * @param src source XML file
     * @param dst destination XML file
     * @throws Exception if parsing or serializing failed
     */
    public static void normalize(final File src, final File dst) throws Exception {
        final Transformer serializer = TransformerFactory.newInstance().newTransformer();
        final XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setEntityResolver(new CatalogResolver());
        serializer.transform(new SAXSource(parser, new InputSource(src.toURI().toString())), new StreamResult(dst));
    }

    /**
     * Reset XMLUnit configuration.
     */
    public static void resetXMLUnit() {
        final DocumentBuilderFactory b = DocumentBuilderFactory.newInstance();
        XMLUnit.setControlDocumentBuilderFactory(b);
        XMLUnit.setTestDocumentBuilderFactory(b);
        XMLUnit.setControlEntityResolver(null);
        XMLUnit.setTestEntityResolver(null);
    }

    /**
     * DITA-OT logger that will throw an assertion error for error messages.
     */
    public static class TestLogger implements DITAOTLogger {

        public void logInfo(final String msg) {
            System.out.println(msg);
        }

        public void logWarn(final String msg) {
            System.err.println(msg);
        }

        public void logError(final String msg) {
            throw new AssertionError("Error message was thrown: " + msg);
        }

        public void logFatal(final String msg) {
            throw new AssertionError("Fatal message was thrown: " + msg);
        }

        public void logDebug(final String msg) {
            System.out.println(msg);
        }

        public void logException(final Throwable t) {
            t.printStackTrace();
            throw new AssertionError("Throwable was thrown: " + t.getMessage());
        }
    }
}
