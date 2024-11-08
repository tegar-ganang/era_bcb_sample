package intellibitz.sted.io;

import intellibitz.sted.util.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * FontMapXMLReader reads the FontMap into an xml document format generates sax
 * events that can be used to transform fontmap to xml document
 */
class FontMapXMLReader implements XMLReader {

    private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

    private ContentHandler contentHandler;

    private final String nsu = Resources.EMPTY_STRING;

    private final String indent = "\n";

    FontMapXMLReader() {
    }

    /**
     * @param input
     * @throws IOException
     * @throws SAXException
     */
    public void parse(InputSource input) throws IOException, SAXException {
        if (contentHandler == null) {
            throw new SAXException("No content contentHandler");
        }
        final String rootElement = "fontmap";
        final BufferedReader bufferedReader = new BufferedReader(input.getCharacterStream());
        contentHandler.startDocument();
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, Resources.EMPTY_STRING, "name", "ID", bufferedReader.readLine());
        atts.addAttribute(nsu, Resources.EMPTY_STRING, "version", "CDATA", bufferedReader.readLine());
        contentHandler.startElement(nsu, rootElement, rootElement, atts);
        newLine();
        contentHandler.startElement(nsu, "font", "font", EMPTY_ATTRIBUTES);
        newLine();
        writeElement(nsu, "font_from", "font_from", getFontAttributes(bufferedReader.readLine()));
        newLine();
        writeElement(nsu, "font_to", "font_to", getFontAttributes(bufferedReader.readLine()));
        newLine();
        contentHandler.endElement(nsu, "font", "font");
        newLine();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            writeFontEntry(line);
            newLine();
        }
        contentHandler.endElement(nsu, rootElement, rootElement);
        newLine();
        contentHandler.endDocument();
    }

    private Attributes getFontAttributes(String line) {
        final AttributesImpl atts = new AttributesImpl();
        final StringTokenizer stringTokenizer = new StringTokenizer(line, Resources.SYMBOL_ASTERISK);
        atts.addAttribute(nsu, Resources.EMPTY_STRING, "value", "CDATA", stringTokenizer.nextToken());
        atts.addAttribute(nsu, Resources.EMPTY_STRING, "path", "CDATA", stringTokenizer.nextToken());
        return atts;
    }

    private Attributes getValueAttribute(String val) {
        return getAttributeImpl(nsu, Resources.EMPTY_STRING, "value", "CDATA", val);
    }

    private static Attributes getAttributeImpl(String uri, String localName, String qName, String type, String value) {
        final AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(uri, localName, qName, type, value);
        return atts;
    }

    private void writeElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        contentHandler.startElement(uri, localName, qName, atts);
        contentHandler.endElement(uri, localName, qName);
    }

    private void newLine() throws SAXException {
        contentHandler.ignorableWhitespace(indent.toCharArray(), 0, indent.length());
    }

    /**
     * <font_entry> <entry_from value="NA"/> <entry_to value="��"/>
     * <begins_with/> <ends_with/> <followed_by value="Nu"/> <preceded_by
     * value="Nu"/> <conditional value="AND"/> </font_entry>
     *
     * @param entry
     * @throws SAXException
     */
    private void writeFontEntry(String entry) throws SAXException {
        contentHandler.startElement(nsu, "font_entry", "font_entry", EMPTY_ATTRIBUTES);
        newLine();
        final StringTokenizer stringTokenizer = new StringTokenizer(entry, Resources.ENTRY_TOSTRING_DELIMITER);
        int i = 0;
        while (stringTokenizer.hasMoreElements()) {
            final String token = stringTokenizer.nextToken();
            if (!(token.toLowerCase().startsWith("null") || token.toLowerCase().startsWith("false") || Resources.EMPTY_STRING.equals(token))) {
                switch(i) {
                    case 0:
                        writeElement(nsu, "entry_from", "entry_from", getValueAttribute(token));
                        newLine();
                        break;
                    case 1:
                        writeElement(nsu, "entry_to", "entry_to", getValueAttribute(token));
                        newLine();
                        break;
                    case 2:
                        writeElement(nsu, "begins_with", "begins_with", getValueAttribute(token));
                        newLine();
                        break;
                    case 3:
                        writeElement(nsu, "ends_with", "ends_with", getValueAttribute(token));
                        newLine();
                        break;
                    case 4:
                        writeElement(nsu, "followed_by", "followed_by", getValueAttribute(token));
                        newLine();
                        break;
                    case 5:
                        writeElement(nsu, "preceded_by", "preceded_by", getValueAttribute(token));
                        newLine();
                        break;
                    case 6:
                        writeElement(nsu, "conditional", "conditional", getValueAttribute(token));
                        newLine();
                        break;
                    default:
                        break;
                }
            }
            i++;
        }
        contentHandler.endElement(nsu, "font_entry", "font_entry");
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setContentHandler(ContentHandler handler) {
        contentHandler = handler;
    }

    public void parse(String systemId) throws IOException, SAXException {
    }

    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return false;
    }

    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    public DTDHandler getDTDHandler() {
        return null;
    }

    public void setDTDHandler(DTDHandler handler) {
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public void setEntityResolver(EntityResolver resolver) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setErrorHandler(ErrorHandler handler) {
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return null;
    }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
    }
}
