package com.teknokala.xtempore.xml.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.teknokala.xtempore.xml.AttributeEvent;
import com.teknokala.xtempore.xml.BufferedXMLInput;
import com.teknokala.xtempore.xml.CDataEvent;
import com.teknokala.xtempore.xml.EndTagEvent;
import com.teknokala.xtempore.xml.Mark;
import com.teknokala.xtempore.xml.StartTagEvent;
import com.teknokala.xtempore.xml.XMLDataEvent;
import com.teknokala.xtempore.xml.XMLEvent;
import com.teknokala.xtempore.xml.XMLInput;
import com.teknokala.xtempore.xml.XMLInputFactory;
import com.teknokala.xtempore.xml.XMLOutput;
import com.teknokala.xtempore.xml.XMLStream;
import com.teknokala.xtempore.xml.XMLStreamException;
import com.teknokala.xtempore.xml.filter.WhitespaceCompressor;
import com.teknokala.xtempore.xml.filter.XMLFilter;
import com.teknokala.xtempore.xml.filter.XMLFilterFactory;
import com.teknokala.xtempore.xml.filter.XMLInputFilter;
import com.teknokala.xtempore.xml.filter.WhitespaceCompressor.CompressionMethod;
import com.teknokala.xtempore.xml.writer.XMLWriter;

/**
 * Utility class for xml processing.
 *
 * @author Timo Santasalo <timo.santasalo@teknokala.com>
 * @see XMLStream
 */
public final class XMLUtil {

    private static char[] XML_ESCAPED_CHARS = new char[] { '\"', '&', '\'', '<', '>' };

    private static String[] XML_ESCAPED_STRINGS = new String[] { "&quot;", "&amp;", "&apos;", "&lt;", "&gt;" };

    private static String XML_ESCAPE_PREFIX = "&#x";

    private static String XML_ESCAPE_SUFFIX = ";";

    public static final String ANY_NS_URI = "xtempore://any.ns.uri";

    public static final XMLOutput NULL_OUT = new XMLOutput() {

        public void write(XMLEvent ev) throws XMLStreamException {
        }

        public void write(List<XMLEvent> events) throws XMLStreamException {
        }

        public void write(XMLEvent[] events) throws XMLStreamException {
        }

        public void close() {
        }

        public String getSystemId() {
            return null;
        }
    };

    public static final XMLFilter PASSTHROUGH = new XMLFilter() {

        public void flush(XMLOutput out) throws XMLStreamException {
        }

        public void filter(XMLEvent in, XMLOutput out) throws XMLStreamException {
            out.write(in);
        }
    };

    public static final XMLFilterFactory PASSTHROUGH_FACTORY = new XMLFilterFactory() {

        public XMLFilter open() {
            return PASSTHROUGH;
        }
    };

    private XMLUtil() {
    }

    public static String toString(Iterable<XMLDataEvent> data) {
        StringBuffer ret = new StringBuffer();
        for (XMLDataEvent ev : data) {
            ret.append(ev.getData());
        }
        return ret.toString();
    }

    public static String toString(XMLInputFactory in) {
        return toString(in, (XMLFilterFactory) null);
    }

    public static String toString(XMLInputFactory in, CompressionMethod method) {
        return toString(in, new WhitespaceCompressor(method));
    }

    public static String toString(XMLInputFactory in, XMLFilterFactory flt) {
        try {
            in = flt == null ? in : new XMLInputFilter(in, flt);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLWriter wr = new XMLWriter(out, "UTF-8");
            XMLInput xi = in.open();
            XMLUtil.copy(xi, wr);
            xi.close();
            wr.close();
            out.close();
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML serialization failed", e);
        }
    }

    public static String readCData(XMLInput from) throws XMLStreamException {
        StringBuffer ret = new StringBuffer();
        while (from.peek() instanceof CDataEvent) {
            ret.append(((CDataEvent) from.read()).getData());
        }
        return ret.toString();
    }

    public static void copy(XMLInput from, XMLOutput to) throws XMLStreamException {
        XMLEvent ev = from.read();
        while (ev != null) {
            to.write(ev);
            ev = from.read();
        }
    }

    public static StartTagEvent findStartTag(String nsUri, String name, BufferedXMLInput in) throws XMLStreamException {
        Mark m = in.mark();
        XMLEvent ev = in.read();
        while (ev != null) {
            if (ev instanceof StartTagEvent) {
                StartTagEvent sv = (StartTagEvent) ev;
                if (equals(nsUri, sv.getNamespaceURI()) && equals(name, sv.getLocalName())) {
                    m.release();
                    return sv;
                }
            }
            ev = in.read();
        }
        m.resetAndRelease();
        return null;
    }

    public static EndTagEvent copyTree(XMLInput from, XMLOutput to) throws XMLStreamException {
        return copyTree(from, to, 0);
    }

    public static EndTagEvent copyTree(XMLInput from, XMLOutput to, int startLevel) throws XMLStreamException {
        XMLEvent ev = from.read();
        int level = startLevel;
        while (ev != null) {
            if (ev != null) {
                if (ev instanceof StartTagEvent) {
                    level++;
                } else if (ev instanceof EndTagEvent) {
                    level--;
                    if (level == -1) {
                        return (EndTagEvent) ev;
                    }
                }
                to.write(ev);
            }
            ev = from.read();
        }
        if (level == 0) {
            return null;
        } else {
            throw new XMLStreamException("XML stream ended unexpectedly (level=" + level + ")!");
        }
    }

    public static EndTagEvent skipTree(XMLInput from) throws XMLStreamException {
        return copyTree(from, NULL_OUT, 0);
    }

    public static EndTagEvent skipTree(XMLInput from, int startLevel) throws XMLStreamException {
        return copyTree(from, NULL_OUT, startLevel);
    }

    public static List<AttributeEvent> findAttributes(String nsUri, String prefix, String attrName, BufferedXMLInput in, boolean mark) throws XMLStreamException {
        List<AttributeEvent> ret = new ArrayList<AttributeEvent>();
        Mark m = mark ? in.mark() : null;
        while (in.peek() instanceof AttributeEvent) {
            AttributeEvent av = (AttributeEvent) in.read();
            if ((nsUri == null || equals(nsUri, av.getNamespaceURI())) && (prefix == null || equals(prefix, av.getPrefix())) && (attrName == null || equals(attrName, av.getLocalName()))) {
                ret.add(av);
            }
        }
        if (mark) {
            m.resetAndRelease();
        }
        return ret;
    }

    public static String findAttributeValue(String nsUri, String attr, BufferedXMLInput in, boolean mark) throws XMLStreamException {
        Mark m = mark ? in.mark() : null;
        String ret = null;
        while (ret == null && in.peek() instanceof AttributeEvent) {
            AttributeEvent av = (AttributeEvent) in.read();
            if (equals(nsUri, av.getNamespaceURI()) && equals(attr, av.getLocalName())) {
                ret = av.getValue();
            }
        }
        if (mark) {
            m.resetAndRelease();
        }
        return ret;
    }

    public static void skipAttributes(XMLInput in) throws XMLStreamException {
        while (in.peek() instanceof AttributeEvent) {
            in.read();
        }
    }

    public static void skipCData(XMLInput in) throws XMLStreamException {
        while (in.peek() instanceof CDataEvent) {
            in.read();
        }
    }

    public static void copyAttributes(XMLInput in, XMLOutput out) throws XMLStreamException {
        while (in.peek() instanceof AttributeEvent) {
            out.write(in.read());
        }
    }

    public static void filter(XMLInputFactory in, XMLFilterFactory filter, XMLOutput out) throws XMLStreamException {
        XMLInputFilter xf = new XMLInputFilter(in, filter);
        XMLInput xi = xf.open();
        copyTree(xi, out);
        xi.close();
    }

    public static XMLSequence filter(XMLInputFactory in, XMLFilterFactory filter) throws XMLStreamException {
        XMLSequence ret = new XMLSequence();
        filter(in, filter, ret);
        return ret;
    }

    public static String escapeComments(String s) {
        return s;
    }

    public static void escapeXml(Reader in, PrintStream out) throws IOException {
        escapeXml(in, new PrintStreamWriter(out));
    }

    public static String escapeXml(char ch) {
        int p = Arrays.binarySearch(XML_ESCAPED_CHARS, ch);
        if (p >= 0) {
            return XML_ESCAPED_STRINGS[p];
        }
        return p >= 0 ? XML_ESCAPED_STRINGS[p] : Character.isDefined(ch) ? null : XML_ESCAPE_PREFIX + Integer.toHexString(ch) + XML_ESCAPE_SUFFIX;
    }

    public static void escapeXml(Reader in, Writer out) throws IOException {
        char c = (char) in.read();
        String rep;
        while (c != 0xffff) {
            rep = escapeXml(c);
            if (rep == null) {
                out.write(c);
            } else {
                out.write(rep);
            }
            c = (char) in.read();
        }
    }

    public static String escapeXml(String str) {
        StringBuffer ret = new StringBuffer();
        String rep;
        for (char ch : str.toCharArray()) {
            rep = escapeXml(ch);
            if (rep == null) {
                ret.append(ch);
            } else {
                ret.append(rep);
            }
        }
        return ret.toString();
    }

    private static boolean equals(Object a, Object b) {
        return ANY_NS_URI.equals(a) || a != null && a.equals(b);
    }

    private static final class PrintStreamWriter extends Writer {

        private final PrintStream out;

        public PrintStreamWriter(PrintStream out) {
            super();
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            out.print(new String(cbuf, off, len));
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            out.print(cbuf);
        }

        @Override
        public void write(int c) throws IOException {
            out.print((char) c);
        }

        @Override
        public void write(String str) throws IOException {
            out.print(str);
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            out.print(str.substring(off, off + len));
        }
    }
}
