package javax.jcr.tools.backup.impl.nodes;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.tools.backup.Context;
import static javax.jcr.tools.backup.impl.nodes.BackupFormatConstants.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * An exporter for non-versionable nodes. Binary properties are exported as standalone files.
 * Non-binary jcr properties are not exported.
 * @author Alex Karshakevich
 * Date:    Aug 19, 2007
 * Time:    5:21:14 PM
 */
public class SimpleNodeExporter implements NodeExporter {

    private Logger logger = Logger.getLogger(getClass());

    /**
   * Export a node as SAX events, sans child nodes
   * @param node         node to export
   * @param context      backup context
   * @param ch           target ContentHandler for export
   * @param attr empty attributes holder to reduce object creation
   * @throws org.xml.sax.SAXException      on SAX-related errors
   * @throws javax.jcr.RepositoryException on JCR-related errors
   */
    public void exportNode(Node node, Context context, ContentHandler ch, AttributesImpl attr) throws SAXException, RepositoryException {
        try {
            NodeType[] types = node.getMixinNodeTypes();
            for (NodeType type : types) {
                attr.clear();
                attr.addAttribute("", NAME, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + NAME, "string", type.getName());
                ch.startElement("", MIXIN, MIXIN, attr);
                ch.endElement("", MIXIN, MIXIN);
            }
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                toPropertySAX(properties.nextProperty(), attr, ch, context);
            }
        } catch (RepositoryException e) {
            String path = null;
            try {
                path = node.getPath();
            } catch (RepositoryException e1) {
            }
            logger.error("Could not backup node [" + (path == null ? "{path unavailable for node [" + node + "]}" : path) + "]", e);
        }
    }

    public void nodeStartElement(Node node, Context context, ContentHandler ch, AttributesImpl attr) throws SAXException, RepositoryException {
        attr.clear();
        attr.addAttribute("", NAME, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + NAME, "string", node.getName());
        attr.addAttribute("", TYPE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + TYPE, "string", node.getPrimaryNodeType().getName());
        if (node.isNodeType("mix:referenceable")) {
            try {
                attr.addAttribute("", UUID, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + UUID, "string", node.getUUID());
            } catch (RepositoryException e) {
                logger.warn("Unable to retrieve UUID from node {" + node.getName() + "}", e);
            }
        }
        attr.addAttribute("", PATH, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + PATH, "string", node.getPath());
        ch.startElement("", NODE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + NODE, attr);
    }

    public void nodeEndElement(Node node, Context context, ContentHandler ch) throws SAXException, RepositoryException {
        ch.endElement("", NODE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + NODE);
    }

    /**
   * Constrcut SAX Source from given Property
   *
   * @param property property to transfer
   * @param attr attributes impl, for performance sake will not create Attributes intance for each node, but reuse the given one.
   * @param contentHandler content handler
   * @param context backup context
   */
    public static void toPropertySAX(Property property, AttributesImpl attr, ContentHandler contentHandler, Context context) throws RepositoryException, SAXException {
        if (null == property) return;
        if (isIgnorable(property)) return;
        attr.clear();
        attr.addAttribute("", NAME, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + NAME, "string", property.getName());
        attr.addAttribute("", TYPE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + TYPE, "string", PropertyType.nameFromValue(property.getType()));
        attr.addAttribute("", MULTIPLE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + MULTIPLE, "string", String.valueOf(property.getDefinition().isMultiple()));
        contentHandler.startElement("", PROPERTY, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + PROPERTY, attr);
        if (property.getDefinition().isMultiple()) {
            for (Value _value : property.getValues()) {
                toValueSAX(property, _value, property.getType(), contentHandler, attr, context);
            }
        } else {
            toValueSAX(property, property.getValue(), property.getType(), contentHandler, attr, context);
        }
        contentHandler.endElement("", PROPERTY, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + PROPERTY);
    }

    /**
   * A facility to exclude certain nodes from backup
   * @param property property to be backed up
   * @return true iff this property should not be backed up
   * @throws RepositoryException on errors accessing the property
   */
    protected static boolean isIgnorable(Property property) throws RepositoryException {
        return property.getType() != PropertyType.BINARY && property.getName().startsWith("jcr:");
    }

    /**
   * Construct SAX Source from given property value
   * @param property property this value belongs to
   * @param value value
   * @param valueType value type
   * @param contentHandler content handler
   * @param na
   * @param context backup context
   */
    public static void toValueSAX(Property property, Value value, int valueType, ContentHandler contentHandler, AttributesImpl na, Context context) throws SAXException, RepositoryException {
        na.clear();
        String _value = null;
        switch(valueType) {
            case PropertyType.DATE:
                DateFormat df = new SimpleDateFormat(BackupFormatConstants.DATE_FORMAT_STRING);
                df.setTimeZone(value.getDate().getTimeZone());
                _value = df.format(value.getDate().getTime());
                break;
            case PropertyType.BINARY:
                String outResourceName = property.getParent().getPath() + "/" + property.getName();
                OutputStream os = null;
                InputStream is = null;
                try {
                    os = context.getPersistenceManager().getOutResource(outResourceName, true);
                    is = value.getStream();
                    IOUtils.copy(is, os);
                    os.flush();
                } catch (Exception e) {
                    throw new SAXException("Could not backup binary value of property [" + property.getName() + "]", e);
                } finally {
                    if (null != is) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (null != os) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                na.addAttribute("", ATTACHMENT, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + ATTACHMENT, "string", outResourceName);
                break;
            case PropertyType.REFERENCE:
                _value = value.getString();
                break;
            default:
                _value = value.getString();
        }
        contentHandler.startElement("", VALUE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + VALUE, na);
        if (null != _value) contentHandler.characters(_value.toCharArray(), 0, _value.length());
        contentHandler.endElement("", VALUE, (NAMESPACE.length() > 0 ? NAMESPACE + ":" : "") + VALUE);
    }
}
