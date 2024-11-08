package org.ogce.schemas.gfac.wsdl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * This sample demonstrates how to roundtrip XML document (roundtrip is not
 * exact but infoset level)
 */
public class RoundTrip {

    private static final String PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/features.html#xmldecl-standalone";

    private static final String PROPERTY_SERIALIZER_INDENTATION = "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";

    private XmlPullParser parser;

    private XmlSerializer serializer;

    private RoundTrip(XmlPullParser parser, XmlSerializer serializer) {
        this.parser = parser;
        this.serializer = serializer;
    }

    private void writeStartTag() throws XmlPullParserException, IOException {
        if (!parser.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
            for (int i = parser.getNamespaceCount(parser.getDepth() - 1); i <= parser.getNamespaceCount(parser.getDepth()) - 1; i++) {
                serializer.setPrefix(parser.getNamespacePrefix(i), parser.getNamespaceUri(i));
            }
        }
        serializer.startTag(parser.getNamespace(), parser.getName());
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            serializer.attribute(parser.getAttributeNamespace(i), parser.getAttributeName(i), parser.getAttributeValue(i));
        }
    }

    private void writeToken(int eventType) throws XmlPullParserException, IOException {
        switch(eventType) {
            case XmlPullParser.START_DOCUMENT:
                Boolean standalone = (Boolean) parser.getProperty(PROPERTY_XMLDECL_STANDALONE);
                serializer.startDocument(parser.getInputEncoding(), standalone);
                break;
            case XmlPullParser.END_DOCUMENT:
                serializer.endDocument();
                break;
            case XmlPullParser.START_TAG:
                writeStartTag();
                break;
            case XmlPullParser.END_TAG:
                serializer.endTag(parser.getNamespace(), parser.getName());
                break;
            case XmlPullParser.IGNORABLE_WHITESPACE:
                String s = parser.getText();
                serializer.ignorableWhitespace(s);
                break;
            case XmlPullParser.TEXT:
                serializer.text(parser.getText());
                break;
            case XmlPullParser.ENTITY_REF:
                serializer.entityRef(parser.getName());
                break;
            case XmlPullParser.CDSECT:
                serializer.cdsect(parser.getText());
                break;
            case XmlPullParser.PROCESSING_INSTRUCTION:
                serializer.processingInstruction(parser.getText());
                break;
            case XmlPullParser.COMMENT:
                serializer.comment(parser.getText());
                break;
            case XmlPullParser.DOCDECL:
                serializer.docdecl(parser.getText());
                break;
        }
    }

    private void roundTrip() throws XmlPullParserException, IOException {
        parser.nextToken();
        writeToken(XmlPullParser.START_DOCUMENT);
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            writeToken(parser.getEventType());
            parser.nextToken();
        }
        writeToken(XmlPullParser.END_DOCUMENT);
    }

    public static void roundTrip(Reader reader, Writer writer, String indent) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser pp = factory.newPullParser();
        pp.setInput(reader);
        XmlSerializer serializer = factory.newSerializer();
        serializer.setOutput(writer);
        if (indent != null) {
            serializer.setProperty(PROPERTY_SERIALIZER_INDENTATION, indent);
        }
        (new RoundTrip(pp, serializer)).roundTrip();
    }

    public static void main(String[] args) throws Exception {
        String XML = "<test><foo>fdf</foo></test>";
        Reader r = new StringReader(XML);
        Writer w = new StringWriter();
        roundTrip(r, w, "  ");
        System.out.println("indented XML=" + w);
    }
}
