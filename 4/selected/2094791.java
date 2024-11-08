package org.akrogen.core.xml.parsers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.akrogen.core.xml.XMLConstants;

/**
 * When XML content is parsed into org.w3c.dom.Document, the whitespace between
 * attributes and order of attributes is lost. Ex : if you have XML source like
 * this : <root name="...." id="..." > After parsing into Document and
 * serializing it, you have <root id="..." name="..." >
 * 
 * This class parse XML content to add attribute <b>whitespace_attributes</b>
 * into each element which have whitespace between attributes.
 * whitespace_attributes will contains information about whitespace and order of
 * attributes.
 * 
 * Ex : if you have XML source like this : <root name="...." id="..." >
 * 
 * parse method will return : <root name="...." id="..."
 * whitespace_attributes="@ |name|@rn |id|@ " > />
 * 
 * @version 1.0.0
 * @author <a href="mailto:angelo.zerr@gmail.com">Angelo ZERR</a>
 * 
 */
public class XMLWhitespaceParser extends AbstractXMLWhitespaceParser {

    private StringBuffer whiteSpaceBuffer;

    private StringBuffer attributeBuffer;

    private StringBuffer attributeListBuffer;

    /**
	 * Parse XML file to add <b>whitespace_attributes</b>.
	 * 
	 * @param f
	 * @return the XML source with <b>whitespace_attributes</b>
	 * @throws IOException
	 */
    public void parse(File f, Writer writer) throws IOException {
        parse(new FileReader(f), writer);
    }

    public void parse(String s, Writer writer) throws IOException {
        parse(new StringReader(s), writer);
    }

    public void parse(InputStream stream, Writer writer) throws IOException {
        super.parse(stream, writer);
    }

    public void parse(Reader reader, Writer writer) throws IOException {
        super.parse(reader, writer);
    }

    protected void parseEndElement(Object out) throws IOException {
        if (attributeListBuffer != null || whiteSpaceBuffer != null) {
            if (!cdataParsing) {
                write(" ", out);
                write(XMLConstants.WS_ATTRIBUTES_KEY, out);
                write("=\"", out);
                if (whiteSpaceBuffer != null) {
                    attributeListBuffer = XMLWhitespaceHelper.addValueToWhitespaceAttribute(attributeListBuffer, whiteSpaceBuffer, true);
                }
                if (attributeListBuffer != null) write(attributeListBuffer.toString(), out);
                write("\" ", out);
            }
        }
        attributeListBuffer = null;
        whiteSpaceBuffer = null;
        attributeBuffer = null;
        this.attributeParsing = false;
    }

    protected void parseAttributes(char c, Object out) throws IOException {
        this.attributeParsing = true;
        write(c, out);
        if (XMLWhitespaceHelper.isWhitespace(c)) {
            if (whiteSpaceBuffer == null) whiteSpaceBuffer = new StringBuffer();
            switch(c) {
                case ' ':
                    whiteSpaceBuffer.append(c);
                    break;
                case '\f':
                    whiteSpaceBuffer.append("f");
                    break;
                case '\t':
                    whiteSpaceBuffer.append("t");
                    break;
                case '\n':
                    whiteSpaceBuffer.append("n");
                    break;
                case '\r':
                    whiteSpaceBuffer.append("r");
                    break;
            }
            attributeBuffer = null;
        } else {
            if (whiteSpaceBuffer != null) {
                attributeListBuffer = XMLWhitespaceHelper.addValueToWhitespaceAttribute(attributeListBuffer, whiteSpaceBuffer, true);
            }
            whiteSpaceBuffer = null;
            if (c != '=') {
                if (attributeBuffer == null) attributeBuffer = new StringBuffer();
                attributeBuffer.append(c);
            } else {
                if (attributeBuffer != null) {
                    attributeListBuffer = XMLWhitespaceHelper.addValueToWhitespaceAttribute(attributeListBuffer, attributeBuffer, false);
                }
            }
        }
    }
}
