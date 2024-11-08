package gate.yam.convert;

import gate.util.GateException;
import gate.yam.YamFile;
import org.jdom.*;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.springframework.core.io.FileSystemResource;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.nio.channels.FileChannel;

/** 
 * Convert JSPWiki syntax to YAM.
 * @author Valentin Tablan
 */
public class JSPWikiToYamConverter {

    /** Encoding used when reading JSPWiki files and writing YAM files */
    private static final String INPUT_ENCODING = "ISO-8859-1";

    private static final String OUTPUT_ENCODING = "UTF-8";

    /**
   * Characters that should be escaped when generating Yam.
   */
    private static final char[] YAM_SPECIAL_CHARACTERS = "_*^".toCharArray();

    /**
   * Converts a JSPWiki page into YAM format.
   * @param jspWikiSource the String representing the JSPWiki content
   * @return a String representation of a YAM page
   * @throws TransformerException if problems occurred while performing the XSL
   * transformation
   * @throws IOException if problems occurred while parsing the JSPWiki format 
   */
    public static String stringToString(String jspWikiSource) throws TransformerException, IOException {
        Reader reader = new StringReader(jspWikiSource);
        return readerToString(reader);
    }

    /**
   * Converts text in JSPWiki format to YAM format.
   * @param jspReader a reader that provides the JSPWiki content
   * @return a String with YAM data
   * @throws TransformerException
   * @throws IOException
   */
    public static String readerToString(Reader jspReader) throws TransformerException, IOException {
        return readerToStringWithTitle(jspReader, null);
    }

    /**
   * Converts text in JSPWiki format to YAM format, adding the given title
   * to the document. If the title is null, none is added.
   * @param jspReader a reader that provides the JSPWiki content
   * @param title the title to give the YAM document
   * @return a String with YAM data
   * @throws TransformerException
   * @throws IOException
   */
    public static String readerToStringWithTitle(Reader jspReader, String title) throws TransformerException, IOException {
        JSPWikiMarkupParser parser = new JSPWikiMarkupParser(jspReader);
        Document jdomDoc = parser.parse();
        processHeadings(jdomDoc);
        processEscapes(jdomDoc);
        massageLinks(jdomDoc);
        processEntityReferences(jdomDoc);
        processSpecifics(jdomDoc);
        if (title != null) addTitle(jdomDoc, title);
        return HtmlToYamConverter.jdomToString(jdomDoc);
    }

    /**
   * Find all headings in a DOM, and add an empty paragraph after all of
   * those headings that don't have one already. JSPWiki headings are terminated
   * by new lines, whereas YAM headings are terminated by blank lines - this
   * method ensures a correct translation.
   * @param jdomDoc The document in which headings will be adjusted
   */
    private static void processHeadings(org.jdom.Document jdomDoc) {
        final Pattern headingPattern = Pattern.compile("[Hh][123456]");
        class HeadingFilter implements Filter {

            public boolean matches(Object obj) {
                if (!(obj instanceof Element)) return false;
                Element el = (Element) obj;
                return headingPattern.matcher(el.getName()).matches();
            }
        }
        List<Element> toAddParaAfter = new ArrayList<Element>();
        for (Iterator hIt = jdomDoc.getDescendants(new HeadingFilter()); hIt.hasNext(); ) {
            Element hEl = (Element) hIt.next();
            Content next = getNextSibling(hEl);
            boolean emptyPara = false;
            if (next instanceof Element) {
                Element nextEl = (Element) next;
                if (nextEl.getName().equalsIgnoreCase("p") && nextEl.getChildren().isEmpty()) {
                    emptyPara = true;
                }
            }
            if (!emptyPara) {
                toAddParaAfter.add(hEl);
            }
        }
        for (Element hEl : toAddParaAfter) {
            Element parEl = hEl.getParentElement();
            int hIndex = parEl.indexOf(hEl);
            parEl.addContent(hIndex + 1, new Element("p"));
        }
    }

    /**
   * Adds some text content to the document, as the first child Element of the
   * body Element. This will become the title in YAM syntax.
   * @param jdomDoc The document to which text content will be added
   * @param title The title to add as text content
   */
    private static void addTitle(org.jdom.Document jdomDoc, String title) {
        Element body = jdomDoc.getRootElement().getChild("body");
        body.addContent(0, new Text(title));
        body.addContent(1, new Element("p"));
        body.addContent(2, new Element("p"));
    }

    /**
   * Find all local hrefs in a document, and massage them into yam form. Strip
   * out the leading VIEW which is added by JSPWikiMarkupParser, and carried
   * through to yam unless we remove them. Add a .html suffix.
   * @param jdomDoc The document in which links will be adjusted
   */
    private static void massageLinks(org.jdom.Document jdomDoc) {
        for (Iterator aIt = jdomDoc.getDescendants(new ElementFilter("a")); aIt.hasNext(); ) {
            Element aEl = (Element) aIt.next();
            String href = aEl.getAttributeValue("href");
            if (href != null) {
                if (href.startsWith("VIEW")) {
                    href = href.substring(4) + ".html";
                }
                href = href.replace(",", "\\,");
                href = href.replace(" ", "\\ ");
                aEl.setAttribute("href", href);
            }
        }
    }

    /**
   * Deal with specific one-off problems in JSPWiki to YAM conversion.
   * Essentially, a load of hard coding to handle strange cases...
   * @param jdomDoc The document to process
   */
    private static void processSpecifics(org.jdom.Document jdomDoc) {
        List<Content> toRemove = new ArrayList<Content>();
        for (Iterator textIt = jdomDoc.getDescendants(new ContentFilter(ContentFilter.TEXT)); textIt.hasNext(); ) {
            Text text = (Text) textIt.next();
            String content = text.getText();
            if (content.contains("Group.jsp?group")) {
                toRemove.add(text);
            }
        }
        for (Content remove : toRemove) {
            Element parent = remove.getParentElement();
            parent.removeContent(remove);
            if (parent.getName().equals("li") && parent.getChildren().size() == 0) {
                Element grandParent = parent.getParentElement();
                grandParent.removeContent(parent);
            }
        }
    }

    /**
   * Special entities and their replacements
   */
    private static Map<String, String> SPECIAL_ENTITIES;

    static {
        SPECIAL_ENTITIES = new HashMap<String, String>();
        SPECIAL_ENTITIES.put("&lt;", "<");
        SPECIAL_ENTITIES.put("&gt;", ">");
        SPECIAL_ENTITIES.put("&amp;", "&");
        SPECIAL_ENTITIES.put("&quot;", "\"");
    }

    /**
   * This method replaces all references to html special
   * entities, in a DOM, with their legal yam characters.
   * @param jdomDoc The document to process
   */
    private static void processEntityReferences(org.jdom.Document jdomDoc) {
        for (Iterator textIt = jdomDoc.getDescendants(new ContentFilter(ContentFilter.TEXT)); textIt.hasNext(); ) {
            Text text = (Text) textIt.next();
            String content = text.getText();
            for (String key : SPECIAL_ENTITIES.keySet()) {
                content = content.replace(key, SPECIAL_ENTITIES.get(key));
            }
            if (text.getParentElement().getName().equalsIgnoreCase("li")) content = content.replaceAll("^(?:\\r?\\n)*", " ").replaceAll("(?:\\r?\\n)*$", " ");
            text.setText(content);
        }
    }

    /**
   * This method walks the whole DOM tree and, for each text node found, it
   * escapes the YAM special characters. Adapted from HtmlToYamConverter.
   *
   * @param jdomDoc The document in which special characters will be escaped
   */
    private static void processEscapes(org.jdom.Document jdomDoc) {
        org.jdom.Content currentNode = jdomDoc.getRootElement();
        boolean finished = false;
        while (!finished) {
            if (currentNode instanceof org.jdom.Text && !currentNode.getParentElement().getName().equalsIgnoreCase("pre")) {
                org.jdom.Text textNode = (org.jdom.Text) currentNode;
                String textData = textNode.getText();
                for (char c : YAM_SPECIAL_CHARACTERS) {
                    if (textData.indexOf(c) != -1) {
                        textData = textData.replace(Character.toString(c), "\\" + c);
                    }
                }
                textNode.setText(textData);
            }
            Content nextNode = null;
            if (currentNode instanceof Parent && ((Parent) currentNode).getContentSize() > 0) {
                nextNode = ((Parent) currentNode).getContent(0);
            }
            if (nextNode == null) {
                nextNode = getNextSibling(currentNode);
                if (nextNode == null) {
                    while (nextNode == null && !finished) {
                        Parent parent = currentNode.getParent();
                        if (parent == null || parent instanceof org.jdom.Document) {
                            finished = true;
                        } else {
                            currentNode = (Content) parent;
                            nextNode = getNextSibling((Content) parent);
                        }
                    }
                }
            }
            currentNode = nextNode;
        }
    }

    /**
   * Gets the sibling of a JDom node. Copied from HtmlToYamConverter
   * @param node The node for which the next sibling will be returned
   * @return The next sibling of node
   */
    private static Content getNextSibling(Content node) {
        Parent parent = node.getParent();
        if (parent != null) {
            int currentIndex = parent.indexOf(node);
            if (parent.getContentSize() > (currentIndex + 1)) {
                return parent.getContent(currentIndex + 1);
            }
        }
        return null;
    }

    /**
   * Get the attachments to a JSPWiki file, copy them to a YAM wiki site, and
   * list links to them at the end of the given yam file.
   * @param jspwFile The JSPWiki text file from which attachments will be taken
   * @param yamFile The YAM text file to which attachments will be added
   */
    private static void processAttachments(File jspwFile, File yamFile) throws IOException {
        String jspwFilePath = jspwFile.getAbsolutePath();
        String jspwAttachDirPath = jspwFilePath.substring(0, jspwFilePath.length() - 4);
        File jspwAttachDir = new File(jspwAttachDirPath);
        String yamAttachDirName = jspwAttachDir.getName();
        File yamAttachDir = new File(yamFile.getParent(), yamAttachDirName);
        if (jspwAttachDir.isDirectory()) {
            List<String> yamAttachFileNames = new ArrayList<String>();
            for (File jspwAttachFile : jspwAttachDir.listFiles()) {
                if (jspwAttachFile.isFile() && !jspwAttachFile.isHidden()) {
                    String yamAttachFileName = jspwAttachFile.getName();
                    yamAttachFileNames.add(yamAttachFileName);
                    File yamAttachFile = new File(yamAttachDir, yamAttachFileName);
                    copy(jspwAttachFile, yamAttachFile);
                }
            }
            StringBuilder strB = new StringBuilder();
            strB.append("---\n");
            strB.append("%2* Attachments\n");
            for (String fileName : yamAttachFileNames) {
                strB.append("- %(");
                strB.append(yamAttachDirName).append("/").append(fileName);
                strB.append(", ").append(fileName).append(")\n");
            }
            PrintWriter pw = new PrintWriter(new FileOutputStream(yamFile, true), true);
            pw.append(strB.toString());
            pw.close();
        }
    }

    /**
   * Copy one File to another.
   * @param in The File that will be copied
   * @param out The File to which in will be copied
   * @throws IOException if the copy fails
   */
    private static void copy(File in, File out) throws IOException {
        if (!out.getParentFile().isDirectory()) out.getParentFile().mkdirs();
        FileChannel ic = new FileInputStream(in).getChannel();
        FileChannel oc = new FileOutputStream(out).getChannel();
        ic.transferTo(0, ic.size(), oc);
        ic.close();
        oc.close();
    }

    /**
   * A FilenameFilter that accepts JSPWiki .txt source files.
   */
    private static class JSPWikiFileFilter implements FilenameFilter {

        /** Accept a file if it is a .txt file*/
        public boolean accept(File dir, String name) {
            return name.endsWith(".txt");
        }
    }

    /**
   * Run the JSPWikiToYamConverter, translating the files specified on the
   * command line from JSPWiki to YAM format.
   * @param args (JSPWiki file | JSPWiki directory) [output directory]
   */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        File inFile = new File(args[0]);
        List<File> filesToConvert = new ArrayList<File>();
        if (inFile.isFile()) {
            filesToConvert.add(inFile);
        } else if (inFile.isDirectory()) {
            File[] filesInDir = inFile.listFiles(new JSPWikiFileFilter());
            filesToConvert.addAll(Arrays.asList(filesInDir));
        } else {
            printUsage();
            System.exit(1);
        }
        String outDirName = null;
        if (args.length > 1) {
            outDirName = args[1];
            if (!new File(outDirName).isDirectory()) {
                printUsage();
                System.exit(1);
            }
        }
        List<String> errors = new ArrayList<String>();
        List<YamFile> yamsToGenerate = new ArrayList<YamFile>();
        for (File jspwFile : filesToConvert) {
            String jspwFileName = jspwFile.getName();
            String prefix = jspwFileName.substring(0, jspwFileName.length() - 4);
            String yamFileName = prefix + ".yam";
            File yamDiskFile = new File(outDirName, yamFileName);
            try {
                System.out.println("Translating " + jspwFileName);
                PrintWriter yamOut = new PrintWriter(yamDiskFile, OUTPUT_ENCODING);
                Reader reader = new InputStreamReader(new FileInputStream(jspwFile), INPUT_ENCODING);
                yamOut.println(readerToStringWithTitle(reader, prefix));
                yamOut.flush();
                YamFile yamFile = YamFile.get(new FileSystemResource(yamDiskFile.getCanonicalPath()));
                processAttachments(jspwFile, yamDiskFile);
                yamFile.setContextPath(outDirName);
                yamsToGenerate.add(yamFile);
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(yamFileName + ": " + e.toString());
            }
            for (YamFile yam : yamsToGenerate) {
                try {
                    yam.generate();
                } catch (GateException ge) {
                    ge.printStackTrace();
                    errors.add(yam + ": " + ge.toString());
                }
            }
        }
        System.out.println("Translation finished with " + errors.size() + " errors");
        for (String error : errors) {
            System.out.println(error);
        }
    }

    /**
   * Print the command line usage of th is class to standard out.
   */
    private static void printUsage() {
        System.out.println("JSPWikiToYamConverter - convert JSPWiki files to YAM");
        System.out.println("Usage:");
        System.out.println("  JSPWikiToYamConverter (file|directory) [outputDir]");
        System.out.println("    file:      JSPWiki file to translate");
        System.out.println("    directory: directory of files to translate");
        System.out.println("    outputDir: directory to write YAM files to");
        System.out.println("               (defaults to current directory)");
    }
}
