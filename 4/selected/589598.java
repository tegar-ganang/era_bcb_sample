package syntelos.lang;

import alto.io.Code;
import alto.io.Check;
import alto.io.Output;
import alto.io.u.Objmap;
import alto.lang.Buffer;
import alto.lang.Date;
import alto.lang.Header;
import alto.lang.InputStream;
import alto.lang.OutputStream;
import alto.sys.IO;
import alto.sys.Reference;
import java.net.URL;

/**
 * <p> Collection of name, value pairs used in meta data files and
 * messages, and HTTP headers.  </p>
 * 
 * <p> A special implementation of the XML I/O context intended for
 * reuse in other contexts or document I/O under HTTP.  </p>
 * 
 * 
 * @author jdp
 * @since 1.5
 */
public class Headers extends alto.io.u.Objmap implements alto.lang.Headers {

    public static final void Copy(alto.lang.Headers from, alto.lang.Headers to) {
        alto.lang.Header header;
        for (int cc = 0, count = from.countHeaders(); cc < count; cc++) {
            header = from.getHeader(cc);
            if (null != header) to.setHeader(header);
        }
    }

    public static final alto.io.Input Ctor(java.io.InputStream in) {
        if (in instanceof alto.io.Input) return (alto.io.Input) in; else return new InputStream(in);
    }

    /**
     * Upcase the ascii letters.  Otherwise return the input value.
     */
    public static final char Camel0(char ch) {
        switch(ch) {
            case 'a':
                return 'A';
            case 'b':
                return 'B';
            case 'c':
                return 'C';
            case 'd':
                return 'D';
            case 'e':
                return 'E';
            case 'f':
                return 'F';
            case 'g':
                return 'G';
            case 'h':
                return 'H';
            case 'i':
                return 'I';
            case 'j':
                return 'J';
            case 'k':
                return 'K';
            case 'l':
                return 'L';
            case 'm':
                return 'M';
            case 'n':
                return 'N';
            case 'o':
                return 'O';
            case 'p':
                return 'P';
            case 'q':
                return 'Q';
            case 'r':
                return 'R';
            case 's':
                return 'S';
            case 't':
                return 'T';
            case 'u':
                return 'U';
            case 'v':
                return 'V';
            case 'w':
                return 'W';
            case 'x':
                return 'X';
            case 'y':
                return 'Y';
            case 'z':
                return 'Z';
            default:
                return ch;
        }
    }

    /**
     * Upcase the first character in the input string, downcase the
     * remainder of the input string.
     */
    public static final java.lang.String Camel0(java.lang.String name) {
        return (Camel0(name.charAt(0)) + name.substring(1).toLowerCase());
    }

    /**
     * Convert any string into class name "camel" case.  The first
     * character is upcased, and following characters are downcased.
     * Hyphens and underscores (are dropped and) prefix upcased
     * substrings.
     */
    public static final java.lang.String Camel(java.lang.String name) {
        java.util.StringTokenizer strtok = new java.util.StringTokenizer(name, "-_");
        int count = strtok.countTokens();
        if (1 == count) return Camel0(name); else {
            java.lang.StringBuilder strbuf = new java.lang.StringBuilder();
            for (int cc = 0; cc < count; cc++) {
                java.lang.String tok = strtok.nextToken();
                tok = Camel0(tok);
                strbuf.append(tok);
            }
            return strbuf.toString();
        }
    }

    protected static org.w3c.dom.bootstrap.DOMImplementationRegistry DomImplReg;

    protected static org.w3c.dom.ls.DOMImplementationLS DomImpl;

    protected static java.lang.String DomImplFeatures;

    private Objmap attributes;

    protected boolean hasHeaderUpdates;

    protected IO.Edge buffer;

    /**
     * Copy headers.
     */
    public Headers(alto.lang.Headers copy) {
        super();
        Copy(copy, this);
    }

    public Headers(alto.sys.Xml cx) {
        super();
        if (cx instanceof alto.lang.Headers) {
            alto.lang.Headers from = (alto.lang.Headers) cx;
            Copy(from, this);
        }
    }

    public Headers() {
        super();
    }

    /**
     * Read headers format from input stream.  Caller must close the
     * stream, as required.
     */
    public Headers(java.io.InputStream in) throws java.io.IOException {
        this(Ctor(in));
    }

    /**
     * Read headers format from input stream.  Caller must close the
     * stream, as required.
     */
    public Headers(alto.io.Input in) throws java.io.IOException {
        super();
        this.readHeaders(in);
    }

    protected alto.lang.Header newHeader(java.lang.String string) {
        return new Header(string);
    }

    protected alto.lang.Header newHeader(java.lang.String name, java.lang.String value) {
        return new Header(name, value);
    }

    protected alto.lang.Header newHeader(java.lang.String name, java.lang.String value, Object parsed) {
        return new Header(name, value, parsed);
    }

    public final alto.lang.Headers cloneHeaders() {
        syntelos.lang.Headers clone = (syntelos.lang.Headers) super.cloneObjmap();
        if (null != this.attributes) clone.attributes = this.attributes.cloneObjmap();
        return clone;
    }

    public alto.io.Uri getUri() {
        return null;
    }

    /**
     * @see syntelos.sys.Statistics
     * @see syntelos.sx.methods.Status
     * 
     * @see syntelos.sys.Description
     * @see syntelos.sx.methods.Desc
     */
    public final alto.lang.buffer.Abstract copyMessage() throws java.io.IOException {
        alto.lang.buffer.Abstract copy = new alto.lang.buffer.Abstract();
        Output out = copy.openOutput();
        this.writeMessage(out);
        return copy;
    }

    protected void messageRead() throws java.io.IOException {
    }

    protected void messageWritten() throws java.io.IOException {
    }

    public void formatMessage() throws java.io.IOException {
    }

    public void readMessage(alto.io.Input in) throws java.io.IOException {
        this.readHeaders(in);
        messageRead();
    }

    public void writeMessage(alto.io.Output out) throws java.io.IOException {
        this.writeHeaders(out);
        messageWritten();
    }

    public final void writeHeaders(alto.io.Output out) throws java.io.IOException {
        alto.lang.Header header;
        for (int cc = 0, count = this.countHeaders(); cc < count; cc++) {
            header = this.getHeader(cc);
            if (null != header) {
                header.writeln(out);
            }
        }
        out.println();
        out.flush();
    }

    public final void readHeaders(java.io.File file) throws java.io.IOException {
        alto.io.Input in = new InputStream(new java.io.FileInputStream(file));
        try {
            this.readHeaders(in);
        } finally {
            in.close();
        }
    }

    public final void readHeaders(alto.io.Input in) throws java.io.IOException {
        java.lang.String line;
        while (null != (line = in.readLine())) {
            alto.lang.Header header = this.newHeader(line);
            this.addHeader(header);
        }
    }

    public final void clearHeaders() {
        this.clear();
    }

    public final int countHeaders() {
        return this.size();
    }

    public final alto.lang.Header getHeader(int idx) {
        return (alto.lang.Header) this.value(idx);
    }

    public final java.lang.String getHeaderName(int idx) {
        alto.lang.Header header = this.getHeader(idx);
        if (null != header) return header.getName(); else return null;
    }

    public final java.lang.String getHeaderValue(int idx) {
        alto.lang.Header header = this.getHeader(idx);
        if (null != header) return header.getValue(); else return null;
    }

    public final alto.lang.Header[] listHeaders(java.lang.String name) {
        return (alto.lang.Header[]) this.list(name, alto.lang.Header.class);
    }

    public final boolean isList(java.lang.String name) {
        return (1 < this.count(name));
    }

    public final alto.lang.Header getHeader(java.lang.String name) {
        return (alto.lang.Header) this.get(name);
    }

    public final java.lang.String validateHeaderString(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.toString(); else throw new alto.sys.BadRequestException();
    }

    public final java.lang.String getHeaderAsString(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.toString(); else return null;
    }

    public final Boolean getHeaderBoolean(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedBoolean(); else return null;
    }

    public final boolean getHeaderBool(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedBoolean().booleanValue(); else return false;
    }

    public final boolean getHeaderBool(java.lang.String name, boolean defv) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedBoolean().booleanValue(); else return defv;
    }

    public final Number getHeaderNumber(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedNumber(); else return null;
    }

    public final Integer getHeaderInteger(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedInteger(); else return null;
    }

    public final int getHeaderInt(java.lang.String name, int defv) {
        Integer value = this.getHeaderInteger(name);
        if (null != value) return value.intValue(); else return defv;
    }

    public final Float getHeaderFloat(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedFloat(); else return null;
    }

    public final float getHeaderFloat(java.lang.String name, float defv) {
        Float value = this.getHeaderFloat(name);
        if (null != value) return value.intValue(); else return defv;
    }

    public final Long getHeaderLong(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedLong(); else return null;
    }

    public final long getHeaderLong(java.lang.String name, long defv) {
        Long value = this.getHeaderLong(name);
        if (null != value) return value.longValue(); else return defv;
    }

    public final java.math.BigInteger getHeaderHex(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedHex(); else return null;
    }

    public final Object getHeaderObject(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsed(); else return null;
    }

    public final java.lang.String getHeaderString(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getValue(); else return null;
    }

    public final java.lang.String[] listHeaderString(java.lang.String name) {
        alto.lang.Header[] headers = this.listHeaders(name);
        if (null != headers) {
            int count = headers.length;
            java.lang.String[] re = new java.lang.String[count];
            for (int cc = 0; cc < count; cc++) {
                re[cc] = headers[cc].getValue();
            }
            return re;
        } else return null;
    }

    public final java.lang.String getHeaderString(java.lang.String name, java.lang.String defv) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) {
            java.lang.String value = header.getValue();
            if (null != value) return value;
        }
        return defv;
    }

    public final java.lang.String[] getHeaderStringArray(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedStringArray(); else return null;
    }

    public final long getHeaderDate(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null == header) return -1L; else return header.getDate();
    }

    public final URL getHeaderURL(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedURL(); else return null;
    }

    public final java.lang.Class getHeaderClass(java.lang.String name) {
        alto.lang.Header header = this.getHeader(name);
        if (null != header) return header.getParsedClass(); else return null;
    }

    public final alto.lang.Type getHeaderType(java.lang.String name) {
        alto.lang.Header header = (alto.lang.Header) this.getHeader(name);
        if (null != header) return header.getParsedType(); else return null;
    }

    public final boolean hasHeader(java.lang.String name) {
        return (null != this.getHeader(name));
    }

    public final boolean hasHeader(alto.lang.Header header) {
        alto.lang.Header test = this.getHeader(header.getName());
        if (null != test) return (test.equals(header)); else return false;
    }

    public final boolean hasNotHeader(java.lang.String name) {
        return (null == this.getHeader(name));
    }

    public final boolean hasNotHeader(alto.lang.Header header) {
        alto.lang.Header test = this.getHeader(header.getName());
        if (null != test) return (!test.equals(header)); else return true;
    }

    public final boolean hasHeader(java.lang.String name, java.lang.String value) {
        alto.lang.Header test = this.getHeader(name);
        return (null != test && test.getValue().equals(value));
    }

    public final void setHeader(java.lang.String name, java.lang.String value) {
        if (null != name && null != value) {
            alto.lang.Header header = this.newHeader(name, value);
            this.setHeader(header);
        } else if (null != name) this.remove(name);
    }

    public final void setHeader(java.lang.String name, java.lang.String[] value) {
        if (null != value) {
            java.lang.StringBuilder strbuf = new java.lang.StringBuilder();
            for (int cc = 0, count = value.length; cc < count; cc++) {
                if (0 < cc) strbuf.append(';');
                strbuf.append(value[cc]);
            }
            alto.lang.Header header = this.newHeader(name, strbuf.toString());
            this.setHeader(header);
        } else {
            alto.lang.Header header = this.newHeader(name, null);
            this.setHeader(header);
        }
    }

    public final void setHeader(java.lang.String name, int value) {
        alto.lang.Header header = this.newHeader(name, java.lang.String.valueOf(value), new Integer(value));
        this.setHeader(header);
    }

    public final void setHeader(java.lang.String name, long value) {
        alto.lang.Header header = this.newHeader(name, java.lang.String.valueOf(value), new Long(value));
        this.setHeader(header);
    }

    public final void setHeaderDate(java.lang.String name, long value) {
        if (0L < value) {
            alto.lang.Header header = this.newHeader(name, Date.ToString(value), new Long(value));
            this.setHeader(header);
        }
    }

    public final void setHeader(java.lang.String name, Object value) {
        if (null != value) {
            alto.lang.Header header = this.newHeader(name, value.toString(), value);
            this.setHeader(header);
        }
    }

    public final void setHeader(alto.lang.Header header) {
        if (null != header) {
            this.put(header.getName(), header);
        }
    }

    public final void addHeader(java.lang.String name, java.lang.String value) {
        if (null != name && null != value) {
            alto.lang.Header[] list = this.listHeaders(name);
            if (null != list) {
                for (int cc = 0, count = list.length; cc < count; cc++) {
                    alto.lang.Header header = list[cc];
                    if (header.equals(name, value)) return;
                }
            }
            alto.lang.Header header = this.newHeader(name, value);
            this.append(header.getName(), header);
        }
    }

    public final void addHeader(alto.lang.Header header) {
        if (null != header) {
            alto.lang.Header[] list = this.listHeaders(header.getName());
            if (null != list) {
                for (int cc = 0, count = list.length; cc < count; cc++) {
                    alto.lang.Header test = list[cc];
                    if (test.equals(header)) return;
                }
            } else {
                this.append(header.getName(), header);
            }
        }
    }

    public final void removeHeader(java.lang.String name) {
        if (null != name) this.remove(name);
    }

    public final void removeHeader(alto.lang.Header header) {
        if (null != header) this.remove(header.getName());
    }

    public final org.w3c.dom.bootstrap.DOMImplementationRegistry getDOMImplementationRegistry() {
        if (null == DomImplReg) {
            try {
                DomImplReg = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
            } catch (java.lang.Throwable exc) {
                throw new alto.sys.Error.Bug(exc);
            }
        }
        return DomImplReg;
    }

    public final void setDOMImplementationRegistry(org.w3c.dom.bootstrap.DOMImplementationRegistry reg) {
        DomImplReg = reg;
    }

    public final org.w3c.dom.ls.DOMImplementationLS getDOMImplementation() {
        if (null == DomImpl) {
            java.lang.String features = this.getDOMImplementationFeatures();
            org.w3c.dom.DOMImplementation test = this.createDOMImplementation(features);
            if (test instanceof org.w3c.dom.ls.DOMImplementationLS) DomImpl = (org.w3c.dom.ls.DOMImplementationLS) test; else throw new alto.sys.Error.Bug("DOM not LS compatible for features '" + features + "'.");
        }
        return DomImpl;
    }

    public final org.w3c.dom.DOMImplementation getDOMImplementation2() {
        return (org.w3c.dom.DOMImplementation) this.getDOMImplementation();
    }

    public final java.lang.String getDOMImplementationFeatures() {
        return DomImplFeatures;
    }

    public final void setDOMImplementationFeatures(java.lang.String features) {
        DomImpl = null;
        DomImplFeatures = features;
    }

    public final void setDOMImplementation(org.w3c.dom.ls.DOMImplementationLS impl) {
        DomImpl = impl;
    }

    public final org.w3c.dom.DOMImplementation createDOMImplementation(java.lang.String features) {
        org.w3c.dom.DOMImplementation test = this.getDOMImplementationRegistry().getDOMImplementation(features);
        if (null == test) throw new alto.sys.Error.Bug(features); else return test;
    }

    public final org.w3c.dom.Document createDocument() {
        return this.getDOMImplementation2().createDocument(null, null, null);
    }

    public final org.w3c.dom.Document createDocument(java.lang.String ns, java.lang.String qn) {
        return this.getDOMImplementation2().createDocument(ns, qn, null);
    }

    public final org.w3c.dom.Document createDocument(java.lang.String qn, java.lang.String pid, java.lang.String sid) {
        org.w3c.dom.DOMImplementation dom = this.getDOMImplementation2();
        org.w3c.dom.DocumentType type = dom.createDocumentType(qn, pid, sid);
        return dom.createDocument(null, null, type);
    }

    public final org.w3c.dom.Document readDocument(alto.sys.IO.Source url) throws org.w3c.dom.DOMException, org.w3c.dom.ls.LSException {
        return alto.sys.Xml.Tools.ReadDocument(this, url);
    }

    public final org.w3c.dom.Document readDocument(alto.sys.IO.Source url, java.io.InputStream in) throws org.w3c.dom.DOMException, org.w3c.dom.ls.LSException {
        return alto.sys.Xml.Tools.ReadDocument(this, url, in);
    }

    public final void writeDocument(org.w3c.dom.Document doc, alto.sys.IO.Target url) throws org.w3c.dom.DOMException, org.w3c.dom.ls.LSException {
        alto.sys.Xml.Tools.WriteDocument(this, doc, url);
    }

    public final void writeDocument(org.w3c.dom.Document doc, alto.sys.IO.Target url, java.io.OutputStream out) throws org.w3c.dom.DOMException, org.w3c.dom.ls.LSException {
        alto.sys.Xml.Tools.WriteDocument(this, doc, url, out);
    }

    public final org.w3c.dom.traversal.NodeIterator createNodeIterator(org.w3c.dom.Document doc, int show) throws org.w3c.dom.DOMException {
        return alto.sys.Xml.Tools.CreateNodeIterator(doc, show, null);
    }

    public final org.w3c.dom.traversal.NodeIterator createNodeIterator(org.w3c.dom.Document doc, org.w3c.dom.traversal.NodeFilter filter) throws org.w3c.dom.DOMException {
        return alto.sys.Xml.Tools.CreateNodeIterator(doc, org.w3c.dom.traversal.NodeFilter.SHOW_ALL, filter);
    }

    public final org.w3c.dom.traversal.NodeIterator createNodeIterator(org.w3c.dom.Document doc) throws org.w3c.dom.DOMException {
        return alto.sys.Xml.Tools.CreateNodeIterator(doc, org.w3c.dom.traversal.NodeFilter.SHOW_ELEMENT, null);
    }

    public final boolean hasBuffer() {
        return (this.buffer instanceof Buffer);
    }

    /**
     * @return The current or a new {@link alto.lang.buffer.Abstract} object.  If the
     * current edge device is not an instance of {@link alto.lang.buffer.Abstract},
     * it will be replaced with a new instance of {@link alto.lang.buffer.Abstract}
     * by a call to this method.
     */
    public alto.lang.buffer.Abstract getCreateBuffer() {
        IO.Edge buffer = this.buffer;
        if (null == buffer || (!(buffer instanceof alto.lang.buffer.Abstract))) {
            buffer = new alto.lang.buffer.Abstract();
            this.buffer = buffer;
        }
        return (alto.lang.buffer.Abstract) buffer;
    }

    /**
     * @return The current {@link IO$Edge} object.
     */
    public final IO.Edge getBufferEdge() {
        return this.buffer;
    }

    public final byte[] getBuffer() {
        Buffer buffer = this.getBufferB();
        if (null != buffer) return ((Buffer) buffer).getBuffer(); else return null;
    }

    public final int getBufferLength() {
        Buffer buffer = this.getBufferB();
        if (null != buffer) return ((Buffer) buffer).getBufferLength(); else return 0;
    }

    public final CharSequence getCharContent(boolean igEncErr) throws java.io.IOException {
        byte[] bits = this.getBuffer();
        if (null != bits) {
            char[] cary = alto.io.u.Utf8.decode(bits);
            return new String(cary, 0, cary.length);
        } else return null;
    }

    /**
     * @return The current {@link IO$Edge} object cast to {@link
     * Buffer} or null.
     */
    public final Buffer getBufferB() {
        IO.Edge buffer = this.buffer;
        if (buffer instanceof Buffer) return (Buffer) buffer; else return null;
    }

    /**
     * @return The current {@link IO$Edge} object cast to {@link
     * alto.lang.buffer.Abstract} or null.
     */
    public final alto.lang.buffer.Abstract getBufferIOB() {
        IO.Edge buffer = this.buffer;
        if (buffer instanceof alto.lang.buffer.Abstract) return (alto.lang.buffer.Abstract) buffer; else return null;
    }

    /**
     * @param io Define the current {@link IO$Edge} object.
     */
    public final void setBuffer(IO.Edge io) {
        this.buffer = io;
    }

    /**
     * @param filter Define the current {@link IO$Edge} object from
     * the argument.
     */
    public final void setBuffer(IO.Filter filter) {
        IO.Edge io = filter.getIOEdge();
        if (null != io) this.setBuffer(io); else if (null != filter) {
            if (filter instanceof IO.Edge) this.setBuffer((IO.Edge) filter); else throw new alto.sys.Error.Argument("Filter in class '" + filter.getClass().getName() + "' has no edge");
        } else throw new alto.sys.Error.Argument("Null argument 'IO.Filter filter'");
    }

    public final void clearBuffer() {
        alto.lang.buffer.Abstract iob = this.getBufferIOB();
        if (null != iob) {
            try {
                iob.close();
            } catch (java.io.IOException ignore) {
            }
        }
    }

    public final void writeToBuffer(java.lang.Throwable t) throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        java.io.OutputStream buf = (java.io.OutputStream) iob.openOutputStream();
        java.io.PrintStream out = new java.io.PrintStream(buf);
        t.printStackTrace(out);
    }

    public void writeToBuffer(Reference reference) throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        boolean close = false;
        java.io.InputStream in = reference.getInputStream();
        if (null == in) {
            in = reference.openInputStream();
            if (null == in) return;
            close = true;
        }
        try {
            iob.readFrom(in);
        } finally {
            if (close) in.close();
        }
    }

    public long lastModified() {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.lastModified();
    }

    public java.lang.String lastModifiedString() {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.lastModifiedString();
    }

    public java.nio.channels.ReadableByteChannel openChannelReadable() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openChannelReadable();
    }

    public java.nio.channels.ReadableByteChannel getChannelReadable() {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getChannelReadable();
    }

    public java.io.InputStream openInputStream() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openInputStream();
    }

    public alto.io.Input openInput() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openInput();
    }

    public java.io.InputStream getInputStream() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getInputStream();
    }

    public alto.io.Input getInput() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getInput();
    }

    public java.nio.channels.WritableByteChannel openChannelWritable() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openChannelWritable();
    }

    public java.nio.channels.WritableByteChannel getChannelWritable() {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getChannelWritable();
    }

    public java.io.OutputStream openOutputStream() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openOutputStream();
    }

    public alto.io.Output openOutput() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.openOutput();
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getOutputStream();
    }

    public alto.io.Output getOutput() throws java.io.IOException {
        alto.lang.buffer.Abstract iob = this.getCreateBuffer();
        return iob.getOutput();
    }

    public void close() throws java.io.IOException {
        alto.lang.buffer.Abstract buffer = this.getBufferIOB();
        if (null != buffer) buffer.close();
    }

    public void flush() throws java.io.IOException {
    }

    public final Object getAttribute(java.lang.String name) {
        Objmap attributes = this.attributes;
        if (null == attributes) return null; else return attributes.get(name);
    }

    public final void setAttribute(java.lang.String name, java.lang.Object value) {
        Objmap attributes = this.attributes;
        if (null == attributes) {
            attributes = new Objmap();
            this.attributes = attributes;
        }
        attributes.put(name, value);
    }

    public int read(byte[] buf, int ofs, int len) throws java.io.IOException {
        alto.io.Input in = this.getInput();
        if (null != in) return in.read(buf, ofs, len); else throw new alto.sys.Error.State();
    }

    public byte[] readMany(int many) throws java.io.IOException {
        alto.io.Input in = this.getInput();
        if (null != in) return in.readMany(many); else throw new alto.sys.Error.State();
    }

    public java.lang.String readLine() throws java.io.IOException {
        alto.io.Input in = this.getInput();
        if (null != in) return in.readLine(); else throw new alto.sys.Error.State();
    }

    public final void write(int uint8) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.write(uint8); else throw new alto.sys.Error.State();
    }

    public final void write(byte[] buf, int ofs, int len) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.write(buf, ofs, len); else throw new alto.sys.Error.State();
    }

    public final void print(char ch) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.print(ch); else throw new alto.sys.Error.State();
    }

    public final void print(java.lang.String string) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.print(string); else throw new alto.sys.Error.State();
    }

    public final void println() throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.println(); else throw new alto.sys.Error.State();
    }

    public final void println(char ch) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.println(ch); else throw new alto.sys.Error.State();
    }

    public final void println(java.lang.String string) throws java.io.IOException {
        alto.io.Output out = this.getOutput();
        if (null != out) out.println(string); else throw new alto.sys.Error.State();
    }
}
