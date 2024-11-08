package org.xaware.ide.xadev.conversion;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.util.ContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilderImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMWriter;
import org.eclipse.wst.xml.ui.internal.wizards.NewXMLGenerator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.server.common.XMLNamespaceUtil;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Class used by the BizView predictor to predict input XML
 * from a xa:input_type attribute and schema location
 *
 * @author jtarnowski
 */
public class InputFromSchemaPredictor {

    /** Logger for XAware. */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger("org.xaware.ide.xadev.conversion.InputFromSchemaPredictor");

    /**
     * Get the expected outcome of a bizdoc for input XML
     * 
     * @param root - Element that is the root of the BizView
     * @return Element - the root of the expected input XML
     */
    public static Element getExpectedOutcome(final Element root, boolean isChild, final String path) {
        Element result = null;
        String name = null;
        String prefix = "";
        String namespaceURI = "";
        String uri = null;
        String schemaLocations = null;
        try {
            if (!isChild) {
                name = root.getAttributeValue("input_type", XAwareConstants.xaNamespace);
            } else {
                name = root.getAttributeValue("response_type", XAwareConstants.xaNamespace);
            }
            if (name != null) {
                final int index = name.indexOf(':');
                if (index >= 0) {
                    prefix = name.substring(0, index);
                    name = name.substring(index + 1);
                    namespaceURI = root.getNamespace(prefix).getURI();
                }
                if (prefix.equals("")) {
                    schemaLocations = root.getAttributeValue("noNamespaceSchemaLocation", Namespace.getNamespace("http://www.w3.org/2001/XMLSchema-instance"));
                } else {
                    schemaLocations = root.getAttributeValue("schemaLocation", Namespace.getNamespace("http://www.w3.org/2001/XMLSchema-instance"));
                }
                if (schemaLocations != null) {
                    uri = getSchemaLocation(namespaceURI, schemaLocations);
                    if (uri != null) {
                        final StringWriter writer = buildXML(name, uri, path);
                        if (writer != null) {
                            result = getJDOMresult(writer);
                            XMLNamespaceUtil.resolveNamespaceConflicts(result, root);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            ControlFactory.showInfoDialog("Unable to predict input XML " + uri, "Unable  to predict input XML. uri=" + uri);
            logger.debug("Unable to predict input XML: " + e.getLocalizedMessage(), "InputFromSchemaPredictor", "getExpectedOutcome");
        }
        return result;
    }

    /**
     * Get the schema location
     * 
     * @param namespaceURI - String
     * @param locations - String with url/location pairs or a single location
     * @return
     */
    static String getSchemaLocation(final String namespaceURI, final String locations) {
        String result = null;
        final String[] pairs = locations.split(" ");
        if (pairs.length == 1) {
            result = pairs[0];
        } else {
            for (int i = 0; i < pairs.length; i += 2) {
                if (pairs[i].equals(namespaceURI)) {
                    result = pairs[i + 1].trim();
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Build the instance XML
     * 
     * @param name - String name of the top-level element
     * @param uri - String where the schema can be found
     * @return StringWriter - ready to output the XML
     */
    static StringWriter buildXML(final String name, final String uri, final String filePath) throws Exception {
        StringWriter stringWriter = null;
        CMDocument cmDoc = null;
        final String error[] = new String[2];
        final NewXMLGenerator g = new NewXMLGenerator();
        String path;
        cmDoc = NewXMLGenerator.createCMDocument(uri, error);
        if (cmDoc == null) {
            path = XA_Designer_Plugin.getActiveEditedFileDirectory();
            cmDoc = NewXMLGenerator.createCMDocument("file:///" + path + '/' + uri, error);
        }
        if (cmDoc == null) {
            path = "file:///" + filePath + '/' + uri;
            cmDoc = NewXMLGenerator.createCMDocument(path, error);
        }
        if (cmDoc == null) {
            final String auri = ResourceUtils.getAbsolutePath(uri);
            cmDoc = NewXMLGenerator.createCMDocument("file:///" + auri, error);
        }
        g.setCMDocument(cmDoc);
        g.createNamespaceInfoList();
        final CMNamedNodeMap nameNodeMap = cmDoc.getElements();
        final CMElementDeclaration cmElementDeclaration = (CMElementDeclaration) nameNodeMap.getNamedItem(name);
        final org.w3c.dom.Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final DOMContentBuilderImpl contentBuilder = new DOMContentBuilderImpl(xmlDocument);
        contentBuilder.supressCreationOfDoctypeAndXMLDeclaration = true;
        contentBuilder.setBuildPolicy(ContentBuilder.BUILD_ONLY_REQUIRED_CONTENT);
        contentBuilder.createDefaultRootContent(cmDoc, cmElementDeclaration, g.namespaceInfoList);
        stringWriter = new StringWriter();
        final DOMWriter domWriter = new DOMWriter(stringWriter);
        domWriter.print(xmlDocument);
        return stringWriter;
    }

    /**
     * Get contents out of a StringWriter and return JDOM tree
     * 
     * @param writer StringWriter
     * @return Element
     */
    static Element getJDOMresult(final StringWriter writer) {
        Element result = null;
        final StringReader reader = new StringReader(writer.toString());
        try {
            final SAXBuilder builder = new SAXBuilder();
            final Document doc = builder.build(reader);
            result = (Element) doc.getRootElement().detach();
        } catch (final Exception e) {
        }
        return result;
    }
}
