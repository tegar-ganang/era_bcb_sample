package de.mguennewig.pobjimport;

import java.io.*;
import java.util.*;

/** Simple class to generate an XML file on the fly.
 *
 * @author Michael G�nnewig
 */
public class XmlWriter extends Object {

    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String INDENT = "  ";

    private static final class Element extends Object {

        private final String name;

        private final boolean elementContent;

        Element(final String name, final boolean elementContent) {
            super();
            this.name = name;
            this.elementContent = elementContent;
        }

        public String getName() {
            return name;
        }

        public boolean isElementContent() {
            return elementContent;
        }
    }

    private PrintWriter out;

    private final Set<String> elementContent;

    private final Stack<Element> elementStack;

    private boolean openStartTag;

    private boolean doIndent;

    private int indentLevel;

    /** Creates a new XML writer with mixed content for all elements. */
    public XmlWriter(final PrintWriter out) {
        this(out, new String[0]);
    }

    /**
   * Creates a new XML writer with element-only content for the specified
   * elements.
   */
    public XmlWriter(final PrintWriter out, final String[] elementContent) {
        super();
        this.out = out;
        this.elementContent = new HashSet<String>(elementContent.length);
        for (int i = 0; i < elementContent.length; i++) this.elementContent.add(elementContent[i]);
        this.elementStack = new Stack<Element>();
        this.indentLevel = 0;
        this.doIndent = false;
        this.openStartTag = false;
    }

    /** Adds the XML prolog to the stream.
   *
   * <p><b>NOTE:</b> This method must only be called once and before every any
   * other data is written.</p>
   *
   * @param encoding the encoding used for the XML file.  This does <b>not</b>
   *   change the used encoding, but only states it for others.
   * @param standalone iff <code>true</code> no external entities are used
   * @param publicId optional PUBLIC identifier
   * @param systemId optional SYSTEM identifier.  THis is mandatory if a PUBLIC
   *   identifier is given
   * @see #addProlog(String,boolean,String)
   */
    public void addProlog(final String encoding, final boolean standalone, final String publicId, final String systemId) {
        String doctypeDecl = null;
        if (publicId != null && systemId == null) throw new IllegalArgumentException("need systemId for publicId");
        if (publicId != null || systemId != null) {
            doctypeDecl = "<!DOCTYPE ";
            if (publicId != null) doctypeDecl += "PUBLIC \"" + publicId + "\" "; else doctypeDecl += "SYSTEM ";
            doctypeDecl += "\"" + systemId + "\">";
        }
        addProlog(encoding, standalone, doctypeDecl);
    }

    /** Adds the XML prolog to the stream.
   *
   * <p><b>NOTE:</b> This method must only be called once and before every any
   * other data is written.</p>
   *
   * @param encoding states the encoding used for the XML file.
   *   This does <b>not</b> change the used encoding, but only states it for
   *   others.
   * @param standalone iff <code>true</code> no external entities are used
   * @param doctypeDecl optional DOCTYPE declaration
   */
    public void addProlog(final String encoding, final boolean standalone, final String doctypeDecl) {
        if (doIndent || openStartTag || indentLevel > 0 || elementStack.size() > 0) throw new IllegalStateException("can not write prolog if already content exists");
        out.print("<?xml version='1.0");
        if (encoding != null && encoding.length() > 0) {
            out.print("' encoding='");
            out.print(encoding);
        }
        if (!standalone) out.print("' standalone='no");
        out.println("'?>");
        if (doctypeDecl != null && doctypeDecl.length() > 0) out.println(doctypeDecl);
        out.println();
    }

    /** Closes the XML writer and the print writer. */
    public void close() {
        if (elementStack.size() > 0) {
            throw new IllegalStateException("unclosed tag `" + elementStack.peek().getName() + "' on close ");
        }
        newLine();
        out.close();
    }

    /** Returns the used print writer. */
    public final PrintWriter getPrintWriter() {
        return out;
    }

    /** Tests whether the tag <code>name</code> uses the element-only model.
   *
   * @see #XmlWriter(PrintWriter,String[])
   */
    public boolean isElementContent(final String name) {
        return elementContent.contains(name);
    }

    /** Opens a tag <code>tagName</code> with specified mixed content mode.
   *
   * @param tagName the tag name that should be opened
   * @param elementContent iff <code>true</code> this tag uses the element-only
   *   model and indentation will be done, otherwise mixed content is assumed.
   */
    public final void start(final String tagName, final boolean elementContent) {
        if (isOpenStartTag()) closeStartTag();
        newLine();
        out.print('<');
        out.print(tagName);
        elementStack.push(new Element(tagName, elementContent));
        openStartTag = true;
        doIndent = (indentLevel >= 0) && elementContent;
        if (doIndent) indentLevel++;
    }

    /** Opens a tag <code>tagName</code>.
   *
   * @see #start(String,boolean)
   * @see #isElementContent(String)
   */
    public final void start(final String tagName) {
        start(tagName, isElementContent(tagName));
    }

    /** Adds attribute <code>name</code> to the current open element.
   *
   * @throws IllegalStateException if already content has been added to the
   *   current element, like {@link #text(String) text} or sub-elements.
   */
    public final void attr(final String name, final String value) {
        if (!isOpenStartTag()) {
            throw new IllegalStateException("Can not add attribute if already content exists");
        }
        out.print(' ');
        out.print(name);
        if (value.indexOf('\'') == -1) {
            out.print("='");
            out.print(quoteString(value, "'&<\n\r\t"));
            out.print('\'');
        } else {
            out.print("=\"");
            out.print(quoteString(value, "\"&<\n\r\t"));
            out.print('"');
        }
    }

    /** Closes the current open tag.
   *
   * @throws IllegalStateException if no open tag exists.
   */
    public final void end() {
        if (elementStack.isEmpty()) throw new IllegalStateException("No open tag to close");
        final Element element = elementStack.pop();
        if (doIndent) indentLevel--;
        if (isOpenStartTag()) {
            openStartTag = false;
            out.print(" />");
        } else {
            if (element.isElementContent()) newLine();
            out.print("</");
            out.print(element.getName());
            out.print('>');
        }
        doIndent = (indentLevel >= 0);
        if (elementStack.size() > 0) doIndent = doIndent && elementStack.peek().isElementContent();
    }

    /** Closes the current open tag <code>tagName</code>.
   *
   * @throws IllegalStateException if no open tag exists or is not
   *   <code>tagName</code>
   * @see #end()
   */
    public final void end(final String tagName) {
        if (elementStack.isEmpty()) throw new IllegalStateException("No open tag to close");
        final String currentTag = elementStack.peek().getName();
        if (!currentTag.equals(tagName)) {
            throw new IllegalStateException("Expected to close tag `" + currentTag + "' instead of `" + tagName + "'");
        }
        end();
    }

    /** Adds a new line into the XML document. */
    public final void newLine() {
        if (isOpenStartTag()) closeStartTag();
        if (doIndent) {
            out.println();
            for (int i = 0; i < indentLevel; i++) out.print(INDENT);
        }
    }

    /** Writes text data to the current open element. */
    public final void text(final String text) {
        if (text == null) throw new IllegalArgumentException("text==null");
        if (isOpenStartTag()) closeStartTag();
        out.print(quoteString(text, "<&"));
    }

    /** Generates a CDATA section.
   *
   * @throws IllegalArgumentException if <code>cdata</code> is
   *   <code>null</code> or contains <code>]]&gt;</code>.
   */
    public final void cdata(final String cdata) {
        if (cdata == null) throw new IllegalArgumentException("cdata==null");
        if (cdata.indexOf("]]>") != -1) throw new IllegalArgumentException("CDATA section must not contain `]]>'");
        if (isOpenStartTag()) closeStartTag();
        out.print("<![CDATA[");
        out.print(cdata);
        out.print("]]>");
    }

    /** Generates a CDATA section from the given reader.
   *
   * <p><b>NOTE:</b> The stream is not checked for the forbidden character
   * sequence <code>]]&gt;</code>.</p>
   *
   * @throws IOException if an IO error occurs
   */
    public final void cdata(final Reader reader) throws IOException {
        final char[] buf = new char[4096];
        int readBytes;
        if (isOpenStartTag()) closeStartTag();
        out.print("<![CDATA[");
        do {
            readBytes = reader.read(buf);
            if (readBytes != -1) out.write(buf, 0, readBytes);
        } while (readBytes != -1);
        out.print("]]>");
    }

    private static char[] encodeHex(final byte[] data, final int start, final int len) {
        final int end = start + len;
        final char[] out = new char[len << 1];
        for (int i = start, j = 0; i < end; i++) {
            out[j++] = DIGITS[(data[i] & 0xF0) >>> 4];
            out[j++] = DIGITS[data[i] & 0x0F];
        }
        return out;
    }

    /** Generates a CDATA section from the given stream.
   *
   * <p>The data is encoded in hex which doubles the required size.</p>
   *
   * @throws IOException if an IO error occurs
   */
    public final void cdata(final InputStream in) throws IOException {
        final byte[] buf = new byte[2048];
        int readBytes;
        if (isOpenStartTag()) closeStartTag();
        out.print("<![CDATA[");
        do {
            readBytes = in.read(buf);
            if (readBytes != -1) out.write(encodeHex(buf, 0, readBytes));
        } while (readBytes != -1);
        out.print("]]>");
    }

    /** Generates a COMMENT section. */
    public final void comment(final String comment) {
        if (isOpenStartTag()) closeStartTag();
        out.print("<!-- ");
        out.print(comment.replaceAll("--", "- - "));
        out.print(" -->");
    }

    protected final boolean isOpenStartTag() {
        return openStartTag;
    }

    protected final void closeStartTag() {
        assert (openStartTag);
        out.print('>');
        openStartTag = false;
    }

    protected final String quoteString(final String s, final String quoteChars) {
        final int len = s.length();
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            if (quoteChars.indexOf(c) != -1 || c >= 'Ā') {
                switch(c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '\'':
                        sb.append("&apos;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    default:
                        sb.append("&#" + (int) c + ";");
                        break;
                }
            } else sb.append(c);
        }
        return sb.toString();
    }
}
