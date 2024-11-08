import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.MultiLineProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyFile;
import genj.gedcom.PropertyName;
import genj.gedcom.PropertySex;
import genj.gedcom.PropertyXRef;
import genj.gedcom.TagPath;
import genj.report.Report;
import genj.window.CloseWindow;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * GenJ - Report
 * @author Nils Meier <nils@meiers.net>
 * @version 1.0
 */
public class ReportHTMLSheets extends Report {

    private static final String INDEX = "index.html";

    /** an options - style sheet */
    public String styleSheet = "";

    /** A <pre>&nbsp</pre> looks better than an empty String in a HTML cell */
    private static final String SPACE = "&nbsp;";

    /** whether to translate between unicode and html or not (slow!) */
    public boolean isUnicode2HTML = true;

    /** buffer size for read/write operation on images */
    private static final int IMG_BUFFER_SIZE = 1024;

    /** buffer for read/write operation on images */
    private byte[] imgBuffer = new byte[IMG_BUFFER_SIZE];

    /** HTML Coded Character Set (see http://www.w3.org/MarkUp/html-spec/html-spec_13.html)*/
    private static final String[] codeTable = { "Ñ", "&Ntilde;", "Ò", "&Ograve;", "Ó", "&Oacute;", "Ô", "&Ocirc;", "Õ", "&Otilde;", "Ö", "&Ouml;", "ß", "&szlig;", "À", "&Agrave;", "Á", "&Aacute;", "Â", "&Acirc;", "Ã", "&Atilde;", "Ä", "&Auml;", "Å", "&Aring;", "Æ", "&AElig;", "Ç", "&Ccedil;", "È", "&Egrave;", "É", "&Eacute;", "Ê", "&Ecirc;", "Ë", "&Euml;", "Ì", "&Igrave;", "Í", "&Iacute;", "Î", "&Icirc;", "Ï", "&Iuml;", "Ù", "&Ugrave;", "Ú", "&Uacute;", "Û", "&Ucirc;", "Ü", "&Uuml;", "Ý", "&Yacute;", "à", "&agrave;", "á", "&aacute;", "â", "&acirc;", "ã", "&atilde;", "ä", "&auml;", "æ", "&aelig;", "ç", "&ccedil;", "è", "&egrave;", "é", "&eacute;", "ê", "&ecirc;", "ë", "&euml;", "ì", "&igrave;", "í", "&iacute;", "î", "&icirc;", "ï", "&iuml;", "ð", "&eth;", "ñ", "&ntilde;", "ò", "&ograve;", "ó", "&oacute;", "ô", "&ocirc;", "õ", "&otilde;", "ö", "&ouml;", "ø", "&oslash;", "ù", "&ugrave;", "ú", "&uacute;", "û", "&ucirc;", "ü", "&uuml;", "ý", "&yacute;", "þ", "&thorn;", "ÿ", "&yuml;" };

    /** the mapping between unicode and html */
    private static Hashtable unicode2html = initializeUnicodeSupport();

    /**
   * Exports the given entity to given directory
   */
    private void export(Entity ent, File dir, PrintWriter out) throws IOException {
        printOpenHTML(out, ent.toString());
        out.println("<TABLE border=\"1\" cellspacing=\"1\" width=\"100%\">");
        out.println("<TR>");
        out.println("<TD colspan=\"2\" bgcolor=\"f0f0f0\">" + wrapText(ent.toString()) + "</TD>");
        out.println("</TR>");
        out.println("<TR>");
        out.println("<TD width=\"50%\" valign=\"center\" align=\"center\">");
        exportImage(ent, dir, out);
        out.println("</TD>");
        out.println("<TD width=\"50%\" valign=\"top\" align=\"left\">");
        out.println("<TABLE border=0>");
        exportProperty(ent, out, 0);
        out.println("</TABLE>");
        out.println("</TD>");
        out.println("</TR>");
        out.println("</TABLE>");
        printCloseHTML(out);
    }

    /**
   * Exports the given entity's image
   */
    private void exportImage(Entity ent, File dir, PrintWriter out) throws IOException {
        String url = null;
        PropertyFile file = (PropertyFile) ent.getProperty(new TagPath("INDI:OBJE:FILE"));
        if (file != null && file.getFile() != null && file.getFile().exists()) {
            url = exportImage(file, dir, ent.getId());
            out.println("<IMG src=\"" + url + "\"></IMG>");
        }
        if (url == null) out.println(i18n("no.image"));
    }

    /**
   * Exports the given entity's image
   */
    private String exportImage(PropertyFile prop, File dir, String name) throws IOException {
        String suffix = null;
        if (prop.getValue().toLowerCase().indexOf("jpg") > 0) {
            suffix = ".jpg";
        }
        if (prop.getValue().toLowerCase().indexOf("gif") > 0) {
            suffix = ".gif";
        }
        if (prop.getValue().toLowerCase().indexOf("png") > 0) {
            suffix = ".png";
        }
        if (suffix == null) {
            return "";
        }
        OutputStream imgOut = null;
        InputStream imgIn = null;
        try {
            imgOut = new FileOutputStream(new File(dir, name + suffix));
            imgIn = prop.getInputStream();
            while (true) {
                int read = imgIn.read(imgBuffer);
                if (read <= 0) {
                    break;
                }
                imgOut.write(imgBuffer, 0, read);
            }
        } finally {
            try {
                imgOut.close();
            } catch (Exception e) {
            }
            ;
            try {
                imgIn.close();
            } catch (Exception e) {
            }
            ;
        }
        return name + suffix;
    }

    /**
   * Exports the given entity's properties
   */
    private void exportProperty(Property prop, PrintWriter out, int level) {
        exportPropertyLine(prop, out, level);
        for (int i = 0; i < prop.getNoOfProperties(); i++) {
            exportProperty(prop.getProperty(i), out, level + 1);
        }
    }

    /**
   * Exports the given property in a line in the table
   */
    private void exportPropertyLine(Property prop, PrintWriter out, int level) {
        if (prop instanceof PropertyXRef) {
            PropertyXRef xref = (PropertyXRef) prop;
            if (!(xref.getReferencedEntity() instanceof Indi || xref.getReferencedEntity() instanceof Fam)) return;
        }
        String markupBeg;
        String markupEnd;
        if (level == 0) {
            markupBeg = "<b><u>";
            markupEnd = "</u></b>";
        } else if (level == 1) {
            markupBeg = "<i><u>";
            markupEnd = "</u></i>";
        } else {
            markupBeg = "<i>";
            markupEnd = "</i>";
        }
        out.println("<tr>");
        boolean showValue = level > 0 && !prop.isReadOnly();
        out.print("<td valign=TOP ");
        if (!showValue) out.print("colspan=2");
        out.print(">");
        exportSpaces(out, level);
        out.print(markupBeg);
        StringTokenizer tag = new StringTokenizer(wrapText(Gedcom.getName(prop.getTag())), " ");
        while (tag.hasMoreElements()) out.print(tag.nextToken() + SPACE);
        out.print(markupEnd);
        out.println("</td>");
        if (showValue) {
            out.print("<td>");
            exportPropertyValue(prop, out);
            out.println("</td>");
        }
        out.println("</tr>");
    }

    /**
   * Exports the given property's value
   */
    private void exportPropertyValue(Property prop, PrintWriter out) {
        if (prop instanceof PropertyXRef) {
            PropertyXRef xref = (PropertyXRef) prop;
            Entity ent = xref.getReferencedEntity();
            out.println("<A HREF=\"" + ent.getId() + ".html\">" + wrapText(ent.toString()) + "</a>");
            return;
        }
        if (prop instanceof MultiLineProperty) {
            MultiLineProperty.Iterator lines = ((MultiLineProperty) prop).getLineIterator();
            do {
                out.print(wrapText(lines.getValue()));
                out.print("<br>");
            } while (lines.next());
            return;
        }
        String value;
        if (prop instanceof PropertyName) value = ((PropertyName) prop).getName(); else value = wrapText(prop.toString());
        out.print(value);
    }

    /**
   * Generates Spaces
   */
    private void exportSpaces(PrintWriter out, int num) {
        for (int c = 0; c < num; c++) {
            out.print(SPACE);
        }
    }

    /**
   * Helper that resolves a filename for given entity
   */
    private File getFileForEntity(File dir, Entity entity) {
        return new File(dir, entity.getId() + ".html");
    }

    private File getFileForIndex(File dir) {
        return new File(dir, INDEX);
    }

    /**
   * Exports the given entity to given directory
   */
    private void exportSheet(Entity ent, File dir) throws IOException {
        File file = getFileForEntity(dir, ent);
        println(i18n("exporting", new String[] { ent.toString(), file.getName() }));
        PrintWriter htmlOut = new PrintWriter(new FileOutputStream(file));
        export(ent, dir, htmlOut);
        if (htmlOut.checkError()) throw new IOException("Error while writing " + ent);
        htmlOut.close();
    }

    /**
   * Exports the given entities to given directory
   */
    private void exportSheets(Collection ents, File dir) throws IOException {
        for (Iterator it = ents.iterator(); it.hasNext(); ) exportSheet((Entity) it.next(), dir);
    }

    /**
   * Exports index.html row
   */
    private void exportIndexRow(PrintWriter out, Indi indi) throws IOException {
        printCell(out, wrapID(indi));
        printCell(out, indi.getLastName());
        printCell(out, indi.getFirstName());
        printCell(out, indi.getProperty("SEX", true));
        printCell(out, indi.getProperty(new TagPath("INDI:BIRT:DATE")));
        printCell(out, indi.getProperty(new TagPath("INDI:BIRT:PLAC")));
        printCell(out, indi.getProperty(new TagPath("INDI:DEAT:DATE")));
        printCell(out, indi.getProperty(new TagPath("INDI:DEAT:PLAC")));
    }

    /** 
   * Exports index.html
   */
    private void exportIndex(Gedcom gedcom, File dir) throws IOException {
        PrintWriter out = new PrintWriter(new FileOutputStream(getFileForIndex(dir)));
        println(i18n("exporting", new String[] { "index.html", dir.toString() }));
        printOpenHTML(out, gedcom.getName());
        out.println("<TABLE border=1 cellspacing=1>");
        out.println("<TR BGCOLOR=\"yellow\">");
        printCell(out, "ID");
        printCell(out, PropertyName.getLabelForLastName());
        printCell(out, PropertyName.getLabelForFirstName());
        printCell(out, PropertySex.TXT_SEX);
        printCell(out, Gedcom.getName("BIRT"));
        printCell(out, Gedcom.getName("PLAC"));
        printCell(out, Gedcom.getName("DEAT"));
        printCell(out, Gedcom.getName("PLAC"));
        out.println("</TR>");
        Entity[] indis = gedcom.getEntities(Gedcom.INDI, "INDI:NAME");
        for (int i = 0; i < indis.length; i++) {
            out.println("<TR>");
            exportIndexRow(out, (Indi) indis[i]);
            out.println("</TR>");
        }
        out.println("</TABLE>");
        printCloseHTML(out);
        if (out.checkError()) throw new IOException("Error while writing index.html");
        out.close();
    }

    /**
   * While we generate information on stdout it's not really
   * necessary because we're bringing up the result in a
   * browser anyways
   */
    public boolean usesStandardOut() {
        return false;
    }

    /**
   * The report's entry point
   */
    public void start(Object context) {
        Gedcom gedcom = (Gedcom) context;
        File dir = getDirectoryFromUser(i18n("target.dir"), CloseWindow.TXT_OK);
        if (dir == null) return;
        println(i18n("target.dir") + " = " + dir);
        if (!dir.exists() && !dir.mkdirs()) {
            println("***Couldn't create output directory " + dir);
            return;
        }
        try {
            exportSheets(gedcom.getEntities(Gedcom.INDI), dir);
            exportSheets(gedcom.getEntities(Gedcom.FAM), dir);
            exportIndex(gedcom, dir);
        } catch (IOException e) {
            println("Error while exporting: " + e.getMessage());
        }
        try {
            showBrowserToUser(getFileForIndex(dir).toURL());
        } catch (MalformedURLException e) {
        }
    }

    /**
   * Initializes a Hashtable of Unicode 2 HTML code mappings
   */
    private static Hashtable initializeUnicodeSupport() {
        Hashtable result = new Hashtable();
        for (int c = 0; c < codeTable.length; c += 2) {
            result.put(codeTable[c + 0], codeTable[c + 1]);
        }
        return result;
    }

    /**
   * Calculate a url for individual's id 
   */
    private String wrapID(Indi indi) {
        return "<a href=\"" + getFileForEntity(null, indi).getName() + "\">" + indi.getId() + "</a>";
    }

    /**
   * Helper to make sure that a Unicode text ends up nicely in HTML
   */
    private String wrapText(String text) {
        if (!isUnicode2HTML) return text;
        StringBuffer result = new StringBuffer(256);
        for (int c = 0; c < text.length(); c++) {
            String unicode = text.substring(c, c + 1);
            Object html = unicode2html.get(unicode);
            if (html == null) {
                result.append(unicode);
            } else {
                result.append(html);
            }
        }
        return result.toString();
    }

    /**
   * Writes HTML table cell information
   */
    private void printCell(PrintWriter out, Object content) {
        if (content instanceof Property) content = ((Property) content).toString();
        if (content == null || content.toString().length() == 0) content = SPACE;
        out.println("<TD>" + wrapText(content.toString()) + "</TD>");
    }

    /**
   * Writes HTML header and body information
   */
    private void printOpenHTML(PrintWriter out, String title) {
        out.println("<HTML>");
        out.println("<HEAD>");
        out.println("<TITLE>" + title + "</TITLE>");
        if (styleSheet.length() > 0) out.println("<link rel=StyleSheet href=\"" + styleSheet + "\" type=\"text/css\"></link>");
        out.println("</HEAD>");
        out.println("<BODY bgcolor=\"#ffffff\">");
    }

    /**
   * Writes HTML end header and end body information
   */
    private void printCloseHTML(PrintWriter out) {
        out.println("<a href=\"" + INDEX + "\">Index</a>");
        out.println("</BODY>");
        out.println("</HTML>");
    }
}
