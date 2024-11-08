package java.util.prefs;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.harmony.prefs.internal.nls.Messages;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utility class for the Preferences import/export from XML file.
 */
class XMLParser {

    static final String PREFS_DTD_NAME = "http://java.sun.com/dtd/preferences.dtd";

    @SuppressWarnings("nls")
    static final String PREFS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "    <!ELEMENT preferences (root)>" + "    <!ATTLIST preferences EXTERNAL_XML_VERSION CDATA \"0.0\" >" + "    <!ELEMENT root (map, node*) >" + "    <!ATTLIST root type (system|user) #REQUIRED >" + "    <!ELEMENT node (map, node*) >" + "    <!ATTLIST node name CDATA #REQUIRED >" + "    <!ELEMENT map (entry*) >" + "    <!ELEMENT entry EMPTY >" + "    <!ATTLIST entry key   CDATA #REQUIRED value CDATA #REQUIRED >";

    static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    static final String DOCTYPE = "<!DOCTYPE preferences SYSTEM";

    private static final String[] EMPTY_SARRAY = new String[0];

    private static final String FILE_PREFS = "<!DOCTYPE map SYSTEM 'http://java.sun.com/dtd/preferences.dtd'>";

    private static final float XML_VERSION = 1.0f;

    private static final DocumentBuilder builder;

    private static int indent = -1;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId.equals(PREFS_DTD_NAME)) {
                    InputSource result = new InputSource(new StringReader(PREFS_DTD));
                    result.setSystemId(PREFS_DTD_NAME);
                    return result;
                }
                throw new SAXException(Messages.getString("prefs.1", systemId));
            }
        });
        builder.setErrorHandler(new ErrorHandler() {

            public void warning(SAXParseException e) throws SAXException {
                throw e;
            }

            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        });
    }

    private XMLParser() {
    }

    @SuppressWarnings("nls")
    static void exportPrefs(Preferences prefs, OutputStream stream, boolean withSubTree) throws IOException, BackingStoreException {
        indent = -1;
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        out.write(HEADER);
        out.newLine();
        out.newLine();
        out.write(DOCTYPE);
        out.write(" '");
        out.write(PREFS_DTD_NAME);
        out.write("'>");
        out.newLine();
        out.newLine();
        flushStartTag("preferences", new String[] { "EXTERNAL_XML_VERSION" }, new String[] { String.valueOf(XML_VERSION) }, out);
        flushStartTag("root", new String[] { "type" }, new String[] { prefs.isUserNode() ? "user" : "system" }, out);
        flushEmptyElement("map", out);
        StringTokenizer ancestors = new StringTokenizer(prefs.absolutePath(), "/");
        exportNode(ancestors, prefs, withSubTree, out);
        flushEndTag("root", out);
        flushEndTag("preferences", out);
        out.flush();
        out = null;
    }

    private static void exportNode(StringTokenizer ancestors, Preferences prefs, boolean withSubTree, BufferedWriter out) throws IOException, BackingStoreException {
        if (ancestors.hasMoreTokens()) {
            String name = ancestors.nextToken();
            flushStartTag("node", new String[] { "name" }, new String[] { name }, out);
            if (ancestors.hasMoreTokens()) {
                flushEmptyElement("map", out);
                exportNode(ancestors, prefs, withSubTree, out);
            } else {
                exportEntries(prefs, out);
                if (withSubTree) {
                    exportSubTree(prefs, out);
                }
            }
            flushEndTag("node", out);
        }
    }

    private static void exportSubTree(Preferences prefs, BufferedWriter out) throws BackingStoreException, IOException {
        String[] names = prefs.childrenNames();
        if (names.length > 0) {
            for (int i = 0; i < names.length; i++) {
                Preferences child = prefs.node(names[i]);
                flushStartTag("node", new String[] { "name" }, new String[] { names[i] }, out);
                exportEntries(child, out);
                exportSubTree(child, out);
                flushEndTag("node", out);
            }
        }
    }

    private static void exportEntries(Preferences prefs, BufferedWriter out) throws BackingStoreException, IOException {
        String[] keys = prefs.keys();
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = prefs.get(keys[i], null);
        }
        exportEntries(keys, values, out);
    }

    private static void exportEntries(String[] keys, String[] values, BufferedWriter out) throws IOException {
        if (keys.length == 0) {
            flushEmptyElement("map", out);
            return;
        }
        flushStartTag("map", out);
        for (int i = 0; i < keys.length; i++) {
            if (values[i] != null) {
                flushEmptyElement("entry", new String[] { "key", "value" }, new String[] { keys[i], values[i] }, out);
            }
        }
        flushEndTag("map", out);
    }

    private static void flushEndTag(String tagName, BufferedWriter out) throws IOException {
        flushIndent(indent--, out);
        out.write("</");
        out.write(tagName);
        out.write(">");
        out.newLine();
    }

    private static void flushEmptyElement(String tagName, BufferedWriter out) throws IOException {
        flushIndent(++indent, out);
        out.write("<");
        out.write(tagName);
        out.write(" />");
        out.newLine();
        indent--;
    }

    private static void flushEmptyElement(String tagName, String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        flushIndent(++indent, out);
        out.write("<");
        out.write(tagName);
        flushPairs(attrKeys, attrValues, out);
        out.write(" />");
        out.newLine();
        indent--;
    }

    private static void flushPairs(String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        for (int i = 0; i < attrKeys.length; i++) {
            out.write(" ");
            out.write(attrKeys[i]);
            out.write("=\"");
            out.write(htmlEncode(attrValues[i]));
            out.write("\"");
        }
    }

    private static void flushIndent(int ind, BufferedWriter out) throws IOException {
        for (int i = 0; i < ind; i++) {
            out.write("  ");
        }
    }

    private static void flushStartTag(String tagName, String[] attrKeys, String[] attrValues, BufferedWriter out) throws IOException {
        flushIndent(++indent, out);
        out.write("<");
        out.write(tagName);
        flushPairs(attrKeys, attrValues, out);
        out.write(">");
        out.newLine();
    }

    private static void flushStartTag(String tagName, BufferedWriter out) throws IOException {
        flushIndent(++indent, out);
        out.write("<");
        out.write(tagName);
        out.write(">");
        out.newLine();
    }

    private static String htmlEncode(String s) {
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch(c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '\\':
                    sb.append("&apos;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    static void importPrefs(InputStream in) throws IOException, InvalidPreferencesFormatException {
        try {
            Document doc = builder.parse(new InputSource(in));
            Element preferences;
            preferences = doc.getDocumentElement();
            String version = preferences.getAttribute("EXTERNAL_XML_VERSION");
            if (version != null && Float.parseFloat(version) > XML_VERSION) {
                throw new InvalidPreferencesFormatException(Messages.getString("prefs.2", version));
            }
            Element root = (Element) preferences.getElementsByTagName("root").item(0);
            Preferences prefsRoot = null;
            String type = root.getAttribute("type");
            if (type.equals("user")) {
                prefsRoot = Preferences.userRoot();
            } else {
                prefsRoot = Preferences.systemRoot();
            }
            loadNode(prefsRoot, root);
        } catch (FactoryConfigurationError e) {
            throw new InvalidPreferencesFormatException(e);
        } catch (SAXException e) {
            throw new InvalidPreferencesFormatException(e);
        } catch (TransformerException e) {
            throw new InvalidPreferencesFormatException(e);
        }
    }

    private static void loadNode(Preferences prefs, Element node) throws TransformerException {
        NodeList children = XPathAPI.selectNodeList(node, "node");
        NodeList entries = XPathAPI.selectNodeList(node, "map/entry");
        int childNumber = children.getLength();
        Preferences[] prefChildren = new Preferences[childNumber];
        int entryNumber = entries.getLength();
        synchronized (((AbstractPreferences) prefs).lock) {
            if (((AbstractPreferences) prefs).isRemoved()) {
                return;
            }
            for (int i = 0; i < entryNumber; i++) {
                Element entry = (Element) entries.item(i);
                String key = entry.getAttribute("key");
                String value = entry.getAttribute("value");
                prefs.put(key, value);
            }
            for (int i = 0; i < childNumber; i++) {
                Element child = (Element) children.item(i);
                String name = child.getAttribute("name");
                prefChildren[i] = prefs.node(name);
            }
        }
        for (int i = 0; i < childNumber; i++) {
            loadNode(prefChildren[i], (Element) children.item(i));
        }
    }

    static Properties loadFilePrefs(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Properties>() {

            public Properties run() {
                return loadFilePrefsImpl(file);
            }
        });
    }

    static Properties loadFilePrefsImpl(final File file) {
        Properties result = new Properties();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return result;
        }
        if (file.canRead()) {
            InputStream in = null;
            FileLock lock = null;
            try {
                FileInputStream istream = new FileInputStream(file);
                in = new BufferedInputStream(istream);
                FileChannel channel = istream.getChannel();
                lock = channel.lock(0L, Long.MAX_VALUE, true);
                Document doc = builder.parse(in);
                NodeList entries = XPathAPI.selectNodeList(doc.getDocumentElement(), "entry");
                int length = entries.getLength();
                for (int i = 0; i < length; i++) {
                    Element node = (Element) entries.item(i);
                    String key = node.getAttribute("key");
                    String value = node.getAttribute("value");
                    result.setProperty(key, value);
                }
                return result;
            } catch (IOException e) {
            } catch (SAXException e) {
            } catch (TransformerException e) {
                throw new AssertionError(e);
            } finally {
                releaseQuietly(lock);
                closeQuietly(in);
            }
        } else {
            file.delete();
        }
        return result;
    }

    static void flushFilePrefs(final File file, final Properties prefs) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

            public Object run() throws IOException {
                flushFilePrefsImpl(file, prefs);
                return null;
            }
        });
    }

    static void flushFilePrefsImpl(File file, Properties prefs) throws IOException {
        BufferedWriter out = null;
        FileLock lock = null;
        try {
            FileOutputStream ostream = new FileOutputStream(file);
            out = new BufferedWriter(new OutputStreamWriter(ostream, "UTF-8"));
            FileChannel channel = ostream.getChannel();
            lock = channel.lock();
            out.write(HEADER);
            out.newLine();
            out.write(FILE_PREFS);
            out.newLine();
            if (prefs.size() == 0) {
                exportEntries(EMPTY_SARRAY, EMPTY_SARRAY, out);
            } else {
                String[] keys = prefs.keySet().toArray(new String[prefs.size()]);
                int length = keys.length;
                String[] values = new String[length];
                for (int i = 0; i < length; i++) {
                    values[i] = prefs.getProperty(keys[i]);
                }
                exportEntries(keys, values, out);
            }
            out.flush();
        } finally {
            releaseQuietly(lock);
            closeQuietly(out);
        }
    }

    private static void releaseQuietly(FileLock lock) {
        if (lock == null) {
            return;
        }
        try {
            lock.release();
        } catch (IOException e) {
        }
    }

    private static void closeQuietly(Writer out) {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
        }
    }
}
