package org.apache.xml.serializer.dom3;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.xml.serializer.DOM3Serializer;
import org.apache.xml.serializer.Encodings;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.apache.xml.serializer.utils.MsgKey;
import org.apache.xml.serializer.utils.SystemIDResolver;
import org.apache.xml.serializer.utils.Utils;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

/**
 * Implemenatation of DOM Level 3 org.w3c.ls.LSSerializer and 
 * org.w3c.dom.ls.DOMConfiguration.  Serialization is achieved by delegating 
 * serialization calls to <CODE>org.apache.xml.serializer.ToStream</CODE> or 
 * one of its derived classes depending on the serialization method, while walking
 * the DOM in DOM3TreeWalker.  
 * @see <a href="http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407/load-save.html#LS-LSSerializer">org.w3c.dom.ls.LSSerializer</a>
 * @see <a href="http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#DOMConfiguration">org.w3c.dom.DOMConfiguration</a>
 *  
 * @version $Id:  
 * 
 * @xsl.usage internal 
 */
public final class LSSerializerImpl implements DOMConfiguration, LSSerializer {

    private static final String DEFAULT_END_OF_LINE;

    static {
        String lineSeparator = (String) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    return System.getProperty("line.separator");
                } catch (SecurityException ex) {
                }
                return null;
            }
        });
        DEFAULT_END_OF_LINE = lineSeparator != null && (lineSeparator.equals("\r\n") || lineSeparator.equals("\r")) ? lineSeparator : "\n";
    }

    /** private data members */
    private Serializer fXMLSerializer = null;

    protected int fFeatures = 0;

    private DOM3Serializer fDOMSerializer = null;

    private LSSerializerFilter fSerializerFilter = null;

    private Node fVisitedNode = null;

    private String fEndOfLine = DEFAULT_END_OF_LINE;

    private DOMErrorHandler fDOMErrorHandler = null;

    private Properties fDOMConfigProperties = null;

    private String fEncoding;

    private static final int CANONICAL = 0x1 << 0;

    private static final int CDATA = 0x1 << 1;

    private static final int CHARNORMALIZE = 0x1 << 2;

    private static final int COMMENTS = 0x1 << 3;

    private static final int DTNORMALIZE = 0x1 << 4;

    private static final int ELEM_CONTENT_WHITESPACE = 0x1 << 5;

    private static final int ENTITIES = 0x1 << 6;

    private static final int INFOSET = 0x1 << 7;

    private static final int NAMESPACES = 0x1 << 8;

    private static final int NAMESPACEDECLS = 0x1 << 9;

    private static final int NORMALIZECHARS = 0x1 << 10;

    private static final int SPLITCDATA = 0x1 << 11;

    private static final int VALIDATE = 0x1 << 12;

    private static final int SCHEMAVALIDATE = 0x1 << 13;

    private static final int WELLFORMED = 0x1 << 14;

    private static final int DISCARDDEFAULT = 0x1 << 15;

    private static final int PRETTY_PRINT = 0x1 << 16;

    private static final int IGNORE_CHAR_DENORMALIZE = 0x1 << 17;

    private static final int XMLDECL = 0x1 << 18;

    private String fRecognizedParameters[] = { DOMConstants.DOM_CANONICAL_FORM, DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM_CHECK_CHAR_NORMALIZATION, DOMConstants.DOM_COMMENTS, DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM_ENTITIES, DOMConstants.DOM_INFOSET, DOMConstants.DOM_NAMESPACES, DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM_VALIDATE, DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM_WELLFORMED, DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM_FORMAT_PRETTY_PRINT, DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS, DOMConstants.DOM_XMLDECL, DOMConstants.DOM_ERROR_HANDLER };

    /**
     * Constructor:  Creates a LSSerializerImpl object.  The underlying
     * XML 1.0 or XML 1.1 org.apache.xml.serializer.Serializer object is
     * created and initialized the first time any of the write methods are  
     * invoked to serialize the Node.  Subsequent write methods on the same
     * LSSerializerImpl object will use the previously created Serializer object.
     */
    public LSSerializerImpl() {
        fFeatures |= CDATA;
        fFeatures |= COMMENTS;
        fFeatures |= ELEM_CONTENT_WHITESPACE;
        fFeatures |= ENTITIES;
        fFeatures |= NAMESPACES;
        fFeatures |= NAMESPACEDECLS;
        fFeatures |= SPLITCDATA;
        fFeatures |= WELLFORMED;
        fFeatures |= DISCARDDEFAULT;
        fFeatures |= XMLDECL;
        fDOMConfigProperties = new Properties();
        initializeSerializerProps();
        Properties configProps = OutputPropertiesFactory.getDefaultMethodProperties("xml");
        fXMLSerializer = SerializerFactory.getSerializer(configProps);
        fXMLSerializer.setOutputFormat(fDOMConfigProperties);
    }

    /**
     * Initializes the underlying serializer's configuration depending on the
     * default DOMConfiguration parameters. This method must be called before a
     * node is to be serialized.
     * 
     * @xsl.usage internal
     */
    public void initializeSerializerProps() {
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CANONICAL_FORM, DOMConstants.DOM3_DEFAULT_FALSE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CHECK_CHAR_NORMALIZATION, DOMConstants.DOM3_DEFAULT_FALSE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM3_DEFAULT_FALSE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_DEFAULT_TRUE);
        if ((fFeatures & INFOSET) != 0) {
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES, DOMConstants.DOM3_DEFAULT_TRUE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM3_DEFAULT_TRUE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS, DOMConstants.DOM3_DEFAULT_TRUE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM3_DEFAULT_TRUE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED, DOMConstants.DOM3_DEFAULT_TRUE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_DEFAULT_FALSE);
            fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_DEFAULT_FALSE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM3_DEFAULT_FALSE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM3_DEFAULT_FALSE);
            fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM3_DEFAULT_FALSE);
        }
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE, DOMConstants.DOM3_DEFAULT_FALSE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM3_DEFAULT_FALSE);
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_INDENT, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, Integer.toString(3));
        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM3_DEFAULT_TRUE);
        fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, "no");
    }

    /** 
     * Checks if setting a parameter to a specific value is supported.    
     *  
     * @see org.w3c.dom.DOMConfiguration#canSetParameter(java.lang.String, java.lang.Object)
     * @since DOM Level 3
     * @param name A String containing the DOMConfiguration parameter name.
     * @param value An Object specifying the value of the corresponding parameter. 
     */
    public boolean canSetParameter(String name, Object value) {
        if (value instanceof Boolean) {
            if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || name.equalsIgnoreCase(DOMConstants.DOM_INFOSET) || name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
                return true;
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
                return !((Boolean) value).booleanValue();
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                return ((Boolean) value).booleanValue();
            }
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) && value == null || value instanceof DOMErrorHandler) {
            return true;
        }
        return false;
    }

    /**
     * This method returns the value of a parameter if known.
     * 
     * @see org.w3c.dom.DOMConfiguration#getParameter(java.lang.String)
     * 
     * @param name A String containing the DOMConfiguration parameter name 
     *             whose value is to be returned.
     * @return Object The value of the parameter if known. 
     */
    public Object getParameter(String name) throws DOMException {
        if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
            return ((fFeatures & COMMENTS) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
            return ((fFeatures & CDATA) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
            return ((fFeatures & ENTITIES) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
            return ((fFeatures & NAMESPACES) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
            return ((fFeatures & NAMESPACEDECLS) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
            return ((fFeatures & SPLITCDATA) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
            return ((fFeatures & WELLFORMED) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
            return ((fFeatures & DISCARDDEFAULT) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return ((fFeatures & PRETTY_PRINT) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
            return ((fFeatures & XMLDECL) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
            return ((fFeatures & ELEM_CONTENT_WHITESPACE) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
            return ((fFeatures & PRETTY_PRINT) != 0) ? Boolean.TRUE : Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
            return Boolean.TRUE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
            return Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
            if ((fFeatures & ENTITIES) == 0 && (fFeatures & CDATA) == 0 && (fFeatures & ELEM_CONTENT_WHITESPACE) != 0 && (fFeatures & NAMESPACES) != 0 && (fFeatures & NAMESPACEDECLS) != 0 && (fFeatures & WELLFORMED) != 0 && (fFeatures & COMMENTS) != 0) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
            return fDOMErrorHandler;
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
            return null;
        } else {
            String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_FOUND, new Object[] { name });
            throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
        }
    }

    /**
     * This method returns a of the parameters supported by this DOMConfiguration object 
     * and for which at least one value can be set by the application
     * 
     * @see org.w3c.dom.DOMConfiguration#getParameterNames()
     * 
     * @return DOMStringList A list of DOMConfiguration parameters recognized
     *                       by the serializer
     */
    public DOMStringList getParameterNames() {
        return new DOMStringListImpl(fRecognizedParameters);
    }

    /**
     * This method sets the value of the named parameter.
     *   
     * @see org.w3c.dom.DOMConfiguration#setParameter(java.lang.String, java.lang.Object)
     * 
     * @param name A String containing the DOMConfiguration parameter name.
     * @param value An Object contaiing the parameters value to set.
     */
    public void setParameter(String name, Object value) throws DOMException {
        if (value instanceof Boolean) {
            boolean state = ((Boolean) value).booleanValue();
            if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS)) {
                fFeatures = state ? fFeatures | COMMENTS : fFeatures & ~COMMENTS;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS)) {
                fFeatures = state ? fFeatures | CDATA : fFeatures & ~CDATA;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES)) {
                fFeatures = state ? fFeatures | ENTITIES : fFeatures & ~ENTITIES;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_FALSE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES)) {
                fFeatures = state ? fFeatures | NAMESPACES : fFeatures & ~NAMESPACES;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS)) {
                fFeatures = state ? fFeatures | NAMESPACEDECLS : fFeatures & ~NAMESPACEDECLS;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA)) {
                fFeatures = state ? fFeatures | SPLITCDATA : fFeatures & ~SPLITCDATA;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_SPLIT_CDATA, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED)) {
                fFeatures = state ? fFeatures | WELLFORMED : fFeatures & ~WELLFORMED;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT)) {
                fFeatures = state ? fFeatures | DISCARDDEFAULT : fFeatures & ~DISCARDDEFAULT;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DISCARD_DEFAULT_CONTENT, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT)) {
                fFeatures = state ? fFeatures | PRETTY_PRINT : fFeatures & ~PRETTY_PRINT;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_FORMAT_PRETTY_PRINT, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_FORMAT_PRETTY_PRINT, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL)) {
                fFeatures = state ? fFeatures | XMLDECL : fFeatures & ~XMLDECL;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, "no");
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, "yes");
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                fFeatures = state ? fFeatures | ELEM_CONTENT_WHITESPACE : fFeatures & ~ELEM_CONTENT_WHITESPACE;
                if (state) {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM3_EXPLICIT_TRUE);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                if (!state) {
                    String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_SUPPORTED, new Object[] { name });
                    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
                } else {
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS, DOMConstants.DOM3_EXPLICIT_TRUE);
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                if (state) {
                    String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_SUPPORTED, new Object[] { name });
                    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
                } else {
                    if (name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM)) {
                        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CANONICAL_FORM, DOMConstants.DOM3_EXPLICIT_FALSE);
                    } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM3_EXPLICIT_FALSE);
                    } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE)) {
                        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE, DOMConstants.DOM3_EXPLICIT_FALSE);
                    } else if (name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA)) {
                        fDOMConfigProperties.setProperty(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION + DOMConstants.DOM_CHECK_CHAR_NORMALIZATION, DOMConstants.DOM3_EXPLICIT_FALSE);
                    } else if (name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION)) {
                        fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM3_EXPLICIT_FALSE);
                    }
                }
            } else if (name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
                if (state) {
                    fFeatures &= ~ENTITIES;
                    fFeatures &= ~CDATA;
                    fFeatures &= ~SCHEMAVALIDATE;
                    fFeatures &= ~DTNORMALIZE;
                    fFeatures |= NAMESPACES;
                    fFeatures |= NAMESPACEDECLS;
                    fFeatures |= WELLFORMED;
                    fFeatures |= ELEM_CONTENT_WHITESPACE;
                    fFeatures |= COMMENTS;
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACES, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_NAMESPACE_DECLARATIONS, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_COMMENTS, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_WELLFORMED, DOMConstants.DOM3_EXPLICIT_TRUE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_FALSE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.DOM_ENTITIES, DOMConstants.DOM3_EXPLICIT_FALSE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_CDATA_SECTIONS, DOMConstants.DOM3_EXPLICIT_FALSE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_VALIDATE_IF_SCHEMA, DOMConstants.DOM3_EXPLICIT_FALSE);
                    fDOMConfigProperties.setProperty(DOMConstants.S_DOM3_PROPERTIES_NS + DOMConstants.DOM_DATATYPE_NORMALIZATION, DOMConstants.DOM3_EXPLICIT_FALSE);
                }
            } else {
                if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
                    String msg = Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[] { name });
                    throw new DOMException(DOMException.TYPE_MISMATCH_ERR, msg);
                }
                String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_FOUND, new Object[] { name });
                throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
            }
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_ERROR_HANDLER)) {
            if (value == null || value instanceof DOMErrorHandler) {
                fDOMErrorHandler = (DOMErrorHandler) value;
            } else {
                String msg = Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[] { name });
                throw new DOMException(DOMException.TYPE_MISMATCH_ERR, msg);
            }
        } else if (name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_LOCATION) || name.equalsIgnoreCase(DOMConstants.DOM_SCHEMA_TYPE)) {
            if (value != null) {
                if (!(value instanceof String)) {
                    String msg = Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[] { name });
                    throw new DOMException(DOMException.TYPE_MISMATCH_ERR, msg);
                }
                String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_SUPPORTED, new Object[] { name });
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
        } else {
            if (name.equalsIgnoreCase(DOMConstants.DOM_COMMENTS) || name.equalsIgnoreCase(DOMConstants.DOM_CDATA_SECTIONS) || name.equalsIgnoreCase(DOMConstants.DOM_ENTITIES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACES) || name.equalsIgnoreCase(DOMConstants.DOM_NAMESPACE_DECLARATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_SPLIT_CDATA) || name.equalsIgnoreCase(DOMConstants.DOM_WELLFORMED) || name.equalsIgnoreCase(DOMConstants.DOM_DISCARD_DEFAULT_CONTENT) || name.equalsIgnoreCase(DOMConstants.DOM_FORMAT_PRETTY_PRINT) || name.equalsIgnoreCase(DOMConstants.DOM_XMLDECL) || name.equalsIgnoreCase(DOMConstants.DOM_ELEMENT_CONTENT_WHITESPACE) || name.equalsIgnoreCase(DOMConstants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS) || name.equalsIgnoreCase(DOMConstants.DOM_CANONICAL_FORM) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE_IF_SCHEMA) || name.equalsIgnoreCase(DOMConstants.DOM_VALIDATE) || name.equalsIgnoreCase(DOMConstants.DOM_CHECK_CHAR_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_DATATYPE_NORMALIZATION) || name.equalsIgnoreCase(DOMConstants.DOM_INFOSET)) {
                String msg = Utils.messages.createMessage(MsgKey.ER_TYPE_MISMATCH_ERR, new Object[] { name });
                throw new DOMException(DOMException.TYPE_MISMATCH_ERR, msg);
            }
            String msg = Utils.messages.createMessage(MsgKey.ER_FEATURE_NOT_FOUND, new Object[] { name });
            throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
        }
    }

    /** 
     * Returns the DOMConfiguration of the LSSerializer.
     *  
     * @see org.w3c.dom.ls.LSSerializer#getDomConfig()
     * @since DOM Level 3
     * @return A DOMConfiguration object.
     */
    public DOMConfiguration getDomConfig() {
        return (DOMConfiguration) this;
    }

    /** 
     * Returns the DOMConfiguration of the LSSerializer.
     *  
     * @see org.w3c.dom.ls.LSSerializer#getFilter()
     * @since DOM Level 3
     * @return A LSSerializerFilter object.
     */
    public LSSerializerFilter getFilter() {
        return fSerializerFilter;
    }

    /** 
     * Returns the End-Of-Line sequence of characters to be used in the XML 
     * being serialized.  If none is set a default "\n" is returned.
     * 
     * @see org.w3c.dom.ls.LSSerializer#getNewLine()
     * @since DOM Level 3
     * @return A String containing the end-of-line character sequence  used in 
     * serialization.
     */
    public String getNewLine() {
        return fEndOfLine;
    }

    /** 
     * Set a LSSerilizerFilter on the LSSerializer.  When set, the filter is
     * called before each node is serialized which depending on its implemention
     * determines if the node is to be serialized or not.    
     *  
     * @see org.w3c.dom.ls.LSSerializer#setFilter
     * @since DOM Level 3
     * @param filter A LSSerializerFilter to be applied to the stream to serialize.
     */
    public void setFilter(LSSerializerFilter filter) {
        fSerializerFilter = filter;
    }

    /** 
     * Sets the End-Of-Line sequence of characters to be used in the XML 
     * being serialized.  Setting this attribute to null will reset its 
     * value to the default value i.e. "\n".
     * 
     * @see org.w3c.dom.ls.LSSerializer#setNewLine
     * @since DOM Level 3
     * @param newLine a String that is the end-of-line character sequence to be used in 
     * serialization.
     */
    public void setNewLine(String newLine) {
        fEndOfLine = (newLine != null) ? newLine : DEFAULT_END_OF_LINE;
    }

    /** 
     * Serializes the specified node to the specified LSOutput and returns true if the Node 
     * was successfully serialized. 
     * 
     * @see org.w3c.dom.ls.LSSerializer#write(org.w3c.dom.Node, org.w3c.dom.ls.LSOutput)
     * @since DOM Level 3
     * @param nodeArg The Node to serialize.
     * @throws org.w3c.dom.ls.LSException SERIALIZE_ERR: Raised if the 
     * LSSerializer was unable to serialize the node.
     *      
     */
    public boolean write(Node nodeArg, LSOutput destination) throws LSException {
        if (destination == null) {
            String msg = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
            if (fDOMErrorHandler != null) {
                fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, msg, MsgKey.ER_NO_OUTPUT_SPECIFIED));
            }
            throw new LSException(LSException.SERIALIZE_ERR, msg);
        }
        if (nodeArg == null) {
            return false;
        }
        Serializer serializer = fXMLSerializer;
        serializer.reset();
        if (nodeArg != fVisitedNode) {
            String xmlVersion = getXMLVersion(nodeArg);
            fEncoding = destination.getEncoding();
            if (fEncoding == null) {
                fEncoding = getInputEncoding(nodeArg);
                fEncoding = fEncoding != null ? fEncoding : getXMLEncoding(nodeArg) == null ? "UTF-8" : getXMLEncoding(nodeArg);
            }
            if (!Encodings.isRecognizedEncoding(fEncoding)) {
                String msg = Utils.messages.createMessage(MsgKey.ER_UNSUPPORTED_ENCODING, null);
                if (fDOMErrorHandler != null) {
                    fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, msg, MsgKey.ER_UNSUPPORTED_ENCODING));
                }
                throw new LSException(LSException.SERIALIZE_ERR, msg);
            }
            serializer.getOutputFormat().setProperty("version", xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.S_XML_VERSION, xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_ENCODING, fEncoding);
            if ((nodeArg.getNodeType() != Node.DOCUMENT_NODE || nodeArg.getNodeType() != Node.ELEMENT_NODE || nodeArg.getNodeType() != Node.ENTITY_NODE) && ((fFeatures & XMLDECL) != 0)) {
                fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, DOMConstants.DOM3_DEFAULT_FALSE);
            }
            fVisitedNode = nodeArg;
        }
        fXMLSerializer.setOutputFormat(fDOMConfigProperties);
        try {
            Writer writer = destination.getCharacterStream();
            if (writer == null) {
                OutputStream outputStream = destination.getByteStream();
                if (outputStream == null) {
                    String uri = destination.getSystemId();
                    if (uri == null) {
                        String msg = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
                        if (fDOMErrorHandler != null) {
                            fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, msg, MsgKey.ER_NO_OUTPUT_SPECIFIED));
                        }
                        throw new LSException(LSException.SERIALIZE_ERR, msg);
                    } else {
                        String absoluteURI = SystemIDResolver.getAbsoluteURI(uri);
                        URL url = new URL(absoluteURI);
                        OutputStream urlOutStream = null;
                        String protocol = url.getProtocol();
                        String host = url.getHost();
                        if (protocol.equalsIgnoreCase("file") && (host == null || host.length() == 0 || host.equals("localhost"))) {
                            urlOutStream = new FileOutputStream(getPathWithoutEscapes(url.getPath()));
                        } else {
                            URLConnection urlCon = url.openConnection();
                            urlCon.setDoInput(false);
                            urlCon.setDoOutput(true);
                            urlCon.setUseCaches(false);
                            urlCon.setAllowUserInteraction(false);
                            if (urlCon instanceof HttpURLConnection) {
                                HttpURLConnection httpCon = (HttpURLConnection) urlCon;
                                httpCon.setRequestMethod("PUT");
                            }
                            urlOutStream = urlCon.getOutputStream();
                        }
                        serializer.setOutputStream(urlOutStream);
                    }
                } else {
                    serializer.setOutputStream(outputStream);
                }
            } else {
                serializer.setWriter(writer);
            }
            if (fDOMSerializer == null) {
                fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (fDOMErrorHandler != null) {
                fDOMSerializer.setErrorHandler(fDOMErrorHandler);
            }
            if (fSerializerFilter != null) {
                fDOMSerializer.setNodeFilter(fSerializerFilter);
            }
            fDOMSerializer.setNewLine(fEndOfLine.toCharArray());
            fDOMSerializer.serializeDOM3(nodeArg);
        } catch (UnsupportedEncodingException ue) {
            String msg = Utils.messages.createMessage(MsgKey.ER_UNSUPPORTED_ENCODING, null);
            if (fDOMErrorHandler != null) {
                fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, msg, MsgKey.ER_UNSUPPORTED_ENCODING, ue));
            }
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, ue).fillInStackTrace();
        } catch (LSException lse) {
            throw lse;
        } catch (RuntimeException e) {
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        } catch (Exception e) {
            if (fDOMErrorHandler != null) {
                fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, e.getMessage(), null, e));
            }
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        }
        return true;
    }

    /** 
     * Serializes the specified node and returns a String with the serialized
     * data to the caller.  
     * 
     * @see org.w3c.dom.ls.LSSerializer#writeToString(org.w3c.dom.Node)
     * @since DOM Level 3
     * @param nodeArg The Node to serialize.
     * @throws org.w3c.dom.ls.LSException SERIALIZE_ERR: Raised if the 
     * LSSerializer was unable to serialize the node.
     *      
     */
    public String writeToString(Node nodeArg) throws DOMException, LSException {
        if (nodeArg == null) {
            return null;
        }
        Serializer serializer = fXMLSerializer;
        serializer.reset();
        if (nodeArg != fVisitedNode) {
            String xmlVersion = getXMLVersion(nodeArg);
            serializer.getOutputFormat().setProperty("version", xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.S_XML_VERSION, xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_ENCODING, "UTF-16");
            if ((nodeArg.getNodeType() != Node.DOCUMENT_NODE || nodeArg.getNodeType() != Node.ELEMENT_NODE || nodeArg.getNodeType() != Node.ENTITY_NODE) && ((fFeatures & XMLDECL) != 0)) {
                fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, DOMConstants.DOM3_DEFAULT_FALSE);
            }
            fVisitedNode = nodeArg;
        }
        fXMLSerializer.setOutputFormat(fDOMConfigProperties);
        StringWriter output = new StringWriter();
        try {
            serializer.setWriter(output);
            if (fDOMSerializer == null) {
                fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (fDOMErrorHandler != null) {
                fDOMSerializer.setErrorHandler(fDOMErrorHandler);
            }
            if (fSerializerFilter != null) {
                fDOMSerializer.setNodeFilter(fSerializerFilter);
            }
            fDOMSerializer.setNewLine(fEndOfLine.toCharArray());
            fDOMSerializer.serializeDOM3(nodeArg);
        } catch (LSException lse) {
            throw lse;
        } catch (RuntimeException e) {
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        } catch (Exception e) {
            if (fDOMErrorHandler != null) {
                fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, e.getMessage(), null, e));
            }
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        }
        return output.toString();
    }

    /** 
     * Serializes the specified node to the specified URI and returns true if the Node 
     * was successfully serialized. 
     * 
     * @see org.w3c.dom.ls.LSSerializer#writeToURI(org.w3c.dom.Node, String)
     * @since DOM Level 3
     * @param nodeArg The Node to serialize.
     * @throws org.w3c.dom.ls.LSException SERIALIZE_ERR: Raised if the 
     * LSSerializer was unable to serialize the node.
     *      
     */
    public boolean writeToURI(Node nodeArg, String uri) throws LSException {
        if (nodeArg == null) {
            return false;
        }
        Serializer serializer = fXMLSerializer;
        serializer.reset();
        if (nodeArg != fVisitedNode) {
            String xmlVersion = getXMLVersion(nodeArg);
            fEncoding = getInputEncoding(nodeArg);
            if (fEncoding == null) {
                fEncoding = fEncoding != null ? fEncoding : getXMLEncoding(nodeArg) == null ? "UTF-8" : getXMLEncoding(nodeArg);
            }
            serializer.getOutputFormat().setProperty("version", xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XERCES_PROPERTIES_NS + DOMConstants.S_XML_VERSION, xmlVersion);
            fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_ENCODING, fEncoding);
            if ((nodeArg.getNodeType() != Node.DOCUMENT_NODE || nodeArg.getNodeType() != Node.ELEMENT_NODE || nodeArg.getNodeType() != Node.ENTITY_NODE) && ((fFeatures & XMLDECL) != 0)) {
                fDOMConfigProperties.setProperty(DOMConstants.S_XSL_OUTPUT_OMIT_XML_DECL, DOMConstants.DOM3_DEFAULT_FALSE);
            }
            fVisitedNode = nodeArg;
        }
        fXMLSerializer.setOutputFormat(fDOMConfigProperties);
        try {
            if (uri == null) {
                String msg = Utils.messages.createMessage(MsgKey.ER_NO_OUTPUT_SPECIFIED, null);
                if (fDOMErrorHandler != null) {
                    fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, msg, MsgKey.ER_NO_OUTPUT_SPECIFIED));
                }
                throw new LSException(LSException.SERIALIZE_ERR, msg);
            } else {
                String absoluteURI = SystemIDResolver.getAbsoluteURI(uri);
                URL url = new URL(absoluteURI);
                OutputStream urlOutStream = null;
                String protocol = url.getProtocol();
                String host = url.getHost();
                if (protocol.equalsIgnoreCase("file") && (host == null || host.length() == 0 || host.equals("localhost"))) {
                    urlOutStream = new FileOutputStream(getPathWithoutEscapes(url.getPath()));
                } else {
                    URLConnection urlCon = url.openConnection();
                    urlCon.setDoInput(false);
                    urlCon.setDoOutput(true);
                    urlCon.setUseCaches(false);
                    urlCon.setAllowUserInteraction(false);
                    if (urlCon instanceof HttpURLConnection) {
                        HttpURLConnection httpCon = (HttpURLConnection) urlCon;
                        httpCon.setRequestMethod("PUT");
                    }
                    urlOutStream = urlCon.getOutputStream();
                }
                serializer.setOutputStream(urlOutStream);
            }
            if (fDOMSerializer == null) {
                fDOMSerializer = (DOM3Serializer) serializer.asDOM3Serializer();
            }
            if (fDOMErrorHandler != null) {
                fDOMSerializer.setErrorHandler(fDOMErrorHandler);
            }
            if (fSerializerFilter != null) {
                fDOMSerializer.setNodeFilter(fSerializerFilter);
            }
            fDOMSerializer.setNewLine(fEndOfLine.toCharArray());
            fDOMSerializer.serializeDOM3(nodeArg);
        } catch (LSException lse) {
            throw lse;
        } catch (RuntimeException e) {
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        } catch (Exception e) {
            if (fDOMErrorHandler != null) {
                fDOMErrorHandler.handleError(new DOMErrorImpl(DOMError.SEVERITY_FATAL_ERROR, e.getMessage(), null, e));
            }
            throw (LSException) createLSException(LSException.SERIALIZE_ERR, e).fillInStackTrace();
        }
        return true;
    }

    protected String getXMLVersion(Node nodeArg) {
        Document doc = null;
        if (nodeArg != null) {
            if (nodeArg.getNodeType() == Node.DOCUMENT_NODE) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getXmlVersion();
            }
        }
        return "1.0";
    }

    /** 
     * Determines the XML Encoding of the Document Node to serialize.  If the Document Node
     * is not a DOM Level 3 Node, then the default encoding "UTF-8" is returned.
     * 
     * @param  nodeArg The Node to serialize
     * @return A String containing the encoding pseudo-attribute of the XMLDecl.  
     * @throws Throwable if the DOM implementation does not implement Document.getXmlEncoding()     
     */
    protected String getXMLEncoding(Node nodeArg) {
        Document doc = null;
        if (nodeArg != null) {
            if (nodeArg.getNodeType() == Node.DOCUMENT_NODE) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getXmlEncoding();
            }
        }
        return "UTF-8";
    }

    /** 
     * Determines the Input Encoding of the Document Node to serialize.  If the Document Node
     * is not a DOM Level 3 Node, then null is returned.
     * 
     * @param  nodeArg The Node to serialize
     * @return A String containing the input encoding.  
     */
    protected String getInputEncoding(Node nodeArg) {
        Document doc = null;
        if (nodeArg != null) {
            if (nodeArg.getNodeType() == Node.DOCUMENT_NODE) {
                doc = (Document) nodeArg;
            } else {
                doc = nodeArg.getOwnerDocument();
            }
            if (doc != null && doc.getImplementation().hasFeature("Core", "3.0")) {
                return doc.getInputEncoding();
            }
        }
        return null;
    }

    /**
     * This method returns the LSSerializer's error handler.
     * 
     * @return Returns the fDOMErrorHandler.
     */
    public DOMErrorHandler getErrorHandler() {
        return fDOMErrorHandler;
    }

    /**
     * Replaces all escape sequences in the given path with their literal characters.
     */
    private static String getPathWithoutEscapes(String origPath) {
        if (origPath != null && origPath.length() != 0 && origPath.indexOf('%') != -1) {
            StringTokenizer tokenizer = new StringTokenizer(origPath, "%");
            StringBuffer result = new StringBuffer(origPath.length());
            int size = tokenizer.countTokens();
            result.append(tokenizer.nextToken());
            for (int i = 1; i < size; ++i) {
                String token = tokenizer.nextToken();
                if (token.length() >= 2 && isHexDigit(token.charAt(0)) && isHexDigit(token.charAt(1))) {
                    result.append((char) Integer.valueOf(token.substring(0, 2), 16).intValue());
                    token = token.substring(2);
                }
                result.append(token);
            }
            return result.toString();
        }
        return origPath;
    }

    /** 
     * Returns true if the given character is a valid hex character.
     */
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F');
    }

    /**
     * Creates an LSException. On J2SE 1.4 and above the cause for the exception will be set.
     */
    private static LSException createLSException(short code, Throwable cause) {
        LSException lse = new LSException(code, cause != null ? cause.getMessage() : null);
        if (cause != null && ThrowableMethods.fgThrowableMethodsAvailable) {
            try {
                ThrowableMethods.fgThrowableInitCauseMethod.invoke(lse, new Object[] { cause });
            } catch (Exception e) {
            }
        }
        return lse;
    }

    /**
     * Holder of methods from java.lang.Throwable.
     */
    static class ThrowableMethods {

        private static java.lang.reflect.Method fgThrowableInitCauseMethod = null;

        private static boolean fgThrowableMethodsAvailable = false;

        private ThrowableMethods() {
        }

        static {
            try {
                fgThrowableInitCauseMethod = Throwable.class.getMethod("initCause", new Class[] { Throwable.class });
                fgThrowableMethodsAvailable = true;
            } catch (Exception exc) {
                fgThrowableInitCauseMethod = null;
                fgThrowableMethodsAvailable = false;
            }
        }
    }
}
