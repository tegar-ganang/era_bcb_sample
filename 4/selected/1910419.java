package org.lindenb.tool.xul4wikipedia;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.lindenb.io.IOUtils;
import org.lindenb.lang.ResourceUtils;
import org.lindenb.net.CGI;
import org.lindenb.sw.vocabulary.XUL;
import org.lindenb.util.C;
import org.lindenb.util.Compilation;
import org.lindenb.xml.XMLUtilities;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XUL4Wikipedia
 *
 */
public class XUL4Wikipedia {

    private static long ID_GENERATOR = System.currentTimeMillis();

    private String action = "";

    private String urchin = null;

    private void echoForm(String msg, String menu) {
        String html = ResourceUtils.getContent(XUL4Wikipedia.class, "form.html", "");
        if (menu == null) menu = ResourceUtils.getContent(XUL4Wikipedia.class, "menu.xml", "");
        html = html.replaceFirst("__MENU_SAMPLE__", (menu == null ? "" : XMLUtilities.escape(menu))).replaceAll("__ABOUT__", XMLUtilities.escape(Compilation.getLabel())).replaceAll("__URCHIN__", (urchin == null ? "" : "<script src=\"http://www.google-analytics.com/urchin.js\" type=\"text/javascript\">" + "</script>" + "<script type=\"text/javascript\">" + "_uacct = \"" + XMLUtilities.escape(urchin) + "\";" + "urchinTracker();" + "</script>")).replaceAll("__ACTION__", (action == null ? "" : action)).replaceAll("__ERROR__", (msg == null ? "" : "<div class='error'>" + XMLUtilities.escape(msg) + "</div>"));
        System.out.println("Content-type: text/html");
        System.out.print("Content-Length: " + html.length() + "\n");
        System.out.println();
        System.out.print(html);
        System.out.flush();
    }

    private void cgiRun() {
        CGI cgi = new CGI();
        cgi.setContentMaxLength(1024 * 8);
        try {
            cgi.parse();
        } catch (Throwable err) {
            cgi = null;
            echoForm(err.getMessage(), null);
            return;
        }
        String xml = cgi.getString("xml");
        if (xml == null) {
            echoForm(null, null);
            return;
        }
        byte xpi[] = null;
        try {
            ByteArrayOutputStream fout = new ByteArrayOutputStream();
            StringReader sr = new StringReader(xml);
            new XUL4Wikipedia().run(sr, fout);
            fout.flush();
            fout.close();
            xpi = fout.toByteArray();
        } catch (Throwable err) {
            echoForm(err.getMessage(), xml);
            return;
        }
        System.out.print("Content-type: application/x-xpinstall\n");
        System.out.print("Content-Disposition: attachment; filename=xul4wikipedia.xpi\n");
        System.out.print("Content-Length: " + xpi.length + "\n");
        System.out.print("Content-Transfer-Encoding: binary\n");
        System.out.print("Cache-Control: no-cache\n");
        System.out.print("Pragma: no-cache\n");
        System.out.print("\n");
        System.out.write(xpi, 0, xpi.length);
        System.out.flush();
    }

    private XUL4Wikipedia() {
    }

    private abstract static class AbstractMenu {

        String id = String.valueOf("wkpd" + (ID_GENERATOR++));

        String label = "Untitled";

        AbstractMenu next;

        public String getId() {
            return this.id;
        }

        void addSib(AbstractMenu c) {
            if (next == null) {
                next = c;
            } else {
                next.addSib(c);
            }
        }

        abstract void toXUL(PrintWriter out) throws IOException;

        abstract void toJS(PrintWriter out) throws IOException;
    }

    private static class Menu extends AbstractMenu {

        AbstractMenu child = null;

        void addChild(AbstractMenu c) {
            if (child == null) {
                child = c;
            } else {
                child.addSib(c);
            }
        }

        void toXUL(PrintWriter out) throws IOException {
            out.print("<menu label=" + quote(label) + " id=" + quote(getId()) + ">");
            out.print("<menupopup>");
            AbstractMenu c = child;
            while (c != null) {
                c.toXUL(out);
                c = c.next;
            }
            out.print("</menupopup>");
            out.print("</menu>");
        }

        void toJS(PrintWriter out) throws IOException {
            AbstractMenu c = child;
            while (c != null) {
                c.toJS(out);
                c = c.next;
            }
        }
    }

    private static class MenuItem extends AbstractMenu {

        String content;

        void toXUL(PrintWriter out) throws IOException {
            out.print("<menuitem label=" + quote(label) + " id=" + quote(getId()) + " oncommand=" + quote("MY.cmd_" + getId() + "();") + "/>");
        }

        void toJS(PrintWriter out) throws IOException {
            out.println(",cmd_" + getId() + ":function(){" + "MY.insertTemplate(\"" + C.escape(this.content) + "\");}");
        }
    }

    private Menu parseMenu(Element root) throws IOException {
        if (!root.getNodeName().equals("menu")) throw new IOException("Expected <menu> but found <" + root.getNodeName() + ">");
        Attr att = root.getAttributeNode("label");
        Menu menu = new Menu();
        if (att != null) {
            menu.label = att.getValue();
        }
        for (Node c1 = root.getFirstChild(); c1 != null; c1 = c1.getNextSibling()) {
            if (c1.getNodeType() != Node.ELEMENT_NODE) continue;
            Element sub = Element.class.cast(c1);
            if (sub.getNodeName().equals("menu")) {
                menu.addChild(parseMenu(sub));
            } else if (sub.getNodeName().equals("menu-item")) {
                if (!sub.hasChildNodes()) throw new IOException("found empty <" + root.getNodeName() + ">");
                att = sub.getAttributeNode("label");
                MenuItem mi = new MenuItem();
                if (att != null) {
                    mi.label = att.getValue();
                }
                mi.content = sub.getTextContent();
                menu.addChild(mi);
            } else {
                throw new IOException("Unknown tag found <" + sub.getNodeName() + ">");
            }
        }
        return menu;
    }

    private void run(Reader xmlIn, OutputStream out) throws IOException, SAXException {
        Document dom = null;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            f.setCoalescing(true);
            f.setIgnoringComments(true);
            f.setValidating(false);
            DocumentBuilder b = f.newDocumentBuilder();
            dom = b.parse(new InputSource(xmlIn));
        } catch (ParserConfigurationException err) {
            throw new IOException(err);
        }
        Element root = dom.getDocumentElement();
        if (root == null) throw new SAXException("Not root in document");
        Attr att = root.getAttributeNode("label");
        if (att == null) root.setAttribute("label", "Wikipedia");
        Menu menu = parseMenu(root);
        menu.id = "menuWikipedia";
        ZipOutputStream zout = new ZipOutputStream(out);
        String content = ResourceUtils.getContent(XUL4Wikipedia.class, "chrome.manifest");
        addEntry(zout, "chrome.manifest", content);
        content = ResourceUtils.getContent(XUL4Wikipedia.class, "install.rdf");
        addEntry(zout, "install.rdf", content);
        content = ResourceUtils.getContent(XUL4Wikipedia.class, "library.js");
        addDir(zout, "chrome/");
        addDir(zout, "chrome/content/");
        addDir(zout, "chrome/skin/");
        String signal = "/*INSERT_CMD_HERE*/";
        int n = content.indexOf(signal);
        if (n == -1) throw new RuntimeException("where is " + signal + " ??");
        ZipEntry entry = new ZipEntry("chrome/content/library.js");
        zout.putNextEntry(entry);
        PrintWriter pout = new PrintWriter(zout);
        pout.write(content.substring(0, n));
        menu.toJS(pout);
        pout.write(content.substring(n + signal.length()));
        pout.flush();
        zout.closeEntry();
        entry = new ZipEntry("chrome/content/menu.xul");
        zout.putNextEntry(entry);
        pout = new PrintWriter(zout);
        pout.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pout.println("<overlay id=\"xul4wikipedia\" xmlns=\"" + XUL.NS + "\">");
        pout.println("<script src=\"library.js\"/>");
        pout.println("<popup id=\"contentAreaContextMenu\">");
        pout.println("<menuseparator/>");
        menu.toXUL(pout);
        pout.println("</popup>");
        pout.println("</overlay>");
        pout.flush();
        zout.closeEntry();
        InputStream png = XUL4Wikipedia.class.getResourceAsStream("32px-Wikipedia-logo.png");
        if (png == null) throw new IOException("Cannot get icon");
        entry = new ZipEntry("chrome/skin/wikipedia.png");
        zout.putNextEntry(entry);
        IOUtils.copyTo(png, zout);
        zout.closeEntry();
        zout.finish();
        zout.flush();
    }

    private static void addDir(ZipOutputStream zout, String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zout.putNextEntry(entry);
        zout.closeEntry();
    }

    private static void addEntry(ZipOutputStream zout, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zout.putNextEntry(entry);
        PrintWriter out = new PrintWriter(zout);
        out.write(content);
        out.flush();
        zout.closeEntry();
    }

    private static String quote(String s) {
        return "\"" + XMLUtilities.escape(s) + "\"";
    }

    public static void main(String args[]) {
        try {
            XUL4Wikipedia main = new XUL4Wikipedia();
            int optind = 0;
            boolean runAsCGI = false;
            String xpiFileName = null;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("-cgi run as CGI (must be the first arg)");
                    System.err.println("-h this screen");
                    System.err.println("-o xpi file name (required)");
                    System.err.println("-u urchin google code for cgi (optional)");
                    System.err.println("-a action for cgi form");
                    System.err.println("<xml file>");
                    return;
                } else if (args[optind].equals("-cgi")) {
                    runAsCGI = true;
                } else if (args[optind].equals("-o")) {
                    xpiFileName = args[++optind];
                } else if (args[optind].equals("-u")) {
                    main.urchin = args[++optind];
                } else if (args[optind].equals("-a")) {
                    main.action = args[++optind];
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (runAsCGI) {
                if (optind != args.length) {
                    System.err.println("XUL4Wikipedia: wrong number of arguments");
                    System.exit(-1);
                }
                main.cgiRun();
                return;
            }
            if (xpiFileName == null) {
                System.err.println("xpi filename undefined");
                System.exit(-1);
            } else if (!xpiFileName.endsWith(".xpi")) {
                System.err.println("xpi filename " + xpiFileName + " sould end with xpi");
                System.exit(-1);
            }
            if (optind + 1 != args.length) {
                System.err.println("XUL4Wikipedia: wrong number of arguments");
                System.exit(-1);
            }
            BufferedReader in = new BufferedReader(new FileReader(args[optind++]));
            FileOutputStream fout = new FileOutputStream(xpiFileName);
            main.run(in, fout);
            in.close();
            fout.flush();
            fout.close();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
