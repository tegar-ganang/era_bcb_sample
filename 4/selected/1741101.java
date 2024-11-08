package main.java.org.squidy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nux.xom.xquery.XQueryException;
import nux.xom.xquery.XQueryUtil;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Utility class that provides a framework for XML related operations.
 *
 * @author Luc Giroux
 * @version $Revision: 91 $
 */
public class XmlUtil {

    private static XMLReader parser;

    static {
        try {
            parser = XMLReaderFactory.createXMLReader();
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private static final Builder BUILDER = new Builder(parser);

    /**
	 * To prevent instanciation.
	 */
    private XmlUtil() {
    }

    /**
	 * Load a document from a specified File.
	 */
    public static Document loadDocument(final File file) throws IOException, ParsingException {
        assert file != null;
        assert file.exists();
        assert file.isFile();
        assert file.canRead();
        final Document document = BUILDER.build(file);
        return document;
    }

    /**
	 * Load a document from a specified URL.ad
	 */
    public static Document loadDocument(final URL url) throws IOException, ParsingException {
        assert url != null;
        return loadDocument(url.toString());
    }

    /**
	 * Load a document from a specified URL.
	 */
    public static Document loadDocument(final String url) throws IOException, ParsingException {
        assert url != null;
        assert url.length() > 0;
        if (new File(url).exists()) {
            return loadDocument(new File(url));
        }
        return BUILDER.build(url);
    }

    /**
	 * Run an XQuery against a given document.
	 *
	 * @return <code>null</code> if there is no such node in the document ; a
	 * <code>String</code> representation of the return value otherwise.
	 * @throws Exception 
	 */
    public static String selectStringValue(final Document document, final String query) throws Exception {
        assert document != null;
        assert query != null;
        assert query.length() > 0;
        final Nodes results = XQueryUtil.xquery(document, query);
        if (results.size() == 0) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0).getValue();
        } else throw new Exception("there is more than 1 result for the query " + query);
    }

    /**
	 * Run an XQuery against a given document.
	 */
    public static boolean selectBooleanValue(final Document document, final String query) throws Exception {
        assert document != null;
        assert query != null;
        assert query.length() > 0;
        final String stringValue = selectStringValue(document, query);
        assert stringValue != null;
        assert stringValue.equals("true") || stringValue.equals("false");
        return Boolean.parseBoolean(stringValue);
    }

    /**
	 * Run an XQuery against a given document.
	 *
	 * @return a list of <code>String</code> representations of the values
	 * returned by the query.
	 */
    public static List<String> selectStringValues(final Document document, final String query) throws XQueryException {
        assert document != null;
        assert query != null;
        assert query.length() > 0;
        final Nodes results = XQueryUtil.xquery(document, query);
        final List<String> values = new ArrayList<String>(results.size());
        for (int i = 0; i < results.size(); i++) {
            final String value = results.get(i).getValue();
            values.add(value);
        }
        return values;
    }

    /**
	 * Build a new document given a set of child nodes.
	 *
	 * @param nodes the child nodes to be added to the root element.
	 */
    public static Document newDocument(final Nodes nodes) {
        assert nodes != null;
        if ((nodes.size() == 1) && nodes.get(0) instanceof Element) {
            final Element rootElement = (Element) nodes.get(0).copy();
            final Document document = new Document(rootElement);
            return document;
        }
        return newDocument(nodes, "nodes");
    }

    /**
	 * Build a new document given a set of child nodes and the name of the root
	 * element.
	 *
	 * @param nodes the child nodes to be added to the root element.
	 * @param rootElementName the name of the root element.
	 */
    public static Document newDocument(final Nodes nodes, final String rootElementName) {
        assert nodes != null;
        assert rootElementName != null;
        assert rootElementName.length() > 0;
        final Element rootElement = new Element(rootElementName);
        final Document document = new Document(rootElement);
        for (int i = 0; i < nodes.size(); i++) {
            final Node childNode = nodes.get(i).copy();
            rootElement.appendChild(childNode);
        }
        return document;
    }

    /**
	 * Check whether two documents are equals as per the XPath deep-equal()
	 * function.<p><code>saxon:deep-equal()</code> function is used rather than
	 * the standard one, because it allows us more flexibility, such as the
	 * ability to ignore whitespaces in empty text nodes.</p>
	 *
	 * @param a some document.
	 *
	 * @param b some other document.
	 *
	 * @return <code>true</code> if the two documents are equal as per the
	 * XPath <code>deep-equal</code> function.
	 */
    public static boolean equals(final Document a, final Document b) {
        if (a == null) {
            if (b == null) {
                return true;
            }
            return false;
        }
        final Element elementA = new Element("element");
        elementA.appendChild(a.getRootElement().copy());
        final Element elementB = new Element("element");
        elementB.appendChild(b.getRootElement().copy());
        final Element root = new Element("root");
        root.appendChild(elementA);
        root.appendChild(elementB);
        final Document both = new Document(root);
        final Nodes resultNodes = XQueryUtil.xquery(both, "saxon:deep-equal(/root/element[1], /root/element[2], (), 'w')");
        assert resultNodes.size() == 1;
        final String result = resultNodes.get(0).getValue();
        assert result.equals("true") || result.equals("false");
        return result.equals("true");
    }

    /**
	 * Save a document to a temporary file.
	 *
	 * @return the path of the temporary file.
	 */
    public static String saveDocumentToTempFile(final Document document) throws IOException {
        assert document != null;
        final File tempFile = File.createTempFile(XmlUtil.class.getName(), ".xml");
        final FileOutputStream out = new FileOutputStream(tempFile);
        try {
            final Serializer serializer = new Serializer(out, "ISO-8859-1");
            serializer.setIndent(2);
            serializer.write(document);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempFile.toString();
    }

    /**
	 * Save a document to a file.
	 *
	 */
    public static void saveDocument(final Document document) throws IOException {
        assert document != null;
        final String tmp = document.getBaseURI();
        final String tmpPath = tmp.substring(6);
        final String[] t = tmp.split("/");
        final String s = t[(t.length) - 1];
        final String name = s.substring(0, s.length() - 4);
        final String path = tmpPath.substring(0, tmpPath.length() - (4 + name.length()));
        final File dir = new File(path);
        final File file = new File(path + "/" + name + ".xml");
        assert file.createNewFile();
        final FileOutputStream out = new FileOutputStream(file);
        try {
            final Serializer serializer = new Serializer(out, "ISO-8859-1");
            serializer.setIndent(2);
            serializer.write(document);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Method for copy a file
	 * 
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void copyFile(File src, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        FileChannel channelSrc = fis.getChannel();
        FileChannel channelDest = fos.getChannel();
        channelSrc.transferTo(0, channelSrc.size(), channelDest);
        fis.close();
        fos.close();
    }
}
