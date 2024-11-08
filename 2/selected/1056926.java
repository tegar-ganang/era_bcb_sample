package com.potix.idom.input;

import java.util.Stack;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.XMLReader;
import com.potix.lang.D;
import com.potix.lang.Objects;
import com.potix.util.logging.Log;
import com.potix.util.resource.Locators;
import com.potix.idom.util.SimpleLocator;
import com.potix.idom.*;

/**
 * The SAX Hanlder.
 * It implements ContentHandler, LexicalHandler and DeclHandler.
 * It is the caller's job to set up this handler properly if required.
 *
 * <p>This class doesn't depend on SAXBuilder, so it can be used in any
 * other place, e.g., javax.xml.transform.sax.SAXResult.
 *
 * @author <a href="mailto:tomyeh@potix.com">Tom M. Yeh</a>
 * @see SAXBuilder
 */
public class SAXHandler extends DefaultHandler implements LexicalHandler, DeclHandler {

    private static final Log log = Log.lookup(SAXHandler.class);

    /** The iDOM factory. */
    protected IDOMFactory _factory;

    /** Whether to ignore ignorable whitespace */
    private boolean _ignoreWhitespaces = false;

    /** Whether expansion of entities should occur */
    private boolean _expandEntities = true;

    /** Whether to convert CDATA to Text and coalesce them. */
    private boolean _coalescing = false;

    /** Whether to ignore comments. */
    private boolean _ignoreComments = false;

    /** The error handler. */
    private ErrorHandler _errHandler = null;

    /** The entity resolver. */
    private EntityResolver _resolver = null;

    /** The Document being created. */
    protected Document _doc = null;

    /** Locator. */
    protected Locator _loc = null;

    /** Indicator of whether we are in a DTD. */
    protected boolean _inDTD = false;

    /** Indicator of whether we are in a CDATA. */
    protected boolean _inCData = false;

    /** The Group stack. The top one is the current group being processed. */
    protected Stack _stack = null;

    /** The namespaces in between startPrefixMapping and endPrefixMapping. */
    protected List _declNamespaces = null;

    /** Temporary holder for the internal subset. */
    private StringBuffer _internSubset = null;

    /** Whether it is in internal subset. */
    private boolean _inInternSubset = false;

    /**
	 * Constructor.
	 *
	 * @param factory the iDOM factory; null for DefaultIDOMFactory.
	 */
    public SAXHandler(IDOMFactory factory) {
        _factory = factory != null ? factory : new DefaultIDOMFactory();
    }

    /**
	 * Constructor.
	 */
    public SAXHandler() {
        _factory = new DefaultIDOMFactory();
    }

    /**
	 * Tests whether to ignore whitespaces in element content.
	 */
    public final boolean isIgnoringElementContentWhitespace() {
        return _ignoreWhitespaces;
    }

    /**
	 * Sets whether the parser should elminate whitespace in 
	 * element content. They are known as "ignorable whitespace". 
	 * Only whitespace which is contained within element content that has
	 * an element only content model will be eliminated (see XML Rec 2.10).
	 *
	 * <p>For this setting to take effect requires that validation be turned on.
	 *
	 * <p>Default: false.
	 *
	 * @param ignore Whether to ignore whitespaces in element content.
	 */
    public final void setIgnoringElementContentWhitespace(boolean ignore) {
        _ignoreWhitespaces = ignore;
    }

    /**
	 * Tests whether to expand entity reference nodes.
	 */
    public final boolean isExpandEntityReferences() {
        return _expandEntities;
    }

    /**
	 * Sets whether to expand entities during parsing.
	 * A true means to expand entities as normal content.  A false means to
	 * leave entities unexpanded as <code>EntityReference</code> objects.
	 *
	 * <p>Default: true.
	 *
	 * @param expand whether entity expansion should occur.
	 */
    public final void setExpandEntityReferences(boolean expand) {
        _expandEntities = expand;
    }

    /**
	 * Indicates whether or not the factory is configured to produce parsers
	 * which converts CDATA to Text and appends it to the adjacent (if any)
	 * Text node.
	 *
	 * <p>Default: false.
	 *
	 * @return true if the factory is configured to produce parsers which
	 * converts CDATA nodes to Text nodes
	 * and appends it to the adjacent (if any) Text node; false otherwise.
	 */
    public final boolean isCoalescing() {
        return _coalescing;
    }

    /**
	 * Specifies that the parser produced by this code will convert
	 * CDATA to Text and append it to the adjacent (if any) text.
	 *
	 * <p>Default: false.
	 */
    public final void setCoalescing(boolean coalescing) {
        _coalescing = coalescing;
    }

    /**
	 * Indicates whether or not the factory is configured to produce parsers
	 * which ignores comments.
	 *
	 * <p>Default: false.
	 *
	 * @return true if the factory is configured to produce parsers
	 * which ignores comments; false otherwise.
	 */
    public final boolean isIgnoringComments() {
        return _ignoreComments;
    }

    /**
	 * Specifies that the parser produced by this code will ignore comments.
	 *
	 * <p>Default: false.
	 */
    public final void setIgnoringComments(boolean ignoreComments) {
        _ignoreComments = ignoreComments;
    }

    /**
	 * Specifies the org.xml.sax.ErrorHandler to be used to report errors
	 * present in the XML document to be parsed.
	 * <p>Default: null -- to use the default imple-mentation and behavior.
	 */
    public final void setErrorHandler(ErrorHandler eh) {
        _errHandler = eh;
    }

    /**
	 * Gets the org.xml.sax.ErrorHandler.
	 *
	 * @return the error handler; null to use the default implementation
	 */
    public final ErrorHandler getErrorHandler() {
        return _errHandler;
    }

    /**
	 * Specifies the org.xml.sax.EntityResolver to be used to resolve
	 * entities present in the XML docu-ment to be parsed.
	 * <p>Default: null -- to use the default implementation and behavior.
	 */
    public final void setEntityResolver(org.xml.sax.EntityResolver er) {
        _resolver = er;
    }

    /**
	 * Gets the org.xml.sax.EntityResolver.
	 *
	 * @return the enity resolverr; null to use the default implementation
	 */
    public final EntityResolver getEntityResolver() {
        return _resolver;
    }

    /**
	 * Gets the document being created.
	 * Called to retrieve the iDOM tree after parsed.
	 */
    public final Document getDocument() {
        return _doc;
    }

    /**
	 * Gets the iDOM factory. Null for DefaultIDOMFactory.THE.
	 */
    public final IDOMFactory getIDOMFactory() {
        return _factory;
    }

    /**
	 * Sets the iDOM factory. Null for DefaultIDOMFactory.THE.
	 */
    public final void setIDOMFactory(IDOMFactory factory) {
        _factory = factory;
    }

    /**
	 * Attaches the locator to the item.
	 */
    protected final void attachLocator(Item vtx) {
        if (_loc != null) vtx.setLocator(new SimpleLocator(_loc));
    }

    /** Returns the top group, or null if not available.
	 */
    protected final Group getTopGroup() {
        return _stack.isEmpty() ? null : (Group) _stack.peek();
    }

    /**
	 * Adds the item to the current group; also attach the locator.
	 */
    protected final void addToCurrentGroup(Item vtx) {
        attachLocator(vtx);
        ((Group) _stack.peek()).getChildren().add(vtx);
    }

    /**
	 * Adds a new group to the current group as a child,
	 * and pushes the new group to be the new current group.
	 */
    protected final void pushGroup(Group group) {
        if (_stack.isEmpty()) assert (group instanceof Document); else addToCurrentGroup(group);
        _stack.push(group);
    }

    /**
	 * Pops out the current group, and the one under it becomes the
	 * new current group.
	 */
    protected final void popGroup() {
        ((Group) _stack.pop()).coalesce(false);
    }

    public void externalEntityDecl(String name, String pubId, String sysId) throws SAXException {
        if (D.ON && log.finerable()) log.finer("externalEntityDecl: " + name + " p:" + pubId + " s:" + sysId);
        if (!_inInternSubset) return;
        _internSubset.append("  <!ENTITY ").append(name);
        if (pubId != null) _internSubset.append(" PUBLIC \"").append(pubId).append("\" ");
        if (sysId != null) _internSubset.append(" SYSTEM \"").append(sysId).append("\" ");
        _internSubset.append(">\n");
    }

    public void internalEntityDecl(String name, String value) throws SAXException {
        if (D.ON && log.finerable()) log.finer("internalEntityDecl: " + name + '=' + value);
        if (!_inInternSubset) return;
        _internSubset.append("  <!ENTITY ").append(name).append(" \"").append(value).append("\">\n");
    }

    public void attributeDecl(String eName, String aName, String type, String valueDefault, String value) throws SAXException {
        if (!_inInternSubset) return;
        _internSubset.append("  <!ATTLIST ").append(eName).append(' ').append(aName).append(' ').append(type).append(' ');
        if (valueDefault != null) {
            _internSubset.append(valueDefault);
        } else {
            _internSubset.append('"').append(value).append('"');
        }
        if ((valueDefault != null) && (valueDefault.equals("#FIXED"))) {
            _internSubset.append(" \"").append(value).append('"');
        }
        _internSubset.append(">\n");
    }

    public void elementDecl(String name, String model) throws SAXException {
        if (!_inInternSubset) return;
        _internSubset.append("  <!ELEMENT ").append(name).append(' ').append(model).append(">\n");
    }

    public void startDTD(String name, String pubId, String sysId) throws SAXException {
        if (D.ON && log.finerable()) log.finer("start DTD: " + name + " p:" + pubId + " s:" + sysId);
        addToCurrentGroup(_factory.newDocType(name, pubId, sysId));
        _inDTD = true;
        _internSubset = new StringBuffer();
        _inInternSubset = true;
    }

    public void endDTD() throws SAXException {
        if (D.ON && log.finerable()) log.finer("end DTD: \"" + _internSubset + '"');
        _doc.getDocType().setInternalSubset(_internSubset.toString());
        _inDTD = false;
        _internSubset = null;
        _inInternSubset = false;
    }

    public void startEntity(String name) throws SAXException {
        if (D.ON && log.finerable()) log.finer("startEntity: " + name);
        if (name.equals("[dtd]")) {
            _inInternSubset = false;
            return;
        }
        if (!isExpandEntityReferences() && !_inDTD && !entityToSkip(name)) {
            pushGroup(_factory.newEntityRef(name));
        }
    }

    /** Tests whether the name is something that always expanded. */
    private boolean entityToSkip(String name) {
        switch(name.charAt(0)) {
            case 'a':
                return name.equals("amp") || name.equals("apos");
            case 'g':
                return name.equals("gt");
            case 'l':
                return name.equals("lt");
            case 'q':
                return name.equals("quot");
        }
        return false;
    }

    public void endEntity(String name) throws SAXException {
        if (D.ON && log.finerable()) log.finer("endEntity: " + name);
        if (name.equals("[dtd]")) {
            _inInternSubset = false;
            return;
        }
        if (!isExpandEntityReferences() && !_inDTD && !entityToSkip(name)) {
            popGroup();
        }
    }

    public void startCDATA() throws SAXException {
        _inCData = true;
    }

    public void endCDATA() throws SAXException {
        _inCData = false;
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        if (length == 0 || isIgnoringComments()) return;
        String data = new String(ch, start, length);
        if (_inDTD && _inInternSubset && !isExpandEntityReferences()) {
            _internSubset.append("  <!--").append(data).append("-->\n");
        }
        if (!_inDTD && data.length() != 0) {
            addToCurrentGroup(_factory.newComment(data));
        }
    }

    public void startDocument() throws SAXException {
        _declNamespaces = new LinkedList();
        _stack = new Stack();
        _doc = _factory.newDocument(null, null);
        pushGroup(_doc);
    }

    public void endDocument() throws SAXException {
        popGroup();
        assert (_stack.isEmpty());
        _stack = null;
        _loc = null;
        _declNamespaces = null;
    }

    public void setDocumentLocator(Locator locator) {
        _loc = locator;
    }

    public void processingInstruction(String target, String data) throws SAXException {
        addToCurrentGroup(_factory.newProcessingInstruction(target, data));
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (D.ON && log.finerable()) log.finer("start prefix: " + prefix + ", " + uri + SimpleLocator.toString(_loc));
        final Namespace ns = Namespace.getSpecial(prefix);
        if (ns == null || !ns.getURI().equals(uri)) _declNamespaces.add(0, new Namespace(prefix, uri));
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (D.ON && log.finerable()) log.finer("end prefix: " + prefix + SimpleLocator.toString(_loc));
        for (Iterator itr = _declNamespaces.iterator(); itr.hasNext(); ) {
            final Namespace ns = (Namespace) itr.next();
            if (prefix.equals(ns.getPrefix())) {
                itr.remove();
                break;
            }
        }
    }

    public void startElement(String nsURI, String lname, String tname, Attributes attrs) throws SAXException {
        if (D.ON && log.finerable()) log.finer("start element: nsURI=\"" + nsURI + "\", lname=" + lname + ", tname=" + tname + " attr#=" + attrs.getLength() + SimpleLocator.toString(_loc));
        if (tname == null || tname.length() == 0) tname = lname;
        final Element element = newElement(nsURI, tname);
        for (int j = 0, len = attrs.getLength(); j < len; j++) {
            String attQname = attrs.getQName(j);
            String attLname = attrs.getLocalName(j);
            if (attQname == null || attQname.length() == 0) attQname = attLname;
            final Attribute attr;
            int kp = attQname.indexOf(':');
            if (kp >= 0) {
                final String prefix = attQname.substring(0, kp);
                final Namespace ns = element.getNamespace(prefix);
                if (ns == null) throw new SAXException("Unknown prefix: " + prefix + " at " + _loc + "\nDo you forget to turn on namespace-aware");
                attr = _factory.newAttribute(ns, attQname.substring(kp + 1), attrs.getValue(j));
            } else {
                attr = _factory.newAttribute(attLname == null || attLname.length() == 0 ? attQname : attLname, attrs.getValue(j));
            }
            attachLocator(attr);
            element.getAttributeItems().add(attr);
        }
    }

    private Element newElement(String nsURI, String tname) throws SAXException {
        if (nsURI == null) nsURI = "";
        final int j = tname.indexOf(':');
        final String prefix = j >= 0 ? tname.substring(0, j) : "";
        final String lname = j >= 0 ? tname.substring(j + 1) : tname;
        Namespace ns;
        final Group parent = getTopGroup();
        if (parent instanceof Element) {
            ns = ((Element) parent).getNamespace(prefix);
            if (ns != null && !ns.getURI().equals(nsURI)) ns = null;
        } else {
            ns = Namespace.getSpecial(prefix);
        }
        if (ns == null) {
            if (_declNamespaces.size() > 0) {
                for (Iterator it = _declNamespaces.iterator(); it.hasNext(); ) {
                    final Namespace n = (Namespace) it.next();
                    if (n.getPrefix().equals(prefix) && n.getURI().equals(nsURI)) {
                        if (D.ON && log.finerable()) log.finer("Namespace found in _declNamespaces: " + n);
                        ns = n;
                        break;
                    }
                }
            }
            if (ns == null && nsURI.length() > 0) {
                if (D.ON && log.finerable()) log.finer("Create namespace: " + prefix + " " + nsURI);
                ns = new Namespace(prefix, nsURI);
            }
        }
        final Element element = ns != null ? _factory.newElement(ns, lname) : _factory.newElement(lname);
        if (_declNamespaces.size() > 0) {
            for (Iterator it = _declNamespaces.iterator(); it.hasNext(); ) element.addDeclaredNamespace((Namespace) it.next());
            _declNamespaces.clear();
        }
        pushGroup(element);
        return element;
    }

    public void endElement(String nsURI, String lname, String tname) throws SAXException {
        if (D.ON && log.finerable()) log.finer("end element: " + nsURI + ", " + lname + ", " + tname);
        popGroup();
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (length == 0) return;
        final String data = new String(ch, start, length);
        if (getTopGroup() instanceof Document) {
            if (data.trim().length() > 0) throw new SAXException("Adding non-empty text to Document: " + data);
            return;
        }
        if (_inCData && !isCoalescing()) addToCurrentGroup(_factory.newCData(data)); else addToCurrentGroup(_factory.newText(data));
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (length == 0 || isIgnoringElementContentWhitespace()) return;
        addToCurrentGroup(_factory.newText(new String(ch, start, length)));
    }

    public void notationDecl(String name, String publicID, String systemID) throws SAXException {
        if (!_inInternSubset) return;
        _internSubset.append("  <!NOTATION ").append(name).append(" \"").append(systemID).append("\">\n");
    }

    public void unparsedEntityDecl(String name, String pubId, String sysId, String notationName) throws SAXException {
        if (D.ON && log.finerable()) log.finer("externalEntityDecl: " + name + " p:" + pubId + " s:" + sysId + " n:" + notationName);
        if (!_inInternSubset) return;
        _internSubset.append("  <!ENTITY ").append(name);
        if (pubId != null) _internSubset.append(" PUBLIC \"").append(pubId).append("\" ");
        if (sysId != null) _internSubset.append(" SYSTEM \"").append(sysId).append("\" ");
        _internSubset.append(" NDATA ").append(notationName).append(">\n");
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        if (D.ON && log.finerable()) log.finer("resolveEntity public=" + publicId + " system=" + systemId);
        EntityResolver er = getEntityResolver();
        if (er != null) {
            try {
                return er.resolveEntity(publicId, systemId);
            } catch (IOException ex) {
                throw new SAXException(ex);
            }
        } else {
            InputSource is = defaultResolveEntity(publicId, systemId);
            if (D.ON && is == null && log.finerable()) log.finer("Unable to resolve public=" + publicId + " system=" + systemId);
            return is;
        }
    }

    /** The default entity resolver.
	 * It is used if {@link #setEntityResolver} is not called.
	 * This implementation searches the class path under /metainfo/xml.
	 *
	 * <p>For example, http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd
	 * is asked.
	 * It searches from classpath
	 * for /metainfo/xml/java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd
	 * and /metainfo/xml/portlet-app_1_0.xsd
	 */
    protected InputSource defaultResolveEntity(String publicId, String systemId) throws SAXException {
        if (systemId == null) return null;
        if (systemId.indexOf("file:/") >= 0) {
            try {
                final InputSource is = new InputSource(new URL(systemId).openStream());
                is.setSystemId(systemId);
                if (D.ON && log.finerable()) log.finer("Entity found " + systemId);
                return is;
            } catch (Exception ex) {
                if (D.ON && log.finerable()) log.finer("Unable to open " + systemId);
            }
        }
        final String PREFIX = "/metainfo/xml";
        final com.potix.util.resource.Locator loader = Locators.getDefault();
        URL url = null;
        int j = systemId.indexOf("://");
        if (j > 0) {
            final String resId = PREFIX + systemId.substring(j + 2);
            url = loader.getResource(resId);
        }
        if (url == null) {
            j = systemId.lastIndexOf('/');
            final String resId = j >= 0 ? PREFIX + systemId.substring(j) : PREFIX + '/' + systemId;
            url = loader.getResource(resId);
        }
        if (url != null) {
            if (D.ON && log.finerable()) log.finer("Entity resovled to " + url);
            try {
                final InputSource is = new InputSource(url.openStream());
                is.setSystemId(url.toExternalForm());
                return is;
            } catch (IOException ex) {
                throw new SAXException(ex);
            }
        }
        return null;
    }

    public void warning(SAXParseException ex) throws SAXException {
        ErrorHandler eh = getErrorHandler();
        if (eh != null) {
            eh.warning(ex);
        } else {
            log.warning(ex.getMessage() + SimpleLocator.toString(_loc));
        }
    }

    public void error(SAXParseException ex) throws SAXException {
        ErrorHandler eh = getErrorHandler();
        if (eh != null) {
            eh.error(ex);
        } else {
            log.error(ex.getMessage() + SimpleLocator.toString(_loc));
            throw ex;
        }
    }

    public void fatalError(SAXParseException ex) throws SAXException {
        ErrorHandler eh = getErrorHandler();
        if (eh != null) {
            eh.fatalError(ex);
        } else {
            log.error(ex.getMessage() + SimpleLocator.toString(_loc));
            throw ex;
        }
    }
}
