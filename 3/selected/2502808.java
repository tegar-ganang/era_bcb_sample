package de.schlund.pfixcore.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import de.schlund.pfixxml.IncludeDocumentFactory;
import de.schlund.pfixxml.config.GlobalConfigurator;
import de.schlund.pfixxml.resources.Resource;
import de.schlund.pfixxml.resources.ResourceUtil;
import de.schlund.pfixxml.targets.AuxDependency;
import de.schlund.pfixxml.targets.AuxDependencyInclude;
import de.schlund.pfixxml.targets.DependencyType;
import de.schlund.pfixxml.targets.TargetGenerator;
import de.schlund.pfixxml.util.XPath;
import de.schlund.pfixxml.util.Xml;

/**
 * DumpText.java
 *
 * @author <a href="mailto:jtl@schlund.de">Jens Lautenbacher</a>
 * @version 1.0
 */
public class DumpText implements IDumpText {

    private static final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

    static {
        dbfac.setNamespaceAware(true);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java de.schlund.pfixcore.util.DumpText DOCROOT DEPEND.XML <CLASSNAME>");
            System.err.println("       This will create a dump of all include parts");
            System.err.println("       that are used by the project that belongs to the");
            System.err.println("       given DEPEND.XML file. Note that the DEPEND.XML file");
            System.err.println("       must be given as a relative file name to DOCROOT.");
            System.err.println("       CLASSNAME is an optional class that implements the IDumpText interface that");
            System.err.println("       should be used instead of de.schlund.pfixcore.util.DumpText for processing");
            System.exit(0);
        }
        String docroot = args[0];
        String depend = args[1];
        GlobalConfigurator.setDocroot(docroot);
        IDumpText trans;
        if (args.length == 3) {
            Class<?> clazz = Class.forName(args[2]);
            trans = (IDumpText) clazz.newInstance();
        } else {
            trans = new DumpText();
        }
        Logging.configure("generator_quiet.xml");
        trans.generateList(depend);
    }

    /**
     * <code>generateList</code> iterates over all includes. It will dump all the include parts
     * that are used in the referenced project (which is identified by the given depend.xml file)
     * into the output file "dump.xml".
     *
     * @param depend a <code>String</code> referencing the depend.xml file to process
     * @exception Exception if an error occurs
     */
    public void generateList(String depend) throws Exception {
        Document list = dbfac.newDocumentBuilder().newDocument();
        Element root = list.createElement("dumpedincludeparts");
        root.setAttribute("xmlns:pfx", "http://www.schlund.de/pustefix/core");
        root.setAttribute("xmlns:ixsl", "http://www.w3.org/1999/XSL/Transform");
        root.setAttribute("dependfile", depend);
        addRootNodeAtributes(root);
        list.appendChild(root);
        root.appendChild(list.createTextNode("\n"));
        TargetGenerator gen = new TargetGenerator(ResourceUtil.getFileResourceFromDocroot(depend));
        TreeSet<AuxDependency> incs = gen.getTargetDependencyRelation().getProjectDependenciesForType(DependencyType.TEXT);
        for (Iterator<AuxDependency> i = incs.iterator(); i.hasNext(); ) {
            AuxDependencyInclude aux = (AuxDependencyInclude) i.next();
            if (includePartOK(aux)) {
                Resource file = aux.getPath();
                if (file.exists()) {
                    System.out.print(".");
                    handleInclude(root, aux, gen);
                }
            }
        }
        root.appendChild(list.createTextNode("\n"));
        Xml.serialize(list, "dump.xml", false, true);
        System.out.print("\n");
    }

    /**
     * Overwrite this method to add more attributes to the root node (the dumpincludeparts tag). 
     * Typically used for adding more xmlns:xxx attributes.
     * @param root
     */
    public void addRootNodeAtributes(Element root) {
    }

    /**
     * The method <code>includePartOK</code> can be overridden in derived classes to
     * select based on the include part given as input if this part should be dumped or not.
     * The default implementation just returns true. 
     * @param aux - The AuxDependencyInclude to check
     * @return true, if the include part should be dunped, false if otherwise
     */
    public boolean includePartOK(AuxDependencyInclude aux) {
        return true;
    }

    /**
     * The method <code>retrieveTheme</code> can be overridden to decide which theme
     * to use in the declaration of the dumped content (this is important so you can 
     * change the theme from the current one to another for the import process).
     * The default implementation returns aux.getTheme().  
     * @param aux
     * @return the String to be used in the theme attribute of the dumped 
     */
    public String retrieveTheme(AuxDependencyInclude aux) {
        return aux.getTheme();
    }

    private void handleInclude(Element root, AuxDependencyInclude aux, TargetGenerator generator) throws Exception {
        Resource path = aux.getPath();
        String part = aux.getPart();
        String theme = aux.getTheme();
        Document doc = root.getOwnerDocument();
        IncludeDocumentFactory incfac = new IncludeDocumentFactory(generator.getCacheFactory());
        Document incdoc = incfac.getIncludeDocument(null, path, true).getDocument();
        Node extpart = XPath.selectNode(incdoc, "/include_parts/part[@name = '" + part + "']");
        if (extpart != null) {
            Element partelem = doc.createElement("USEDINCLUDE");
            partelem.setAttribute("PART", part);
            partelem.setAttribute("PATH", path.toURI().toString());
            root.appendChild(doc.createTextNode("\n"));
            root.appendChild(doc.createTextNode("  "));
            root.appendChild(partelem);
            NodeList nl = extpart.getChildNodes();
            Element themenode = null;
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i) instanceof Element) {
                    Element elem = (Element) nl.item(i);
                    if (elem.getNodeName().equals("theme")) {
                        if (elem.getAttribute("name").equals(theme)) {
                            themenode = elem;
                            break;
                        }
                    }
                }
            }
            if (themenode != null) {
                String check = md5ForNode(themenode);
                partelem.setAttribute("CHECK", check);
                partelem.setAttribute("THEME", retrieveTheme(aux));
                NodeList nlist = themenode.getChildNodes();
                for (int i = 0; i < nlist.getLength(); i++) {
                    Node node = (Node) doc.importNode(nlist.item(i), true);
                    partelem.appendChild(node);
                }
            } else {
                System.out.print("\nDidn't find matching theme in part " + part + "@" + path.toURI().toString() + " for theme " + theme + "!");
            }
        }
    }

    public static String md5ForNode(Node root) throws Exception {
        StringBuffer check = new StringBuffer();
        stringForNode(root, check);
        return DumpText.format(check.toString());
    }

    private static void stringForNode(Node root, StringBuffer checkstring) throws Exception {
        NodeList nl = root.getChildNodes();
        int length = nl.getLength();
        for (int m = 0; m < length; m++) {
            Node node = (Node) nl.item(m);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                checkstring.append(node.getNodeName());
                NamedNodeMap map = node.getAttributes();
                for (int k = 0; k < map.getLength(); k++) {
                    Node attr = map.item(k);
                    String attrname = attr.getNodeName();
                    if (!attrname.equals("alt") && !attrname.equals("title")) {
                        checkstring.append(attrname + "=" + attr.getNodeValue());
                    }
                }
                stringForNode(node, checkstring);
                checkstring.append("/" + node.getNodeName());
            }
        }
    }

    private static String format(String check) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        check = check.replaceAll(" ", "");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(check.getBytes("ISO-8859-1"));
        byte[] end = md5.digest();
        String digest = "";
        for (int i = 0; i < end.length; i++) {
            digest += ((end[i] & 0xff) < 16 ? "0" : "") + Integer.toHexString(end[i] & 0xff);
        }
        return digest;
    }
}
