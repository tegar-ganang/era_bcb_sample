package org.allcolor.services.xml.utils;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 
 * DOCUMENT ME!
 * 
 * @author Quentin Anciaux
 * @version 0.1.0
 */
public final class XMLUtils {

    /** DOCUMENT ME! */
    private static final String entity[][] = new String[][] { { "&#0034;", "quot" }, { "&#0038;", "amp" }, { "&#0039;", "apos" }, { "&#0060;", "lt" }, { "&#0062;", "gt" }, { "&#0338;", "OElig" }, { "&#0339;", "oelig" }, { "&#0352;", "Scaron" }, { "&#0353;", "scaron" }, { "&#0376;", "Yuml" }, { "&#0710;", "circ" }, { "&#0732;", "tilde" }, { "&#8194;", "ensp" }, { "&#8195;", "emsp" }, { "&#8201;", "thinsp" }, { "&#8204;", "zwnj" }, { "&#8205;", "zwj" }, { "&#8206;", "lrm" }, { "&#8207;", "rlm" }, { "&#8211;", "ndash" }, { "&#8212;", "mdash" }, { "&#8216;", "lsquo" }, { "&#8217;", "rsquo" }, { "&#8218;", "sbquo" }, { "&#8220;", "ldquo" }, { "&#8221;", "rdquo" }, { "&#8222;", "bdquo" }, { "&#8224;", "dagger" }, { "&#8225;", "Dagger" }, { "&#8240;", "permil" }, { "&#8249;", "lsaquo" }, { "&#8250;", "rsaquo" }, { "&#8364;", "euro" }, { "&#0402;", "fnof" }, { "&#0913;", "Alpha" }, { "&#0914;", "Beta" }, { "&#0915;", "Gamma" }, { "&#0916;", "Delta" }, { "&#0917;", "Epsilon" }, { "&#0918;", "Zeta" }, { "&#0919;", "Eta" }, { "&#0920;", "Theta" }, { "&#0921;", "Iota" }, { "&#0922;", "Kappa" }, { "&#0923;", "Lambda" }, { "&#0924;", "Mu" }, { "&#0925;", "Nu" }, { "&#0926;", "Xi" }, { "&#0927;", "Omicron" }, { "&#0928;", "Pi" }, { "&#0929;", "Rho" }, { "&#0931;", "Sigma" }, { "&#0932;", "Tau" }, { "&#0933;", "Upsilon" }, { "&#0934;", "Phi" }, { "&#0935;", "Chi" }, { "&#0936;", "Psi" }, { "&#0937;", "Omega" }, { "&#0945;", "alpha" }, { "&#0946;", "beta" }, { "&#0947;", "gamma" }, { "&#0948;", "delta" }, { "&#0949;", "epsilon" }, { "&#0950;", "zeta" }, { "&#0951;", "eta" }, { "&#0952;", "theta" }, { "&#0953;", "iota" }, { "&#0954;", "kappa" }, { "&#0955;", "lambda" }, { "&#0956;", "mu" }, { "&#0957;", "nu" }, { "&#0958;", "xi" }, { "&#0959;", "omicron" }, { "&#0960;", "pi" }, { "&#0961;", "rho" }, { "&#0962;", "sigmaf" }, { "&#0963;", "sigma" }, { "&#0964;", "tau" }, { "&#0965;", "upsilon" }, { "&#0966;", "phi" }, { "&#0967;", "chi" }, { "&#0968;", "psi" }, { "&#0969;", "omega" }, { "&#0977;", "thetasym" }, { "&#0978;", "upsih" }, { "&#0982;", "piv" }, { "&#8226;", "bull" }, { "&#8230;", "hellip" }, { "&#8242;", "prime" }, { "&#8243;", "Prime" }, { "&#8254;", "oline" }, { "&#8260;", "frasl" }, { "&#8472;", "weierp" }, { "&#8465;", "image" }, { "&#8476;", "real" }, { "&#8482;", "trade" }, { "&#8501;", "alefsym" }, { "&#8592;", "larr" }, { "&#8593;", "uarr" }, { "&#8594;", "rarr" }, { "&#8595;", "darr" }, { "&#8596;", "harr" }, { "&#8629;", "crarr" }, { "&#8656;", "lArr" }, { "&#8657;", "uArr" }, { "&#8658;", "rArr" }, { "&#8659;", "dArr" }, { "&#8660;", "hArr" }, { "&#8704;", "forall" }, { "&#8706;", "part" }, { "&#8707;", "exist" }, { "&#8709;", "empty" }, { "&#8711;", "nabla" }, { "&#8712;", "isin" }, { "&#8713;", "notin" }, { "&#8715;", "ni" }, { "&#8719;", "prod" }, { "&#8721;", "sum" }, { "&#8722;", "minus" }, { "&#8727;", "lowast" }, { "&#8730;", "radic" }, { "&#8733;", "prop" }, { "&#8734;", "infin" }, { "&#8736;", "ang" }, { "&#8743;", "and" }, { "&#8744;", "or" }, { "&#8745;", "cap" }, { "&#8746;", "cup" }, { "&#8747;", "int" }, { "&#8756;", "there4" }, { "&#8764;", "sim" }, { "&#8773;", "cong" }, { "&#8776;", "asymp" }, { "&#8800;", "ne" }, { "&#8801;", "equiv" }, { "&#8804;", "le" }, { "&#8805;", "ge" }, { "&#8834;", "sub" }, { "&#8835;", "sup" }, { "&#8836;", "nsub" }, { "&#8838;", "sube" }, { "&#8839;", "supe" }, { "&#8853;", "oplus" }, { "&#8855;", "otimes" }, { "&#8869;", "perp" }, { "&#8901;", "sdot" }, { "&#8968;", "lceil" }, { "&#8969;", "rceil" }, { "&#8970;", "lfloor" }, { "&#8971;", "rfloor" }, { "&#9001;", "lang" }, { "&#9002;", "rang" }, { "&#9674;", "loz" }, { "&#9824;", "spades" }, { "&#9827;", "clubs" }, { "&#9829;", "hearts" }, { "&#9830;", "diams" }, { "&#0160;", "nbsp" }, { "&#0161;", "iexcl" }, { "&#0162;", "cent" }, { "&#0163;", "pound" }, { "&#0164;", "curren" }, { "&#0165;", "yen" }, { "&#0166;", "brvbar" }, { "&#0167;", "sect" }, { "&#0168;", "uml" }, { "&#0169;", "copy" }, { "&#0170;", "ordf" }, { "&#0171;", "laquo" }, { "&#0172;", "not" }, { "&#0173;", "shy" }, { "&#0174;", "reg" }, { "&#0175;", "macr" }, { "&#0176;", "deg" }, { "&#0177;", "plusmn" }, { "&#0178;", "sup2" }, { "&#0179;", "sup3" }, { "&#0180;", "acute" }, { "&#0181;", "micro" }, { "&#0182;", "para" }, { "&#0183;", "middot" }, { "&#0184;", "cedil" }, { "&#0185;", "sup1" }, { "&#0186;", "ordm" }, { "&#0187;", "raquo" }, { "&#0188;", "frac14" }, { "&#0189;", "frac12" }, { "&#0190;", "frac34" }, { "&#0191;", "iquest" }, { "&#0192;", "Agrave" }, { "&#0193;", "Aacute" }, { "&#0194;", "Acirc" }, { "&#0195;", "Atilde" }, { "&#0196;", "Auml" }, { "&#0197;", "Aring" }, { "&#0198;", "AElig" }, { "&#0199;", "Ccedil" }, { "&#0200;", "Egrave" }, { "&#0201;", "Eacute" }, { "&#0202;", "Ecirc" }, { "&#0203;", "Euml" }, { "&#0204;", "Igrave" }, { "&#0205;", "Iacute" }, { "&#0206;", "Icirc" }, { "&#0207;", "Iuml" }, { "&#0208;", "ETH" }, { "&#0209;", "Ntilde" }, { "&#0210;", "Ograve" }, { "&#0211;", "Oacute" }, { "&#0212;", "Ocirc" }, { "&#0213;", "Otilde" }, { "&#0214;", "Ouml" }, { "&#0215;", "times" }, { "&#0216;", "Oslash" }, { "&#0217;", "Ugrave" }, { "&#0218;", "Uacute" }, { "&#0219;", "Ucirc" }, { "&#0220;", "Uuml" }, { "&#0221;", "Yacute" }, { "&#0222;", "THORN" }, { "&#0223;", "szlig" }, { "&#0224;", "agrave" }, { "&#0225;", "aacute" }, { "&#0226;", "acirc" }, { "&#0227;", "atilde" }, { "&#0228;", "auml" }, { "&#0229;", "aring" }, { "&#0230;", "aelig" }, { "&#0231;", "ccedil" }, { "&#0232;", "egrave" }, { "&#0233;", "eacute" }, { "&#0234;", "ecirc" }, { "&#0235;", "euml" }, { "&#0236;", "igrave" }, { "&#0237;", "iacute" }, { "&#0238;", "icirc" }, { "&#0239;", "iuml" }, { "&#0240;", "eth" }, { "&#0241;", "ntilde" }, { "&#0242;", "ograve" }, { "&#0243;", "oacute" }, { "&#0244;", "ocirc" }, { "&#0245;", "otilde" }, { "&#0246;", "ouml" }, { "&#0247;", "divide" }, { "&#0248;", "oslash" }, { "&#0249;", "ugrave" }, { "&#0250;", "uacute" }, { "&#0251;", "ucirc" }, { "&#0252;", "uuml" }, { "&#0253;", "yacute" }, { "&#0254;", "thorn" }, { "&#0255;", "yuml" } };

    /** DOCUMENT ME! */
    private static XMLUtils handle = null;

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public static final synchronized XMLUtils getInstance() {
        return (XMLUtils.handle == null) ? (XMLUtils.handle = new XMLUtils()) : XMLUtils.handle;
    }

    /** DOCUMENT ME! */
    String entityDecl = null;

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 * 
	 * @throws UnsupportedEncodingException
	 *             DOCUMENT ME!
	 */
    public final String createEntityDeclaration() throws UnsupportedEncodingException {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<!DOCTYPE padawan [\n");
        for (final String element[] : XMLUtils.entity) {
            buffer.append("<!ENTITY " + element[1] + " '" + element[0] + "' >\n");
        }
        buffer.append("]>\n");
        return buffer.toString();
    }

    public final String escape(final String fin) {
        return this.escape(fin, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param fin
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    private final String escape(final String fin, boolean attr) {
        String in = fin != null ? fin.trim() : null;
        if (in == null || in.length() == 0) {
            return "";
        }
        if (attr) {
            if (in.indexOf('&') == -1 && in.indexOf('\"') == -1 && in.indexOf('<') == -1 && in.indexOf('>') == -1) {
                return in;
            }
        } else if (in.indexOf('&') == -1 && in.indexOf('<') == -1 && in.indexOf('>') == -1) {
            return in;
        }
        final StringBuilder result = new StringBuilder(in.length());
        final char chars[] = in.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&') {
                result.append("&amp;");
            } else if (chars[i] == '<') {
                result.append("&lt;");
            } else if (chars[i] == '>') {
                result.append("&gt;");
            } else if (attr && chars[i] == '\"') {
                result.append("&quot;");
            } else {
                result.append(chars[i]);
            }
        }
        in = result.toString();
        return in;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param fin
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String escapeAttribute(final String fin) {
        return this.escape(fin, true);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String getEntityDeclaration() {
        if (this.entityDecl == null) {
            try {
                this.entityDecl = this.createEntityDeclaration();
            } catch (final Exception e) {
                this.entityDecl = null;
            }
        }
        return this.entityDecl;
    }

    public final Node getRoot(final Node node, final String name, final String ns) {
        if ((node == null) || (name == null)) {
            return null;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if ((ns == null) && name.equals(node.getLocalName())) {
                return node;
            } else if (name.equals(node.getLocalName()) && ns.equals(node.getNamespaceURI())) {
                return node;
            }
            final NodeList nl = node.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                final Node root = this.getRoot(nl.item(i), name, ns);
                if (root != null) {
                    return root;
                }
            }
        }
        return null;
    }

    public final OMElement getRoot(final OMElement elem, final String name, final String ns) {
        if ((elem == null) || (name == null)) {
            return null;
        }
        if ((ns == null) && name.equals(elem.getLocalName())) {
            return elem;
        } else if (name.equals(elem.getLocalName()) && ns.equals(elem.getNamespace().getNamespaceURI())) {
            return elem;
        }
        for (@SuppressWarnings("unchecked") final Iterator<OMElement> it = (Iterator<OMElement>) elem.getChildElements(); it.hasNext(); ) {
            final OMElement root = this.getRoot(it.next(), name, ns);
            if (root != null) {
                return root;
            }
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param tagName
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    private final boolean isAlone(final String tagName, final String aloneTags[]) {
        if (aloneTags == null) {
            return false;
        }
        if (tagName == null) {
            return false;
        }
        for (final String element : aloneTags) {
            if (tagName.equalsIgnoreCase(element)) {
                return true;
            }
        }
        return false;
    }

    public String prettyPrint(final Node node) {
        final TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer;
        try {
            final StringWriter sw = new StringWriter();
            serializer = tfactory.newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            serializer.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (final TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public String prettyPrint(String xml) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            xml = xml.replaceAll("\r\n", "\n").replaceAll("\n\r", "\n");
            return this.prettyPrint(builder.parse(new InputSource(new StringReader(xml))));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param node
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 */
    private final void renderXMLRecurs(final PrintWriter out, final Node node, final String aloneTags[], boolean fullNodeName, boolean booIndent, String indent) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (booIndent) {
                if (!(node.getParentNode() == node.getOwnerDocument())) {
                    out.print("\n");
                }
                out.print(indent);
            }
            out.print("<");
            String nodeName = fullNodeName ? node.getNodeName() : node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
            if (!fullNodeName && nodeName.indexOf(':') != -1) {
                nodeName = nodeName.substring(nodeName.indexOf(':') + 1);
            }
            out.print(nodeName);
            boolean first = true;
            final NamedNodeMap nnm = ((Element) node).getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                final Attr attr = (Attr) nnm.item(i);
                if (!fullNodeName && ("xmlns".equals(attr.getPrefix()) || "xmlns".equals(attr.getLocalName()) || attr.getNodeName().startsWith("xmlns:"))) {
                    continue;
                }
                out.print(" ");
                if (booIndent && !first) {
                    out.print("\n");
                    out.print(indent);
                    for (int j = 0; j < nodeName.length() + 1; j++) {
                        out.print(" ");
                    }
                    out.print(" ");
                } else {
                    first = false;
                }
                String attrName = fullNodeName ? attr.getName() : attr.getLocalName() != null ? attr.getLocalName() : attr.getName();
                if (!fullNodeName && attrName.indexOf(':') != -1) {
                    attrName = attrName.substring(attrName.indexOf(':') + 1);
                }
                out.print(attrName);
                out.print("=\"");
                out.print(this.escapeAttribute(attr.getValue()));
                out.print("\"");
            }
            if (aloneTags != null && this.isAlone(nodeName, aloneTags)) {
                out.print("/>");
                return;
            }
            out.print(">");
            final NodeList nl = node.getChildNodes();
            final String nindent = booIndent ? indent + "  " : null;
            boolean lastIsElementNode = false;
            for (int i = 0; i < nl.getLength(); i++) {
                final Node child = nl.item(i);
                this.renderXMLRecurs(out, child, aloneTags, fullNodeName, booIndent, nindent);
                if (booIndent && !lastIsElementNode && child.getNodeType() == Node.ELEMENT_NODE) {
                    lastIsElementNode = true;
                }
            }
            if (lastIsElementNode) {
                out.print("\n");
                out.print(indent);
            }
            out.print("</");
            out.print(nodeName);
            out.print(">");
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            try {
                if (node.getNodeValue().trim().length() == 0) return;
                out.print(this.escape(node.getNodeValue()));
            } catch (final Exception ignore) {
                ;
            }
        } else if (node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
            try {
                out.print("&" + node.getNodeName() + ";");
            } catch (final Exception ignore) {
                ;
            }
        } else if (node.getNodeType() == Node.DOCUMENT_NODE && node != ((Document) node).getDocumentElement()) {
            this.renderXMLRecurs(out, ((Document) node).getDocumentElement(), aloneTags, fullNodeName, booIndent, indent);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param node
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 */
    private final void renderXMLRecurs(final PrintWriter out, final OMNode node, final String aloneTags[], final boolean rootNode) {
        if (node.getType() == OMNode.ELEMENT_NODE) {
            final OMElement elem = (OMElement) node;
            out.print("<");
            out.print(elem.getLocalName());
            if (rootNode) {
                out.print(" xmlns=\"" + elem.getNamespace().getNamespaceURI() + "\"");
            }
            @SuppressWarnings("unchecked") final Iterator nnm = elem.getAllAttributes();
            if (nnm.hasNext()) {
                out.println(" ");
            }
            for (; nnm.hasNext(); ) {
                final OMAttribute attr = (OMAttribute) nnm.next();
                if ("xmlns".equals(attr.getQName().getPrefix())) continue;
                if ("xmlns".equals(attr.getLocalName())) continue;
                out.print(attr.getLocalName());
                out.print("=\"");
                out.print(this.escapeAttribute(attr.getAttributeValue()));
                out.print("\" ");
            }
            if (this.isAlone(elem.getLocalName(), aloneTags)) {
                out.print("/>");
                return;
            }
            out.print(">");
            @SuppressWarnings("unchecked") final Iterator nl = elem.getChildren();
            for (; nl.hasNext(); ) {
                final OMNode child = (OMNode) nl.next();
                this.renderXMLRecurs(out, child, aloneTags, false);
            }
            out.print("</");
            out.print(elem.getLocalName());
            out.print(">");
        } else if (node.getType() == OMNode.TEXT_NODE) {
            try {
                out.print(this.escape(((OMText) node).getText()));
            } catch (final Exception ignore) {
                ;
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final Node xmlDoc, final String aloneTags[], final boolean fullNodeName) {
        return this.rewriteXml(xmlDoc, aloneTags, fullNodeName, false);
    }

    public final String rewriteXml(final Node xmlDoc, final String aloneTags[], final boolean fullNodeName, final boolean booIndent) {
        return this.rewriteXml(xmlDoc, aloneTags, false, fullNodeName, booIndent);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final Node xmlDoc, final String aloneTags[], final boolean outputXmlHeader, final boolean fullNodeName, final boolean booIndent) {
        try {
            final StringWriter sOut = new StringWriter();
            this.rewriteXml(sOut, xmlDoc, aloneTags, outputXmlHeader, fullNodeName, booIndent);
            return sOut.toString();
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final OMNode xmlDoc, final String aloneTags[]) {
        return this.rewriteXml(xmlDoc, aloneTags, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final OMNode xmlDoc, final String aloneTags[], final boolean outputXmlHeader) {
        try {
            final StringWriter sOut = new StringWriter();
            this.rewriteXml(sOut, xmlDoc, aloneTags, outputXmlHeader);
            return sOut.toString();
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlFile
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final String xmlFile, final String aloneTags[]) {
        return this.rewriteXml(xmlFile, aloneTags, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param xmlFile
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 * 
	 * @return DOCUMENT ME!
	 */
    public final String rewriteXml(final String xmlFile, final String aloneTags[], final boolean outputXmlHeader) {
        try {
            final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            final Document xmlDoc = fact.newDocumentBuilder().parse(new InputSource(new StringReader(xmlFile)));
            return this.rewriteXml(xmlDoc, aloneTags, outputXmlHeader);
        } catch (final Exception e) {
            e.printStackTrace();
            return xmlFile;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final Node xmlDoc, final String aloneTags[], final boolean fullNodeName) {
        this.rewriteXml(out, xmlDoc, aloneTags, false, fullNodeName, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final Node xmlDoc, final String aloneTags[], final boolean outputXmlHeader, final boolean fullNodeName, final boolean booIndent) {
        try {
            if (outputXmlHeader) {
                out.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
            }
            this.renderXMLRecurs(new PrintWriter(out), xmlDoc, aloneTags, fullNodeName, booIndent, "");
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final OMNode xmlDoc, final String aloneTags[]) {
        this.rewriteXml(out, xmlDoc, aloneTags, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlDoc
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final OMNode xmlDoc, final String aloneTags[], final boolean outputXmlHeader) {
        try {
            if (outputXmlHeader) {
                out.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
            }
            this.renderXMLRecurs(new PrintWriter(out), xmlDoc, aloneTags, true);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlFile
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final String xmlFile, final String aloneTags[]) {
        this.rewriteXml(out, xmlFile, aloneTags, false);
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param out
	 *            DOCUMENT ME!
	 * @param xmlFile
	 *            DOCUMENT ME!
	 * @param aloneTags
	 *            DOCUMENT ME!
	 * @param outputXmlHeader
	 *            DOCUMENT ME!
	 */
    public final void rewriteXml(final Writer out, final String xmlFile, final String aloneTags[], final boolean outputXmlHeader) {
        try {
            final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            final Document xmlDoc = fact.newDocumentBuilder().parse(new InputSource(new StringReader(xmlFile)));
            this.rewriteXml(out, xmlDoc, aloneTags, outputXmlHeader);
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public String rewriteXmlForXfire(final XMLStreamReader reader, final String rootTagName, final String xmlns) throws XMLStreamException {
        final StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append(rootTagName);
        builder.append(" xmlns=\"");
        builder.append(xmlns);
        builder.append("\">");
        for (int event = reader.getEventType(); event != XMLStreamConstants.END_DOCUMENT; event = reader.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (reader.getLocalName().equals("Envelope") && reader.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/envelope/")) {
                    continue;
                }
                if (reader.getLocalName().equals("Body") && reader.getNamespaceURI().equals("http://schemas.xmlsoap.org/soap/envelope/")) {
                    continue;
                }
                if (reader.getLocalName().equals(rootTagName)) {
                    continue;
                }
                builder.append("<");
                builder.append(reader.getLocalName());
                builder.append(">");
            }
            if (event == XMLStreamConstants.CHARACTERS) {
                builder.append(this.escape(reader.getText().trim()));
            }
            if (event == XMLStreamConstants.END_ELEMENT) {
                builder.append("</");
                builder.append(reader.getLocalName());
                builder.append(">");
                if (reader.getLocalName().equals(rootTagName)) {
                    break;
                }
            }
        }
        reader.close();
        return builder.toString();
    }
}
