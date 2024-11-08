package com.pallas.unicore.client.xml;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.pallas.unicore.client.Client;
import com.pallas.unicore.client.util.PluginManager;
import com.pallas.unicore.client.util.UnicorePlugin;
import com.pallas.unicore.utility.UserMessages;

/**
 * @author Klaus-Dieter Oertel
 * @version $Id: XMLObjectIO.java,v 1.1 2004/05/25 14:58:54 rmenday Exp $
 */
public class XMLObjectIO {

    class DebugMessages {

        private StringBuffer indent;

        private final String INDENT_BASE;

        private final String INDENT_INCR;

        private final int INDENT_INCR_LENGTH;

        DebugMessages(String base, String incr) {
            INDENT_BASE = base;
            INDENT_INCR = incr;
            INDENT_INCR_LENGTH = INDENT_INCR.length();
            indent = new StringBuffer(INDENT_BASE);
        }

        public void popIndent() {
            indent.setLength(indent.length() - INDENT_INCR_LENGTH);
        }

        public void println(int level, String message) {
            if (DEBUG >= level) {
                if (indent.length() > 0) {
                    System.err.println(indent + " " + message);
                } else {
                    System.err.println(indent + message);
                }
            }
        }

        public void println(String message) {
            println(1, message);
        }

        public void pushIndent() {
            indent.append(INDENT_INCR);
        }
    }

    public static final Integer DEBUGXML = Integer.getInteger("DEBUGXML");

    public static final int DEBUG = (DEBUGXML == null) ? 0 : DEBUGXML.intValue();

    static final boolean FIELDTYPE;

    static {
        if (DEBUG > 2) {
            FIELDTYPE = true;
        } else {
            FIELDTYPE = false;
        }
    }

    static final String ATTR_CHECKSUM = "CHECKSUM";

    static final String ATTR_DIM = "dim";

    static final String ATTR_ELEMVERSION = "XMLV";

    static final String ATTR_FIELDTYPE = "fieldtype";

    static final String ATTR_HASHKEY = "key";

    static final String ATTR_HASHVALUE = "value";

    static final String ATTR_ID = "ID";

    static final String ATTR_IDREF = "IDREF";

    static final String ATTR_INDEX = "i";

    static final String ATTR_LENGTH = "length";

    static final String ATTR_SIZE = "size";

    static final String ATTR_VALUETYPE = "valuetype";

    static final String ATTR_VERSION = "XMLVERSION";

    protected static Logger logger = Logger.getLogger("com.pallas.unicore.client.xml");

    static final int NO_XML = Modifier.STATIC | Modifier.TRANSIENT;

    static final String NOTYETIMPL_INDICATOR = "-NOT-YET-IMPLEMENTED";

    static final String NULL_INDICATOR = "-NULL";

    static ResourceBundle res = ResourceBundle.getBundle("com.pallas.unicore.client.xml.ResourceStrings");

    static final String TAG_BINARY = "BINARY-";

    static final String TAG_UNICORE = "UNICOREpro";

    static final int TIMING = (Integer.getInteger("TIMING") == null) ? 0 : Integer.getInteger("TIMING").intValue();

    static final boolean USE_CDATA_SECTION = false;

    protected static final Method getDeclaredMethod(Class ofClass, String methodName, Class[] args) {
        Method method = null;
        try {
            method = ofClass.getDeclaredMethod(methodName, args);
            if (method != null) {
            } else {
                throw new NoSuchMethodException("METHOD " + methodName + " NOT FOUND");
            }
        } catch (NoSuchMethodException e) {
            if (DEBUG >= 2) {
                System.err.println("LOOKUP RESULT for " + ofClass.getName() + ": no method " + methodName + " found");
            }
        }
        return method;
    }

    protected static final Method getDeclaredMethod(Class ofClass, String methodName, Class[] args, int requiredModifierMask, int disallowedModifierMask) {
        Method method = null;
        try {
            method = ofClass.getDeclaredMethod(methodName, args);
            if (method != null) {
                int mods = method.getModifiers();
                if ((mods & disallowedModifierMask) != 0 || (mods & requiredModifierMask) != requiredModifierMask) {
                    if (DEBUG >= 2) {
                        UserMessages.warning("Found method " + method + " with wrong modifiers");
                    }
                    method = null;
                } else {
                }
            }
        } catch (NoSuchMethodException e) {
            if (DEBUG >= 2) {
                System.err.println("LOOKUP RESULT for " + ofClass.getName() + ": no method " + methodName + " found");
            }
        }
        return method;
    }

    public DebugMessages debug = new DebugMessages("", "#");

    protected StringBuffer indent = new StringBuffer("|");

    protected TransformerFactory tFactory;

    protected XMLObjectIO() throws TransformerConfigurationException {
        tFactory = TransformerFactory.newInstance();
        if (!tFactory.getFeature(DOMSource.FEATURE)) {
            UserMessages.error("Internal library problem: saving in XML format impossible");
            throw new TransformerConfigurationException("No DOMSource");
        }
        if (!tFactory.getFeature(DOMResult.FEATURE)) {
            UserMessages.error("Internal library problem: loading from XML format impossible");
            throw new TransformerConfigurationException("No DOMResult");
        }
        if (!tFactory.getFeature(StreamSource.FEATURE)) {
            UserMessages.error("Internal library problem: loading from XML format impossible");
            throw new TransformerConfigurationException("No StreamSource");
        }
        if (!tFactory.getFeature(StreamResult.FEATURE)) {
            UserMessages.error("Internal library problem: saving in XML format failed");
            throw new TransformerConfigurationException("No StreamResult");
        }
    }

    protected final int checkDim(Class ctype) throws IOException {
        String type = null;
        int depth = 0;
        Class atype = ctype;
        while (atype.isArray()) {
            depth++;
            atype = atype.getComponentType();
        }
        return depth;
    }

    protected final void dumpDOM(Element elem) {
        System.out.println(indent + elem.getNodeName() + " % " + elem.getLocalName());
        dumpDOM((Node) elem);
    }

    protected final void dumpDOM(Node node) {
        NodeList list = node.getChildNodes();
        indent.append(">>");
        for (int i = 0; i < list.getLength(); i++) {
            Node listi = list.item(i);
            if (listi.getNodeType() == Node.ELEMENT_NODE) {
                dumpDOM((Element) listi);
            } else if (listi.getNodeType() == Node.COMMENT_NODE) {
            } else if (listi.getNodeType() == Node.TEXT_NODE) {
                String value = listi.getNodeValue();
                if (!value.startsWith("\n") || value.trim().length() > 0) {
                    System.out.println(indent + "------------- TEXT_NODE: " + value);
                }
            } else {
                System.out.println(indent + "------------- UNKNOWN: " + listi.getNodeType());
                UserMessages.error("UNKNOWN: " + listi.getNodeType());
            }
        }
        indent.setLength(indent.length() - 2);
    }

    protected final String fromvalidXML(String in) {
        if (true) {
            return in;
        }
        String out = new String();
        int i = 0;
        while (i < in.length()) {
            char c = in.charAt(i);
            i++;
            if (c == '&') {
                if (in.startsWith("amp;", i)) {
                    out += '&';
                    i += 4;
                } else if (in.startsWith("lt;", i)) {
                    out += '<';
                    i += 3;
                } else if (in.startsWith("gt;", i)) {
                    out += '>';
                    i += 3;
                } else if (in.startsWith("apos;", i)) {
                    out += '\'';
                    i += 5;
                } else if (in.startsWith("quot;", i)) {
                    out += '"';
                    i += 5;
                }
            } else {
                out += c;
            }
        }
        return out;
    }

    protected long getCheckSum(Node node) {
        long timing = System.currentTimeMillis();
        CRC32 chksum = new CRC32();
        try {
            Transformer transformer = tFactory.newTransformer();
            DOMSource checkSource = new DOMSource(node);
            ByteArrayOutputStream checkBytes = new ByteArrayOutputStream();
            StreamResult checkResult = new StreamResult(checkBytes);
            transformer.transform(checkSource, checkResult);
            checkBytes.close();
            if (DEBUG >= 3) {
                checkBytes.writeTo(new DataOutputStream(new FileOutputStream("tttreadBytes.xml")));
            }
            chksum.update(checkBytes.toByteArray());
        } catch (TransformerConfigurationException tce) {
            logger.severe("\n** Transformer Factory error");
            logger.severe("   " + tce.getMessage());
            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            logger.log(Level.SEVERE, "", x);
        } catch (TransformerException te) {
            logger.severe("\n** Transformation error");
            logger.severe("   " + te.getMessage());
            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            logger.log(Level.SEVERE, "", x);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "", e);
        }
        timing = System.currentTimeMillis() - timing;
        if (TIMING > 2) {
            logger.info("TIMING[3]: checksum calculated in " + timing + " milliseconds");
        }
        return chksum.getValue();
    }

    protected final String getTypeName(Class ctype) throws IOException {
        String type = null;
        if (ctype.isPrimitive()) {
            type = ctype.getName();
        } else if (ctype.isArray()) {
            int depth = 0;
            Class atype = ctype;
            while (atype.isArray()) {
                depth++;
                atype = atype.getComponentType();
                type = getTypeName(atype);
            }
            if (depth > 1) {
                throw new IOException("more than 1 array dimension found [" + depth + "]");
            }
        } else {
            type = (ctype.isInstance(new String())) ? "xString:" + ctype : ctype.getName();
        }
        return type;
    }

    public final Class resolveClass(String className) throws IOException, ClassNotFoundException {
        Class self = XMLObjectIO.class;
        ClassLoader loader = self.getClassLoader();
        PluginManager pManager = Client.getPluginManager();
        if (pManager == null) {
            return Class.forName(className, false, loader);
        }
        Vector knownPlugins = pManager.getPlugins();
        if (!knownPlugins.isEmpty()) {
            for (int i = 0; i < knownPlugins.size(); i++) {
                UnicorePlugin plugin = (UnicorePlugin) knownPlugins.elementAt(i);
                loader = plugin.getPluginClassloader();
                try {
                    Class retClass = Class.forName(className, false, loader);
                    return retClass;
                } catch (ClassNotFoundException cnfe) {
                }
            }
        } else {
            return Class.forName(className, false, loader);
        }
        throw new ClassNotFoundException(className + res.getString("NOT_FOUND"));
    }

    protected final String validClassName(String xmlTag) {
        String className;
        if (xmlTag.indexOf(".") >= 0 || xmlTag.indexOf("_") >= 0) {
            debug.println(2, "Generating class name from valid XML tag " + xmlTag);
            int pos = xmlTag.lastIndexOf('.');
            StringBuffer buf = new StringBuffer(xmlTag.substring(0, pos));
            buf.append('$');
            buf.append(xmlTag.substring(pos + 1));
            className = buf.toString();
            className = className.replace('_', '[').replace(' ', ';');
            debug.println(2, "Generating class object from valid class name " + className);
        } else {
            className = new String(xmlTag);
        }
        return className;
    }

    protected final String validXML(String in) {
        if (true) {
            return in;
        }
        String out = new String("");
        for (int i = 0; i < in.length(); i++) {
            switch(in.charAt(i)) {
                case '&':
                    out += "&amp;";
                    break;
                case '<':
                    out += "&lt;";
                    break;
                case '>':
                    out += "&gt;";
                    break;
                case '\'':
                    out += "&apos;";
                    break;
                case '"':
                    out += "&quot;";
                    break;
                default:
                    out += in.charAt(i);
            }
        }
        return out;
    }

    protected final String validXMLTag(Class clazz) {
        String className = clazz.getName();
        if (className.indexOf("$") >= 0 || className.indexOf("[") >= 0) {
            debug.println(2, "Generating valid XML tag from  " + className);
            className = className.replace('$', '.').replace('[', '_').replace(';', ' ').trim();
            debug.println(2, "Generating final valid XML tag " + className);
        }
        return className;
    }

    protected final String validXMLTag(Object obj) {
        return validXMLTag(obj.getClass());
    }
}
