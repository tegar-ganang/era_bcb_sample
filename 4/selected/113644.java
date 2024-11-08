package net.noderunner.exml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * A very fast incremental, streaming XML pull-parser.  The XML
 * tree is not validated or checked for well-formedness.  However, this
 * allows an XML tree to be read in without creating any unnecessary
 * objects.  This implementation aims to be the most minimal (fastest)
 * and reasonably complete, so as to not create any reduceable overhead
 * or user inconvience.
 * <p>
 * In this implementation, it relies on the 'fatter'
 * <code>XmlReader</code> to do element tree parsing.  This allows both
 * simple and efficient skipping as well as being able to build object
 * trees.  Typically, <code>XmlReader</code> is not used except for its
 * hopefully significant convience.
 * <p>
 * Example usage:
 * <pre>
 * String document = 
 * 	"&lt;?xml version='1.0'?&gt;\n" +
 *	"&lt;doc&gt;\n" +
 *		"&lt;empty1 attr='value1'/&gt;\n" +
 *		"&lt;empty2 attr='value2'/&gt;\n" +
 *		"&lt;para&gt;Hello world!&lt;/para&gt;\n" +
 *	"&lt;/doc&gt;\n";
 * XmlSParser parser = new XmlSParser();
 * parser.setReadString(document);
 * parser.next();        // returns XmlEvent.STAG for 'doc' tag
 * parser.next();        // returns XmlEvent.STAG for empty1 tag
 * Element e = parser.startTag();
 * e.getAttValue("attr");// returns 'value1' attribute value from empty1
 * parser.next();        // returns XmlEvent.ETAG for empty1 tag
 * parser.next();        // returns XmlEvent.STAG for empty2 tag
 * parser.getAttValue(); // returns 'value2' attribute value from empty2
 * parser.next();        // returns XmlEvent.ETAG for empty2 tag
 * parser.next();        // returns XmlEvent.STAG for para tag
 * parser.next();        // returns XmlEvent.CHARDATA for text
 * String text = parser.getText(); // returns 'Hello world!'
 * parser.next();        // returns XmlEvent.ETAG for para tag
 * String etag = parser.getTagName(); // returns 'etag'
 * parser.next();        // returns XmlEvent.ETAG for 'doc' tag
 * parser.next();        // returns XmlEvent.EOD
 * </pre>
 * <p>
 * Additionally, this parser may be used to read document fragments,
 * specifically strings without surronding tags.  It is suggested
 * that EOD notifcation be turned off.  For example:
 * <pre>
 * XmlSParser parser = new XmlSParser(new XmlReader());
 * parser.setEvents(XmlEvent.CHARDATA | XmlEvent.STAG | XmlEvent.ETAG);
 * parser.setReadString("A string &lt;i&gt;with&lt;/i&gt; mark-up!");
 * parser.next();        // returns XmlEvent.CHARDATA for 'a string '
 * parser.next();        // returns XmlEvent.STAG for the 'i' tag
 * parser.next();        // returns XmlEvent.CHARDATA for 'with'
 * String text = parser.getText(); // returns 'with'
 * parser.next();        // returns XmlEvent.ETAG for the 'i' tag
 * parser.next();        // returns XmlEvent.CHARDATA for 'mark-up!'
 * </pre>
 * </p>
 *
 * @author Elias Ross
 * @version 1.0
 */
public class XmlSParser implements XmlEvent {

    /**
	 * Underlying reader.
	 */
    private XmlReader reader;

    /**
	 * Underlying scanner.
	 */
    private XmlScanner scanner;

    /**
	 * Keeps track of parsing depth.
	 */
    private int depth;

    /**
	 * If whitespace should be skipped from after start tags, otherwise,
	 * whitespace is read.
	 */
    private boolean skipWS;

    /**
	 * If all following text events should belong to the previous
	 * sequential text event so the text:
	 * <pre>x &amp;lt; &lt;![CDATA[ y]]&gt;</pre> 
	 * will be treated as one CHARDATA event, not
	 * <code>CHARDATA-REFERENCE-CHARDATA-CDSECT</code> event.
	 */
    private boolean skipRT;

    /**
	 * Events that will be returned from <code>next</code>.
	 */
    private int events;

    /**
	 * The event last returned by <code>next</code>.
	 */
    private int curEvent;

    /**
	 * Whether or not the end tag really exists.  This is
	 * to make consistant the next() routinue in how it handles
	 * both types of empty tags.
	 */
    private boolean fakeETAG;

    /**
	 * Whether or not to return the last peek event during the next
	 * <code>next</code> call.
	 * @see #setPeekNext
	 */
    private boolean returnLast;

    /**
	 * Reused to read text as a String.
	 */
    private XmlCharArrayWriter caw;

    /**
	 * Constructs an XmlSParser, using a default <code>XmlReader</code>.
	 * By default, the following events are returned:
	 * <ul>
	 * <li>{@link XmlEvent#STAG}</li>
	 * <li>{@link XmlEvent#ETAG}</li>
	 * <li>{@link XmlEvent#TEXT}</li>
	 * <li>{@link XmlEvent#EOD}</li>
	 * </ul>
	 * @param reader xml reader to wrap
	 * @see #setEvents
	 */
    public XmlSParser() {
        this(new XmlReader());
    }

    /**
	 * Constructs an XmlSParser around an XmlReader.  By default,
	 * the following events are returned:
	 * <ul>
	 * <li>{@link XmlEvent#STAG}</li>
	 * <li>{@link XmlEvent#ETAG}</li>
	 * <li>{@link XmlEvent#TEXT}</li>
	 * <li>{@link XmlEvent#EOD}</li>
	 * </ul>
	 * @param reader xml reader to wrap
	 * @see #setEvents
	 */
    public XmlSParser(XmlReader reader) {
        this(reader, STAG | ETAG | TEXT | EOD);
    }

    /**
	 * Constructs an XmlSParser using a read string.
   */
    public XmlSParser(String readString) {
        this();
        setReadString(readString);
    }

    /**
	 * Constructs an XmlSParser around an XmlReader with specific events
	 * to trap.
	 * @param reader xml reader to wrap
	 * @see #setEvents
	 */
    public XmlSParser(XmlReader reader, int events) {
        this.reader = reader;
        this.scanner = reader.getScanner();
        this.skipWS = true;
        this.skipRT = true;
        this.caw = new XmlCharArrayWriter();
        setEvents(events);
        initState();
    }

    /**
	 * Resets this object to parse using another <code>Reader</code> source.
	 * The parse depth is reset to zero. 
	 */
    public void setReader(Reader reader) {
        this.reader.setReader(reader);
        initState();
    }

    /**
	 * Resets this object to parse a <code>String</code> source.
	 * The parse depth is reset to zero. 
	 */
    public void setReadString(String xml) {
        scanner.setReadString(xml);
        initState();
    }

    /**
	 * Returns the parser to its initial state.
	 */
    private void initState() {
        this.depth = 0;
        this.curEvent = XmlEvent.NONE;
        this.fakeETAG = false;
        this.returnLast = false;
    }

    /**
	 * Specifies events to watch.
	 * The following one or more events can be specified by OR'ing them
	 * together:
	 * <ul>
	 * <li>{@link XmlEvent#ALL_EVENTS}</li>
	 * <li>{@link XmlEvent#STAG}</li>
	 * <li>{@link XmlEvent#ETAG}</li>
	 * <li>{@link XmlEvent#TEXT}</li>
	 * <li>{@link XmlEvent#CHARDATA}</li>
	 * <li>{@link XmlEvent#REFERENCE}</li>
	 * <li>{@link XmlEvent#CDSECT}</li>
	 * <li>{@link XmlEvent#COMMENT}</li>
	 * <li>{@link XmlEvent#DOCTYPE_DECL}</li>
	 * <li>{@link XmlEvent#XML_DECL}</li>
	 * <li>{@link XmlEvent#PI}</li>
	 * <li>{@link XmlEvent#COMMENT}</li>
	 * <li>{@link XmlEvent#EOD}</li>
	 * <li>{@link XmlEvent#ELEMENT_DECL}</li>
	 * <li>{@link XmlEvent#ATTLIST_DECL}</li>
	 * <li>{@link XmlEvent#ENTITY_DECL}</li>
	 * <li>{@link XmlEvent#CONDITIONAL_SECT}</li>
	 * <li>{@link XmlEvent#NOTATATION_DECL}</li>
	 * </ul>
	 * Note that <code>TEXT</code> composes the <code>CHARDATA</code>,
	 * <code>CDSECT</code>, and <code>REFERENCE</code> flags.  It does
	 * not make much sense to ignore character references and only
	 * accept unescaped text, but that is up to the discretion of the
	 * user.
	 * <p>
	 * It is possible to want to ignore all events, in which case this
	 * parser will continually read XML documents until error or EOF is
	 * reached.
	 * </p>
	 * <p>
	 * Example: (Very quickly check a XML document for basic well-formedness)
	 * <pre>
	 * Reader r = ...;
	 * XmlSParser parser = new XmlSParser(new XmlReader(r));
	 * parser.setEvents(XmlEvent.EOD);
	 * boolean wellFormed = true;
	 * try {
	 * 	parser.next();
	 * } catch (XmlException e) {
	 * 	wellFormed = false;
	 * }
	 * </pre>
	 * </p>
	 * <p>
	 * Example: (Extract all comments from an XML document)
	 * <pre>
	 * Reader r = ...;
	 * XmlSParser parser = new XmlSParser(new XmlReader(r));
	 * parser.setEvents(XmlEvent.COMMENT | XmlEvent.EOD);
	 * while (parser.next() != XmlEvent.EOD) {
	 *	if (parser.isEvent(XmlEvent.COMMENT))
	 * 		Comment c = parser.getComment();
	 * }
	 * </pre>
	 * </p>
	 * @param events OR'd together event flags
	 * @see XmlEvent
	 */
    public void setEvents(int events) {
        this.events = events;
    }

    /**
	 * Returns true if end of document event {@link XmlEvent#EOD} hasn't
	 * been reached.  Note:  It is still possible to read from the
	 * stream and read another document if desired.
	 */
    public boolean hasNext() {
        return !isEvent(EOD);
    }

    /**
	 * A convience method which tests for a particular event.
	 * This is less error-prone than testing with bit flags in code.
	 */
    public boolean isEvent(int event) {
        return (curEvent == event) || (curEvent & event) != 0;
    }

    /**
	 * Skips a CDSect or conditional section.
	 */
    private void skipCDSect() throws IOException, XmlException {
        int c = 0, c2 = 0;
        scanner.skip(XmlTags.CDATA_BEGIN.length);
        do {
            scanner.skipUntil(']');
            c2 = c;
            c = scanner.read();
        } while (c2 != ']' || c != ']' || scanner.peek() != '>');
        scanner.read();
    }

    /**
	 * Skips a comment section.
	 */
    private void skipComment() throws IOException, XmlException {
        int c = 0;
        scanner.skip(4);
        while (true) {
            scanner.skipUntil('-');
            scanner.read();
            c = scanner.peek();
            if (c == '-') {
                scanner.read();
                c = scanner.peek();
                if (c == '>') break; else throw new XmlException("-- not allowed in comments");
            }
        }
        scanner.read();
    }

    /**
	 * Returns the next event.  The stream is advanced such that
	 * the last returned element is skipped.
	 *
	 * @throws XmlException if bad XML data is found or an unknown
	 * state is reached
	 */
    public int next() throws IOException, XmlException {
        return next(this.events);
    }

    /**
	 * Returns the next event matching the event(s) specified.
	 *
	 * @param events OR'd together event flags
	 * @throws XmlException if bad XML data is found or an unknown
	 * state is reached
	 * @see #setEvents
	 */
    public int next(int events) throws IOException, XmlException {
        while (true) {
            if (returnLast) {
                returnLast = false;
                if (want(events, curEvent)) return curEvent;
            }
            switch(curEvent) {
                case NONE:
                    if (reader.XmlDecl()) {
                        curEvent = XML_DECL;
                    } else {
                        peekEvent();
                    }
                    break;
                case STAG:
                    int c = 0;
                    depth++;
                    do {
                        c = scanner.skipUntil('>', '/');
                        scanner.read();
                    } while (c == '/' && scanner.peek() != '>');
                    if (c == '/') {
                        curEvent = ETAG;
                        fakeETAG = true;
                        scanner.read();
                    } else {
                        peekEvent();
                    }
                    break;
                case ETAG:
                    depth--;
                    if (fakeETAG) {
                        fakeETAG = false;
                        peekEvent();
                    } else {
                        scanner.skipUntil('>');
                        scanner.read();
                        if (depth == 0) curEvent = EOD; else peekEvent();
                    }
                    break;
                case REFERENCE:
                    scanner.skipUntil(';');
                    scanner.read();
                    peekEvent();
                    if (skipRT && isEvent(TEXT)) continue;
                    break;
                case CHARDATA:
                    scanner.skipUntil('<', skipRT ? (char) 0 : '&');
                    peekEvent();
                    if (skipRT && isEvent(TEXT)) continue;
                    break;
                case CDSECT:
                    skipCDSect();
                    peekEvent();
                    if (skipRT && isEvent(TEXT)) continue;
                    break;
                case COMMENT:
                    skipComment();
                    peekEvent();
                    break;
                case PI:
                    do {
                        scanner.skipUntil('?');
                        scanner.read();
                    } while (scanner.peek() != '>');
                    scanner.read();
                    peekEvent();
                    break;
                case XML_DECL:
                    peekEvent();
                    break;
                case EOD:
                    curEvent = NONE;
                    break;
                case DOCTYPE_DECL:
                    reader.doctypedecl();
                    peekEvent();
                    break;
                case ELEMENT_DECL:
                case ATTLIST_DECL:
                case ENTITY_DECL:
                case NOTATATION_DECL:
                    scanner.skipUntil('>');
                    scanner.read();
                    peekEvent();
                    break;
                case CONDITIONAL_SECT:
                    reader.conditionalSect();
                    peekEvent();
                    break;
                default:
                    throw new XmlException("Unable to process event: " + eventToString(curEvent));
            }
            if (want(events, curEvent)) {
                return curEvent;
            }
        }
    }

    /**
	 * Creates an <code>Element</code> with data about the current start
	 * tag token under the cursor.  Returns null if no tag was found.
	 *
	 * @see XmlReader#STag()
	 */
    public Element startTag() throws IOException, XmlException {
        depth++;
        Element e = reader.STag();
        if (e == null) return null;
        if (!e.isOpen()) {
            returnLast = true;
            curEvent = ETAG;
            fakeETAG = true;
            if (skipWS) reader.S();
        } else {
            setPeekNext();
        }
        return e;
    }

    /**
	 * Creates an <code>Element</code> with data about the current end
	 * tag token under the cursor.  If the prior element was an empty tag,
	 * this function will not work as expected, as this only works for open
	 * tags.
	 * Returns null if no end tag was found.
	 * @see XmlReader#ETag()
	 */
    public Element endTag() throws IOException, XmlException {
        Element e = reader.ETag();
        if (e != null) depth--;
        setPeekNext();
        return e;
    }

    private void checkTagName() throws IOException, XmlException {
        if (curEvent != ETAG && curEvent != STAG) throw new IllegalXmlParserStateException("Must be on ETAG or STAG");
        scanner.read();
        if (curEvent == ETAG) scanner.read();
    }

    /**
	 * Returns the name of the element (start or end tag)
	 * under the cursor.  Typically, this call will create no
	 * Java objects at all, as all strings are re-used from the local
	 * string pool.
	 * <p>
	 * Example:  (Prints <code>"Got foo!"</code> to standard out.)
	 * <pre>
	 * final String FOO = "foo";
	 *
	 * XmlReader xr = new XmlReader(r);
	 * xr.getStringPool().add(FOO);
	 * XmlSParser p = new XmlSParser(xr);
	 * p.setReadString("&lt;foo&gt;&lt;/foo&gt;");
	 *
	 * p.next();       // returns XmlEvent.STAG
	 * if (p.getTagName() == FOO)
	 * 	System.out.println("Got foo!");
	 * p.next();       // returns XmlEvent.ETAG;
	 * p.getTagName(); // returns 'foo'
	 * </pre>
	 * </p>
	 */
    public String getTagName() throws IOException, XmlException {
        checkTagName();
        return scanner.getName();
    }

    /**
	 * Reads the name and tag of the element (start or end tag)
	 * under the cursor into a {@link NamespaceImpl} instance.  Typically,
	 * this call will create no Java objects at all, as all
	 * strings are re-used from the local string pool.
	 * <p>
	 * Example:
	 * <pre>
	 * XmlSParser p = new XmlSParser();
	 * p.setReadString("&lt;ns:foo&gt;&lt;/ns:foo&gt;");
	 * NamespaceImpl ns = new NamespaceImpl();
	 * p.next();          // returns XmlEvent.STAG
	 * p.readTagName(ns); // sets prefix and localname
	 * p.next();          // returns XmlEvent.ETAG;
	 * p.readTagName(ns); // sets prefix and localname
	 * </pre>
	 * </p>
	 */
    public void readTagName(NamespaceImpl ns) throws IOException, XmlException {
        checkTagName();
        scanner.readNamespace(ns);
    }

    private boolean checkAttName() throws IOException, XmlException {
        int c = scanner.peek();
        if (c == '=') {
            scanner.read();
            reader.S();
            c = scanner.read();
            scanner.skipUntil((char) c);
            scanner.read();
            c = scanner.peek();
        }
        if (c == '>') {
            return false;
        }
        if (c == '<') {
            scanner.read();
            reader.Name();
        }
        boolean space = reader.S();
        if (space) c = scanner.peek();
        if (c == '/' || c == '>') {
            return false;
        }
        if (!space) throw new XmlException("Expected whitespace after name or attribute");
        return true;
    }

    /**
	 * Returns an element's attribute name or <code>null</code> if no
	 * more attributes exist.
	 * <p>
	 * Example:
	 * <pre>
	 * XmlSParser p = new XmlSParser(new XmlReader(r));
	 * p.setReadString("&lt;foo version='1.0' ref = 'bar'&gt;&lt;/foo&gt;");
	 * p.next() == XmlEvent.STAG);
	 * p.getAttName();   // returns "version"
	 * p.getAttName();   // returns "ref"
	 * p.getAttName();   // returns null
	 * </pre>
	 * </p>
	 * @see #getAttValue
	 */
    public String getAttName() throws IOException, XmlException {
        if (curEvent != STAG) throw new IllegalXmlParserStateException("Must be on STAG");
        if (!checkAttName()) return null;
        String name = reader.Name();
        reader.S();
        return name;
    }

    /**
	 * Returns an element's attribute name or <code>null</code> if no
	 * more attributes exist.
	 * <p>
	 * Example:
	 * <pre>
	 * XmlSParser p = new XmlSParser();
	 * p.setReadString("&lt;foo ns:version='1.0' ns:ref = 'bar'&gt;&lt;/foo&gt;");
	 * NamespaceImpl ns = new NamespaceImpl();
	 * p.next();         // returns XmlEvent.STAG
	 * p.getAttName(ns); // returns "version" localname
	 * p.getAttName(ns); // returns "ref" localname
	 * p.getAttName(ns); // returns a clear NamespaceImpl
	 * </pre>
	 * </p>
	 * @see NamespaceImpl#isClear
	 */
    public void readAttName(NamespaceImpl ns) throws IOException, XmlException {
        if (!checkAttName()) {
            ns.clear();
        } else {
            scanner.readNamespace(ns);
            reader.S();
        }
    }

    /**
	 * Retrives an element's attribute values.  Note that this
	 * advances the input stream past the beginning of the start tag,
	 * such that {@link #startTag}, {@link #getElementTree} will
	 * no longer correctly for the current tag.  This is much
	 * more efficient than a <code>startTag</code> call.
	 * Returns <code>dflt</code> as a default value if no more
	 * attributes were found.
	 * <p>
	 * Example:
	 * <pre>
	 * XmlSParser p = new XmlSParser();
	 * p.setReadString("&lt;foo version='1.0' ref='bar'&gt;&lt;/foo&gt;");
	 * p.next()               // returns XmlEvent.STAG
	 * p.getAttValue();       // returns '1.0'
	 * p.getAttValue();       // returns 'bar'
	 * p.getAttValue("okay"); // returns 'okay'
	 * p.next();              // returns XmlEvent.ETAG
	 * </pre>
	 * </p>
	 *
	 * @param dflt either <code>null</code> or a default value to
	 * return
	 * @see #getAttName
	 */
    public String getAttValue(String dflt) throws IOException, XmlException {
        if (curEvent != STAG) throw new IllegalXmlParserStateException("Must be on STAG");
        int c = scanner.peek();
        while (c != '=' && c != '/' && c != '>' && c != -1) {
            scanner.read();
            c = scanner.peek();
        }
        if (c != '=') {
            return dflt;
        }
        scanner.skipUntil('"', '\'');
        String v = reader.AttValue();
        return v;
    }

    /**
	 * This method calls <code>getAttValue(null)</code>.
	 * @see #getAttValue(String)
	 */
    public String getAttValue() throws IOException, XmlException {
        return getAttValue(null);
    }

    /**
	 * Returns true if the tag data or text data at the current parse
	 * position matches the given character array.  In either case of
	 * failure or success, the read position remains the same.
	 * This works with any sort of tag.
	 * <p>
	 * Example usage:
	 * <pre>
	 * static final char[] FOO = "foo".toCharArray();
	 * ...
	 * XmlSParser p = new XmlSParser(new XmlReader(), ALL_EVENTS);
	 * p.setReadString("&lt;foo&gt;&lt;!--fool--&gt;");
	 * p.next();
	 * if (p.matches(FOO))
	 *	System.out.println("foo tag found!");
	 * p.next();
	 * if (p.matches(FOO))
	 *	System.out.println("foo comment found!");
	 * </pre>
	 * </p>
	 * 
	 * @param a characters to find
	 * @return true if matches
	 */
    public boolean matches(final char a[]) throws IOException, XmlException {
        int offs = 0;
        switch(curEvent) {
            case CHARDATA:
                break;
            case STAG:
                offs = 1;
                break;
            case ETAG:
                offs = XmlTags.ETAG_BEGIN.length;
                break;
            case REFERENCE:
                offs = 1;
                break;
            case COMMENT:
                offs = XmlTags.COMMENT_BEGIN.length;
                break;
            case CDSECT:
                offs = XmlTags.CDATA_BEGIN.length;
                break;
            case XML_DECL:
                offs = XmlTags.PI_BEGIN.length;
                break;
            case PI:
                offs = XmlTags.PI_BEGIN.length;
                break;
            case DOCTYPE_DECL:
                offs = XmlTags.DOCTYPE_BEGIN.length;
                break;
            case ELEMENT_DECL:
                offs = XmlTags.ELEMENT_DECL_BEGIN.length;
                break;
            case ATTLIST_DECL:
                offs = XmlTags.ATTLIST_DECL_BEGIN.length;
                break;
            case ENTITY_DECL:
                offs = XmlTags.ENTITY_DECL_BEGIN.length;
                break;
            case CONDITIONAL_SECT:
                offs = XmlTags.CONDITIONAL_BEGIN.length;
                break;
            case NOTATATION_DECL:
                offs = XmlTags.NOTATION_DECL_BEGIN.length;
                break;
            case PE_REFERENCE:
                offs = 1;
                break;
            case EOD:
                break;
        }
        int num = scanner.peek(reader.cbuf, 0, a.length + offs);
        if (num < a.length) return false;
        for (int i = 0; i < a.length; i++) if (reader.cbuf[i + offs] != a[i]) {
            return false;
        }
        return true;
    }

    /**
	 * Indicates that <code>next</code> should return this
	 * event, rather than acting upon it and skipping it.
	 */
    private void setPeekNext() throws IOException, XmlException {
        peekEvent();
        returnLast = true;
    }

    /**
	 * Creates an <code>Element</code> with data about the current token
	 * under the cursor, with its content.  Do not expect an <code>ETAG</code>
	 * event to occur once this element is read.
	 * Returns null if no element at this location.
	 */
    public Element getElementTree() throws IOException, XmlException {
        Element e = reader.element();
        setPeekNext();
        return e;
    }

    /**
	 * Adds content to an <code>Element</code> until an end tag
	 * is reached.  This does not read in the element's end tag.
	 * @param e element's end tag to reach
	 */
    public void getContent(Element e) throws IOException, XmlException {
        reader.content(e);
        setPeekNext();
    }

    /**
	 * Returns a processing instruction at the parse location.
	 * Returns <code>null</code> if no processing instruction was
	 * found.
	 */
    public PI getPI() throws IOException, XmlException {
        PI pi = reader.pi(false);
        setPeekNext();
        return pi;
    }

    /**
	 * Returns a comment at the parse location.
	 * Returns <code>null</code> if no comment was found.
	 */
    public Comment getComment() throws IOException, XmlException {
        Comment c = reader.comment(false);
        setPeekNext();
        return c;
    }

    /**
	 * Returns text at the parse location as a new <code>String</code>
	 * instance.
	 * If we are located at a start tag, returns the text immediately following
	 * this tag, until another tag is reached.
	 */
    public String getText() throws IOException, XmlException {
        caw.reset();
        copyText(caw);
        String s = caw.toString();
        if (caw.size() > 1024) caw = new XmlCharArrayWriter();
        return s;
    }

    /**
	 * Copies text at the parse location to a <code>Writer</code>.
	 * Text is copied from the underlying stream until a non-text entity
	 * is reached, such as an end tag or comment.  If a non-built-in
	 * reference is found, it is ignored.
	 * If we are located at a start tag, returns the text immediately following
	 * this tag, until another tag is reached.
	 * <p>
	 * Use a <code>StringWriter</code> or <code>XmlCharArrayWriter</code>
	 * instance to collect text data in a memory buffer.
	 * </p>
	 * @see XmlCharArrayWriter
	 */
    public void copyText(Writer w) throws IOException, XmlException {
        if (curEvent == XmlEvent.STAG || curEvent == XmlEvent.ETAG) {
            next(XmlEvent.ALL_EVENTS);
            curEvent = scanner.peekEvent();
        }
        while (true) {
            switch(curEvent) {
                case XmlEvent.CHARDATA:
                    reader.CharData(w);
                    break;
                case XmlEvent.CDSECT:
                    reader.CDSect(w);
                    break;
                case XmlEvent.REFERENCE:
                    Entity entity = reader.Reference();
                    if (entity == null) w.write((char) scanner.read()); else w.write(entity.resolveAll(reader));
                    break;
                default:
                    returnLast = true;
                    return;
            }
            curEvent = scanner.peekEvent();
        }
    }

    /**
	 * Skips an start tag, writing to <code>w</code>.
	 * Returns true if it read an open tag.
	 */
    private boolean copyStartTag(Writer w) throws IOException, XmlException {
        int c;
        do {
            c = scanner.copyUntil(w, '>', '/');
            w.write(c);
            scanner.read();
        } while (c == '/' && scanner.peek() != '>');
        if (c != '/') {
            return true;
        }
        w.write(scanner.read());
        return false;
    }

    /**
	 * Copies all content at the parse location to a <code>Writer</code>.
	 * Content must be copied at a start tag.
	 * Text is copied from the underlying stream until the matching
	 * closing tag is read.  If the tag is an closed tag, for example 
	 * <code>&lt;tag /&gt;</code>, no data is written, and per general
	 * convention <code>ETAG</code> will be returned.
	 * <p>
	 * Example:
	 * <pre>
	 * XmlSParser parser = new XmlSParser(new XmlReader());
	 * parser.setReadString("&lt;doc&gt;Some &lt;i&gt;content&lt;/i&gt; here.&lt;/doc&gt;");
	 * parser.next();           // returns XmlEvent.STAG for the 'doc' tag
	 * XmlCharArrayWriter caw = new XmlCharArrayWriter();
	 * parser.copyContent(caw); // writes 'Some &lt;i&gt;content&lt;/i&gt; here.'
	 * parser.next();           // returns XmlEvent.ETAG
	 * </pre>
	 * </p>
	 * <p>
	 * Example 2:
	 * <pre>
	 * XmlSParser parser = new XmlSParser(new XmlReader());
	 * XmlCharArrayWriter caw = new XmlCharArrayWriter();
	 * parser.setReadString("&lt;foo/&gt;  &lt;bar/&gt;");
	 * parser.next();           // returns XmlEvent.STAG for the 'foo' tag
	 * parser.copyContent(caw); // writes nothing
	 * parser.next();           // returns XmlEvent.ETAG
	 * // whitespace is skipped ...
	 * parser.next();           // returns XmlEvent.STAG for the 'bar' tag
	 * </pre>
	 * </p>
	 * <p>
	 * @see XmlCharArrayWriter
	 */
    public void copyContent(Writer w) throws IOException, XmlException {
        if (curEvent != STAG) throw new IllegalXmlParserStateException("Must be on STAG");
        depth++;
        int depth2 = depth;
        do {
            if (depth > depth2) {
                int c = scanner.copyUntil(w, '<');
                if (c != '<') throw new XmlException("EOF in copyContent");
                curEvent = scanner.peekEvent();
            }
            switch(curEvent) {
                case ETAG:
                    depth--;
                    fakeETAG = false;
                    if (depth > depth2) {
                        scanner.copyUntil(w, '>');
                        w.write((char) scanner.read());
                    }
                    break;
                case STAG:
                    Writer w2 = (depth > depth2) ? w : NullWriter.getInstance();
                    boolean open = copyStartTag(w2);
                    if (open) {
                        depth++;
                    } else {
                        curEvent = ETAG;
                        fakeETAG = true;
                    }
                    break;
                case COMMENT:
                    reader.copyUntil(w, XmlTags.COMMENT_END);
                    w.write(XmlTags.COMMENT_END);
                    break;
                case CDSECT:
                    w.write(XmlTags.CDATA_BEGIN);
                    reader.CDSect(w);
                    w.write(XmlTags.CDATA_END);
                    break;
                default:
                    w.write(scanner.read());
                    break;
            }
        } while (depth > depth2);
        if (fakeETAG && skipWS) {
            reader.S();
        }
        returnLast = true;
    }

    /**
	 * Returns a text string, which, once found,
	 * is internalized using the underlying string pool.
	 * This is useful for short text segments which are often repeated
	 * within XML tags.  Note that long text segments may cause much worse
	 * performance than expected, as this routine is optimized to handle
	 * only data within the current buffer.  Character and built-in
	 * parameter references will not cause the text reading to stop,
	 * meaning even strings with &amp; in them will be made canonical.
	 * Reading stops at EOF or before a &lt; character.
	 * <p>
	 * This routine behaves almost exactly the same as {@link #getText},
	 * with the following notes:
	 * <ul>
	 * <li>String object creation is saved; the same string is
	 * not created twice.</li>
	 * <li>Non-built-in references (see {@link Entity#resolveAll}) are
	 * <strong>not</strong> substituted.
	 * </li>
	 * <li>{@link #getText} performs better for non-canonical text segments,
	 * since a string pool reference is then unnecessarily created.
	 * </li>
	 * </p>
	 * <p>
	 * In the following example <code>foo1</code> and <code>foo2</code>
	 * are assigned to the exact same String object:
	 * <pre>
	 * XmlSParser xs = new XmlSParser(new XmlReader(), XmlEvent.CHARDATA);
	 * xs.setReadString("<a><b>Fo&apos;o</b><b>Fo'o</b></a>");
	 * xs.next();
	 * String foo1 = xs.getCanonicalText();
	 * xs.next();
	 * String foo2 = xs.getCanonicalText();
	 * System.out.println(foo1 == foo2);
	 * </pre>
	 * This code performs slightly worse:  (object creation not avoided)
	 * <pre>
	 * XmlReader xr = new XmlReader();
	 * StringPool sp = xr.getStringPool();
	 * XmlSParser xs = new XmlSParser(xr, XmlEvent.CHARDATA);
	 * xs.setReadString("<a><b>Fo&apos;o</b><b>Fo'o</b></a>");
	 * xs.next();
	 * String foo1 = sp.intern(xs.getText());
	 * xs.next();
	 * String foo2 = sp.intern(xs.getText());
	 * System.out.println(foo1 == foo2);
	 * </p>
	 * @see StringPool
	 */
    public String getCanonicalText() throws IOException, XmlException {
        return scanner.getCanonicalText();
    }

    /**
	 * Returns true if interested in this event.
	 */
    private static boolean want(int events, int event) {
        return (events & event) != 0;
    }

    /**
	 * Determines the next event, also skips over whitespace if
	 * set as such.  Sets curEvent.
	 */
    private void peekEvent() throws IOException, XmlException {
        if (skipWS) reader.S();
        curEvent = scanner.peekEvent();
        if (curEvent == NONE) {
            throw new XmlException("Unknown or invalid token read: " + scanner);
        }
    }

    /**
	 * Sets whether or not to preserve whitespace.  This is to eliminate
	 * the frustration of getting {@link XmlEvent#CHARDATA CHARDATA} events
	 * for whitespace.  However, this means that initial whitespace for
	 * elements is not preserved.  Note that whitespace following a
	 * <code>CHARDATA</code> event is not trimmed by the
	 * {@link #getText} methods.
	 * <p>
	 * Currently, this has no effect on the behavior of the 
	 * {@link #getContent} and {@link #getElementTree}
	 * methods.  Create a {@link Dtd} and supply it to the
	 * {@link XmlReader} if whitespace preservation is
	 * not important for creating object trees.
	 * </p>
	 */
    public void setSkipWS(boolean skipWS) {
        this.skipWS = skipWS;
    }

    /**
	 * Sets if repeated text events should be ignored.  The parser
	 * can return multiple events when a reference, text block,
	 * or character data section is read, but this is usually
	 * surperfluous information.  However, upon encountering non-text
	 * data within a text block a separate event is generated.
	 * <p>
	 * By default, this is set true.  This means that the text:
	 * <pre>x &amp;lt; &lt;![CDATA[ y]]&gt;</pre> 
	 * will be treated as one CHARDATA event, not as a string of
	 * four different text events.
	 * </p>
	 */
    public void setSkipRepeatTextEvents(boolean skip) {
        this.skipRT = skip;
    }

    /**
	 * Closes the underlying stream.
	 * Calling any more methods on this object will likely result in an
	 * <code>IOException</code>.
	 */
    public void close() throws IOException {
        reader.close();
    }

    /**
	 * Returns the parse tree depth of this tree.  The depth is incremented
	 * once <code>next</code> is called following a <code>STAG</code> event,
	 * and decremented once <code>next</code> is called following an
	 * <code>ETAG</code> event.
	 * @return an integer, representing the number of open element tags
	 * encountered so far that have not been ended
	 */
    public int getDepth() {
        return depth;
    }

    /**
	 * Moves the stream up to the depth given.
	 * If <code>depth</code> exceeds the current level, this method has
	 * no effect.  If <code>depth</code> is zero, this reads until the
	 * end of the document.  This is useful for reading in the remaining
	 * content of an entire XML document that is no longer important.
	 * Negative values are allowed.
	 *
	 * @see #close
	 * @see #getDepth
	 */
    public void up(int depth) throws IOException, XmlException {
        while (this.depth > depth) {
            next();
        }
    }

    /**
	 * Returns a string representation of this object for debugging.
	 */
    @Override
    public String toString() {
        String s = "XmlSParser [reader=" + reader + " event=" + eventToString(curEvent) + " depth=" + depth + "]";
        return s;
    }

    /**
	 * Converts an event to a debug string.  This does not
	 * function for OR'd together flags, except <code>TEXT</code>, since
	 * only one string is returned.
	 * <p>
	 * Example use: <code>XmlSParser.eventToString(XmlEvent.STAG)</code>
	 * </p>
	 *
	 * @see XmlEvent
	 */
    public static String eventToString(int event) {
        switch(event) {
            case NONE:
                return "NONE";
            case TEXT:
                return "TEXT";
            case ALL_EVENTS:
                return "ALL_EVENTS";
            case STAG:
                return "STAG";
            case ETAG:
                return "ETAG";
            case CHARDATA:
                return "CHARDATA";
            case CDSECT:
                return "CDSECT";
            case REFERENCE:
                return "REFERENCE";
            case DOCTYPE_DECL:
                return "DOCTYPE_DECL";
            case XML_DECL:
                return "XML_DECL";
            case COMMENT:
                return "COMMENT";
            case PI:
                return "PI";
            case ELEMENT_DECL:
                return "ELEMENT_DECL";
            case ATTLIST_DECL:
                return "ATTLIST_DECL";
            case ENTITY_DECL:
                return "ENTITY_DECL";
            case CONDITIONAL_SECT:
                return "CONDITIONAL_SECT";
            case NOTATATION_DECL:
                return "NOTATATION_DECL";
            case EOD:
                return "EOD";
            default:
                return "EVENT_" + event;
        }
    }
}
