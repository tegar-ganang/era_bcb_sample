package org.pagger.data.picture.xmp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.formats.jpeg.xmp.JpegXmpRewriter;
import org.pagger.util.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author Gerd Saurer
 */
public class XmpUtil {

    private static final DocumentBuilderFactory DOC_FACTORY = DocumentBuilderFactory.newInstance();

    private static final TransformerFactory TRANS_FACTORY = TransformerFactory.newInstance();

    private XmpUtil() {
    }

    public static XmpRawMetadata parse(String xmpString) throws IOException {
        try {
            DocumentBuilder builder = DOC_FACTORY.newDocumentBuilder();
            Document doc = null;
            if (xmpString != null) {
                doc = builder.parse(IOUtils.toInputStream(xmpString));
            } else {
                doc = builder.newDocument();
                final Node rootNode = doc.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF");
                doc.appendChild(rootNode);
            }
            return new XmpRawMetadata(doc);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    public static String transform(XmpRawMetadata metadata) throws IOException {
        try {
            Transformer transformer = TRANS_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(metadata.getDocument());
            transformer.transform(source, result);
            String xmpString = result.getWriter().toString();
            return xmpString;
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }

    public static void write(final File location, final XmpRawMetadata metadata) throws IOException {
        Validator.notNull(location, "Image location");
        Validator.notNull(metadata, "Metadata");
        try {
            final String fileName = location.getName();
            final File tempLocation = File.createTempFile("pagger-" + System.currentTimeMillis(), "." + fileName.substring(fileName.lastIndexOf(".") + 1));
            OutputStream out = null;
            try {
                String xmpString = transform(metadata);
                final JpegXmpRewriter writer = new JpegXmpRewriter();
                out = new FileOutputStream(tempLocation);
                writer.updateXmpXml(location, out, xmpString);
            } finally {
                IOUtils.closeQuietly(out);
            }
            FileUtils.copyFile(tempLocation, location);
        } catch (ImageReadException ex) {
            throw new IOException(ex);
        } catch (ImageWriteException ex) {
            throw new IOException(ex);
        }
    }
}
