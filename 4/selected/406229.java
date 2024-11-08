package com.pbonhomme.xf.xml.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.pbonhomme.xf.utils.VariableParser;
import com.pbonhomme.xf.xml.XMLHandlerWriter;
import com.pbonhomme.xf.xml.XMLWriter;

public class VariableXMLTransformer extends AbstractXMLTransformer {

    private static final String VAR_TYPE_ATOMIC = "atomic";

    private static final String VAR_TYPE_LIST = "list";

    private static final String VAR_ELT_NAME = "var";

    private static final String VAR_ATT_NAME_NAME = "name";

    private static final String VAR_ATT_TYPE_NAME = "type";

    private static final String VAR_ATT_ELTNAME_NAME = "eltname";

    private static final char[] NL = { '\n' };

    public static final String SAX_NS_AWARE_SUFFIX = "sax.namespaceAware";

    public static final String SAX_VALIDATING_SUFFIX = "sax.validating";

    public static final String WRITER_ENCODING_SUFFIX = "writer.encoding";

    private boolean namespaceAware = true;

    private boolean validating = false;

    protected String encoding;

    public void load(TransformerFactory transformerFactory) throws TransformerException {
        logger.info("VariableXMLTransformer [" + getName() + "] loading...");
        logger.info("VariableXMLTransformer [" + getName() + "] loaded.");
    }

    public void init(Map<String, Object> properties) throws TransformerException {
        logger.info("VariableXMLTransformer [" + getName() + "] initializing...");
        this.encoding = XMLHandlerWriter.UTF_8;
        Set<String> _keys = properties.keySet();
        if (_keys != null && !_keys.isEmpty()) {
            Iterator<String> _iter = _keys.iterator();
            while (_iter.hasNext()) {
                String _key = _iter.next();
                if (_key != null) {
                    if (_key.endsWith(SAX_NS_AWARE_SUFFIX)) {
                        this.namespaceAware = VariableParser.getBoolean((String) properties.get(_key), Boolean.TRUE).booleanValue();
                    } else if (_key.endsWith(SAX_VALIDATING_SUFFIX)) {
                        this.validating = VariableParser.getBoolean((String) properties.get(_key), Boolean.FALSE).booleanValue();
                    } else if (_key.endsWith(WRITER_ENCODING_SUFFIX)) {
                        Object _o = properties.get(_key);
                        if (_o != null) this.encoding = (String) _o;
                    }
                }
            }
        }
        logger.info("VariableXMLTransformer [" + getName() + "] initialized.");
    }

    public void transform(String inFilename, String outFilename, Map<String, Object> variables) throws TransformerException {
        OutputStream _outstream = null;
        try {
            XMLReader _reader = makeXMLReader();
            XMLWriter _writer = new VariableXMLHandler(variables);
            _writer.setEncoding(encoding);
            _outstream = new FileOutputStream(new File(outFilename));
            _writer.setOutputStream(_outstream);
            _reader.setContentHandler(_writer.getContentHandler());
            _reader.parse(new org.xml.sax.InputSource(inFilename));
        } catch (Exception e) {
            throw new TransformerException(e.getMessage(), e);
        } finally {
            if (_outstream != null) {
                try {
                    _outstream.flush();
                    _outstream.close();
                } catch (java.io.IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    protected final XMLReader makeXMLReader() throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(namespaceAware);
        saxParserFactory.setValidating(validating);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        XMLReader parser = saxParser.getXMLReader();
        parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        return parser;
    }

    final class VariableXMLHandler extends XMLHandlerWriter {

        private Map<String, Object> map;

        VariableXMLHandler(Map<String, Object> map) {
            this.map = map;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals(VAR_ELT_NAME)) {
                String _varName = atts.getValue(VAR_ATT_NAME_NAME);
                String _varType = atts.getValue(VAR_ATT_TYPE_NAME);
                String _eltName = atts.getValue(VAR_ATT_ELTNAME_NAME);
                if (_eltName != null) handleVarElement(_varName, _varType, _eltName); else handleVarText(_varName);
            } else {
                super.startElement(namespaceURI, localName, qName, atts);
            }
        }

        private void handleVarText(String name) throws SAXException {
            if (map != null && !map.isEmpty()) {
                Object _object = map.get(name);
                if (_object != null) {
                    String _s = _object.toString();
                    if (_s != null) super.characters(_s.toCharArray(), 0, _s.length());
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void handleVarElement(String name, String type, String eltName) throws SAXException {
            if (map != null && !map.isEmpty()) {
                Object _object = map.get(name);
                if (_object != null) {
                    if (type.equals(VAR_TYPE_LIST)) {
                        if (_object instanceof java.util.List) {
                            java.util.List _list = (java.util.List) _object;
                            Iterator _iter = _list.iterator();
                            while (_iter.hasNext()) {
                                Object _o = _iter.next();
                                if (_o != null) {
                                    String _s = _o.toString();
                                    super.startElement(null, eltName, eltName, null);
                                    super.characters(_s.toCharArray(), 0, _s.length());
                                    super.endElement(null, eltName, eltName);
                                    super.ignorableWhitespace(NL, 0, NL.length);
                                }
                            }
                        }
                    } else {
                        String _s = _object.toString();
                        super.startElement(null, eltName, eltName, null);
                        super.characters(_s.toCharArray(), 0, _s.length());
                        super.endElement(null, eltName, eltName);
                        super.ignorableWhitespace(NL, 0, NL.length);
                    }
                }
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (qName.equals(VAR_ELT_NAME)) {
                ;
            } else {
                super.endElement(namespaceURI, localName, qName);
            }
        }
    }

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(VariableXMLTransformer.class);
}
