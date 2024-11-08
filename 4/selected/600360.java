package it.jplag.jplagClient;

import com.sun.xml.rpc.encoding.*;
import com.sun.xml.rpc.encoding.xsd.XSDConstants;
import com.sun.xml.rpc.encoding.literal.*;
import com.sun.xml.rpc.encoding.literal.DetailFragmentDeserializer;
import com.sun.xml.rpc.encoding.simpletype.*;
import com.sun.xml.rpc.encoding.soap.SOAPConstants;
import com.sun.xml.rpc.encoding.soap.SOAP12Constants;
import com.sun.xml.rpc.streaming.*;
import com.sun.xml.rpc.wsdl.document.schema.SchemaConstants;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.ArrayList;

public class Option_LiteralSerializer extends LiteralObjectSerializerBase implements Initializable {

    private static final javax.xml.namespace.QName ns1_language_QNAME = new QName("", "language");

    private static final javax.xml.namespace.QName ns2_string_TYPE_QNAME = SchemaConstants.QNAME_TYPE_STRING;

    private CombinedSerializer ns2_myns2_string__java_lang_String_String_Serializer;

    private static final javax.xml.namespace.QName ns1_minimumMatchLength_QNAME = new QName("", "minimumMatchLength");

    private static final javax.xml.namespace.QName ns2_int_TYPE_QNAME = SchemaConstants.QNAME_TYPE_INT;

    private CombinedSerializer ns2_myns2__int__int_Int_Serializer;

    private static final javax.xml.namespace.QName ns1_suffixes_QNAME = new QName("", "suffixes");

    private static final javax.xml.namespace.QName ns1_readSubdirs_QNAME = new QName("", "readSubdirs");

    private static final javax.xml.namespace.QName ns2_boolean_TYPE_QNAME = SchemaConstants.QNAME_TYPE_BOOLEAN;

    private CombinedSerializer ns2_myns2__boolean__boolean_Boolean_Serializer;

    private static final javax.xml.namespace.QName ns1_pathToFiles_QNAME = new QName("", "pathToFiles");

    private static final javax.xml.namespace.QName ns1_basecodeDir_QNAME = new QName("", "basecodeDir");

    private static final javax.xml.namespace.QName ns1_storeMatches_QNAME = new QName("", "storeMatches");

    private static final javax.xml.namespace.QName ns1_clustertype_QNAME = new QName("", "clustertype");

    private static final javax.xml.namespace.QName ns1_countryLang_QNAME = new QName("", "countryLang");

    private static final javax.xml.namespace.QName ns1_title_QNAME = new QName("", "title");

    private static final javax.xml.namespace.QName ns1_originalDir_QNAME = new QName("", "originalDir");

    public Option_LiteralSerializer(javax.xml.namespace.QName type, java.lang.String encodingStyle) {
        this(type, encodingStyle, false);
    }

    public Option_LiteralSerializer(javax.xml.namespace.QName type, java.lang.String encodingStyle, boolean encodeType) {
        super(type, true, encodingStyle, encodeType);
    }

    public void initialize(InternalTypeMappingRegistry registry) throws Exception {
        ns2_myns2_string__java_lang_String_String_Serializer = (CombinedSerializer) registry.getSerializer("", java.lang.String.class, ns2_string_TYPE_QNAME);
        ns2_myns2__int__int_Int_Serializer = (CombinedSerializer) registry.getSerializer("", int.class, ns2_int_TYPE_QNAME);
        ns2_myns2__boolean__boolean_Boolean_Serializer = (CombinedSerializer) registry.getSerializer("", boolean.class, ns2_boolean_TYPE_QNAME);
    }

    public java.lang.Object doDeserialize(XMLReader reader, SOAPDeserializationContext context) throws java.lang.Exception {
        it.jplag.jplagClient.Option instance = new it.jplag.jplagClient.Option();
        java.lang.Object member = null;
        javax.xml.namespace.QName elementName;
        java.util.List values;
        java.lang.Object value;
        reader.nextElementContent();
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_language_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_language_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setLanguage((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_language_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_minimumMatchLength_QNAME)) {
                member = ns2_myns2__int__int_Int_Serializer.deserialize(ns1_minimumMatchLength_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setMinimumMatchLength(((java.lang.Integer) member).intValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_minimumMatchLength_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if ((reader.getState() == XMLReader.START) && (elementName.equals(ns1_suffixes_QNAME))) {
            values = new ArrayList();
            for (; ; ) {
                elementName = reader.getName();
                if ((reader.getState() == XMLReader.START) && (elementName.equals(ns1_suffixes_QNAME))) {
                    value = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_suffixes_QNAME, reader, context);
                    if (value == null) {
                        throw new DeserializationException("literal.unexpectedNull");
                    }
                    values.add(value);
                    reader.nextElementContent();
                } else {
                    break;
                }
            }
            member = new java.lang.String[values.size()];
            member = values.toArray((Object[]) member);
            instance.setSuffixes((java.lang.String[]) member);
        } else {
            instance.setSuffixes(new java.lang.String[0]);
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_readSubdirs_QNAME)) {
                member = ns2_myns2__boolean__boolean_Boolean_Serializer.deserialize(ns1_readSubdirs_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setReadSubdirs(((Boolean) member).booleanValue());
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_readSubdirs_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_pathToFiles_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_pathToFiles_QNAME, reader, context);
                instance.setPathToFiles((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_pathToFiles_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_basecodeDir_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_basecodeDir_QNAME, reader, context);
                instance.setBasecodeDir((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_basecodeDir_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_storeMatches_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_storeMatches_QNAME, reader, context);
                instance.setStoreMatches((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_storeMatches_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_clustertype_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_clustertype_QNAME, reader, context);
                instance.setClustertype((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_clustertype_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_countryLang_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_countryLang_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setCountryLang((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_countryLang_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_title_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_title_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setTitle((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_title_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        elementName = reader.getName();
        if (reader.getState() == XMLReader.START) {
            if (elementName.equals(ns1_originalDir_QNAME)) {
                member = ns2_myns2_string__java_lang_String_String_Serializer.deserialize(ns1_originalDir_QNAME, reader, context);
                if (member == null) {
                    throw new DeserializationException("literal.unexpectedNull");
                }
                instance.setOriginalDir((java.lang.String) member);
                reader.nextElementContent();
            } else {
                throw new DeserializationException("literal.unexpectedElementName", new Object[] { ns1_originalDir_QNAME, reader.getName() });
            }
        } else {
            throw new DeserializationException("literal.expectedElementName", reader.getName().toString());
        }
        XMLReaderUtil.verifyReaderState(reader, XMLReader.END);
        return (java.lang.Object) instance;
    }

    public void doSerializeAttributes(java.lang.Object obj, XMLWriter writer, SOAPSerializationContext context) throws java.lang.Exception {
        it.jplag.jplagClient.Option instance = (it.jplag.jplagClient.Option) obj;
    }

    public void doSerialize(java.lang.Object obj, XMLWriter writer, SOAPSerializationContext context) throws java.lang.Exception {
        it.jplag.jplagClient.Option instance = (it.jplag.jplagClient.Option) obj;
        if (instance.getLanguage() == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getLanguage(), ns1_language_QNAME, null, writer, context);
        if (new java.lang.Integer(instance.getMinimumMatchLength()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2__int__int_Int_Serializer.serialize(new java.lang.Integer(instance.getMinimumMatchLength()), ns1_minimumMatchLength_QNAME, null, writer, context);
        if (instance.getSuffixes() != null) {
            for (int i = 0; i < instance.getSuffixes().length; ++i) {
                ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getSuffixes()[i], ns1_suffixes_QNAME, null, writer, context);
            }
        }
        if (new Boolean(instance.isReadSubdirs()) == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2__boolean__boolean_Boolean_Serializer.serialize(new Boolean(instance.isReadSubdirs()), ns1_readSubdirs_QNAME, null, writer, context);
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getPathToFiles(), ns1_pathToFiles_QNAME, null, writer, context);
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getBasecodeDir(), ns1_basecodeDir_QNAME, null, writer, context);
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getStoreMatches(), ns1_storeMatches_QNAME, null, writer, context);
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getClustertype(), ns1_clustertype_QNAME, null, writer, context);
        if (instance.getCountryLang() == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getCountryLang(), ns1_countryLang_QNAME, null, writer, context);
        if (instance.getTitle() == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getTitle(), ns1_title_QNAME, null, writer, context);
        if (instance.getOriginalDir() == null) {
            throw new SerializationException("literal.unexpectedNull");
        }
        ns2_myns2_string__java_lang_String_String_Serializer.serialize(instance.getOriginalDir(), ns1_originalDir_QNAME, null, writer, context);
    }
}
