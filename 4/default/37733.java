import pspdash.*;
import pspdash.data.DataRepository;
import pspdash.data.InterpolatingFilter;
import pspdash.data.ListData;
import pspdash.data.SaveableData;
import pspdash.data.SimpleData;
import pspdash.data.StringData;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.xml.sax.SAXException;
import org.w3c.dom.*;

/** CGI script for integrating external documents into the dashboard.
 *
 * Nearly every process instructs you to create various documents.
 * PSP, for example, tells you that you should create documents like
 * Process Improvement Proposal forms, Logic Specification Templates,
 * Design Review Checklists, etc.  Although it might be possible to spit
 * out a lot of java code and incorporate such forms into the dashboard
 * (as was done for the Size Estimating Template), the CONs outweigh
 * the PROs:
 * <ul>
 * <li><b>PRO:</b> form is handy, no external (commercial) software is needed
 * <li><b>CON:</b> requires LOTS of java coding for EACH new addition, and
 *     tons of work to maintain all that code
 * <li><b>CON:</b> dramatically limits the form/structure/content of the
 *     document (for example, it would be impossible for a developer to
 *     include a diagram in their design document)
 * <li><b>CON:</b> since the information captured in such documents is
 *     typically free-form, there really wouldn't be any (value added)
 *     analysis you could do on it anyway...so you haven't gained much by
 *     storing it inside the Data Repository.
 * </ul>
 *
 * A much better solution is to leverage existing application software for
 * document creation (for example, Microsoft Office on Windows, or
 * KOffice/StarOffice on Unix/Linux platforms).  Even people with
 * absolutely no commercial software probably still have access to some
 * sort of WYSIWYG editor for rich-text-format files.  The dashboard should
 * not attempt to rewrite or replace these applications.
 *
 * So this new CGI script is designed, based on the assumption that:
 * <ul>
 * <li>the user has other programs that are capable of editing the types
 *     of documents they want to edit, and
 * <li>their web browser is intelligent enough to open such documents in
 *     the appropriate editor.
 * </ul>
 * This is true of both IE and Netscape.
 *
 * Here's how the new script works, from an end-user perspective:
 * <ol>
 * <li>The user has created a project using the Hierarchy Editor and is
 *     viewing a process script for that process.
 * <li>They click on a hyperlink for an external document (e.g., Process
 *     Improvement Proposal Form).
 * <li>The first time they click on such a link, they might get a form
 *     asking them to fill in some information (for example, the name of
 *     the directory where the dashboard should store external documents
 *     for this project).  They fill out the form and click "OK".
 * <li>The document opens for editing.
 * </ol>
 *
 * Step #3 only happens if the dashboard doesn't already have all the
 * information it needs to locate the file.  Once the user fills out the
 * requested information, it is saved in the Data Repository appropriately.
 * So typically, the user might see that form once for any given project;
 * then, any time they click on a hyperlink, the document just opens for
 * editing.
 *
 * In the background, this script is:
 * <ol>
 * <li>Dynamically loading one or more XML files which describe the various
 *     documents that apply to the task at hand (what the documents are
 *     called and where they are located).
 * <li>Asking the user for more information if needed
 * <li>Checking to see if the destination directory/file already exists;
 *     if not,<ol>
 *     <li>creating the destination directory and all needed parents
 *     <li>locating the appropriate template for the file
 *     <li>copying the template to the destination</ol>
 * <li>Sending an HTTP redirect message back to the browser, with a
 *     file:// url pointing to the destination document.
 * </ol>
 *
 * The mechanism is very flexible, and driven by XML files (which would
 * presumably be written by the process author).
 */
public class file extends TinyCGIBase {

    public static final String FILE_PARAM = "file";

    public static final String PAGE_COUNT_PARAM = "pageCount";

    public static final String CONFIRM_PARAM = "confirm";

    public static final String FILE_XML_DATANAME = "FILES_XML";

    public static final String NAME_ATTR = "name";

    public static final String PATH_ATTR = "path";

    public static final String DEFAULT_PATH_ATTR = "defaultPath";

    public static final String TEMPLATE_PATH_ATTR = "templatePath";

    public static final String TEMPLATE_VAL_ATTR = "templateVal";

    public static final String DIRECTORY_TAG_NAME = "directory";

    public static final String TEMPLATE_ROOT_WIN = "\\Templates\\";

    public static final String TEMPLATE_ROOT_UNIX = "/Templates/";

    protected void writeHeader() {
    }

    /** Generate CGI script output. */
    protected void writeContents() throws IOException {
        parseFormData();
        String filename = getParameter(FILE_PARAM);
        if (filename == null) ;
        Element file = findFile(filename);
        if (file == null) {
            sendNoSuchFileMessage(filename);
            return;
        }
        File result = computePath(file, false);
        if (!metaPathVariables.isEmpty()) {
            pathVariables = metaPathVariables;
            pathVariableNames = metaPathVariableNames;
            displayNeedInfoForm(filename, null, false, MISSING_META, file);
            return;
        }
        if (result == null && !needPathInfo()) {
            sendNoSuchFileMessage(filename);
            return;
        }
        if (result == null) {
            displayNeedInfoForm(filename, result, false, MISSING_INFO, file);
            return;
        }
        if (!result.exists()) {
            if (getParameter(CONFIRM_PARAM) == null) {
                displayNeedInfoForm(filename, result, false, CREATE_CONFIRM, file);
                return;
            }
            if (isDirectory) {
                if (!result.mkdirs()) {
                    sendCopyTemplateError("Could not create the directory '" + result.getPath() + "'.");
                    return;
                }
            }
        }
        if (result.exists()) {
            redirectTo(filename, result);
            return;
        }
        savePathInfo();
        File template = computePath(file, true);
        if (!metaPathVariables.isEmpty()) {
            pathVariables = metaPathVariables;
            pathVariableNames = metaPathVariableNames;
            displayNeedInfoForm(filename, null, true, MISSING_META, file);
            return;
        }
        if (!foundTemplate) {
            restorePathInfo();
            displayNeedInfoForm(filename, result, false, CANNOT_LOCATE, file);
            return;
        }
        String templateURL = null;
        if (isTemplateURL(template)) try {
            templateURL = template.toURL().toString();
            templateURL = templateURL.substring(templateURL.indexOf(TEMPLATE_ROOT_UNIX) + TEMPLATE_ROOT_UNIX.length() - 1);
        } catch (MalformedURLException mue) {
        }
        if (template == null || (templateURL == null && !template.exists())) {
            displayNeedInfoForm(filename, template, true, MISSING_INFO, file);
            return;
        }
        File resultDir = result.getParentFile();
        if (!resultDir.exists()) if (!resultDir.mkdirs()) {
            sendCopyTemplateError("Could not create the directory '" + resultDir.getPath() + "'.");
            return;
        }
        if (copyFile(template, templateURL, result) == false) {
            sendCopyTemplateError("Could not copy '" + template.getPath() + "' to '" + result.getPath() + "'.");
            return;
        }
        redirectTo(filename, result);
    }

    /** Send an HTTP REDIRECT message. */
    private void redirectTo(String filename, File result) {
        try {
            boolean remoteRequest = false;
            try {
                DashController.checkIP(env.get("REMOTE_ADDR"));
            } catch (IOException ioe) {
                remoteRequest = true;
            }
            if (remoteRequest || "redirect".equals(docOpenSetting)) out.print("Location: " + result.toURL() + "\r\n\r\n"); else {
                Browser.openDoc(result.toURL().toString());
                out.print("Expires: 0\r\n");
                super.writeHeader();
                String pageCount = getParameter(PAGE_COUNT_PARAM);
                int back = -1;
                if (pageCount != null) back -= pageCount.length();
                out.println("<HTML><HEAD><SCRIPT>");
                out.print("history.go(" + back + ");");
                out.println("</SCRIPT></HEAD><BODY></BODY></HTML>");
            }
        } catch (MalformedURLException mue) {
            System.out.println("Exception: " + mue);
            displayNeedInfoForm(filename, result, false, CANNOT_LOCATE, null);
        }
    }

    /** Copy a file. */
    private boolean copyFile(File template, String templateURL, File result) {
        if (template == result) return true;
        if (templateURL == null && !template.isFile()) return true;
        if (!nameIsSafe(result)) return false;
        try {
            InputStream in = openInput(template, templateURL);
            in = new InterpolatingFilter(in, getDataRepository(), getPrefix());
            OutputStream out = new FileOutputStream(result);
            copyFile(in, out);
            return true;
        } catch (IOException ioe) {
        }
        return false;
    }

    private InputStream openInput(File template, String templateURL) throws IOException {
        if (templateURL != null) return new ByteArrayInputStream(getRequest(templateURL, true)); else return new FileInputStream(template);
    }

    private boolean nameIsSafe(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        for (int i = FORBIDDEN_SUFFIXES.length; i-- > 0; ) if (name.endsWith(FORBIDDEN_SUFFIXES[i])) return false;
        return true;
    }

    private static final String[] FORBIDDEN_SUFFIXES = { ".jar", ".zip", ".class", ".com", ".exe", ".bat", ".cmd", ".vbs", ".vbe", ".js", ".jse", ".wsf", ".wsh", ".pl" };

    /** Copy a file. */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
        in.close();
        out.close();
    }

    private boolean isTemplateURL(File f) {
        if (f == null) return false;
        String path = f.getPath();
        return (path.startsWith(TEMPLATE_ROOT_WIN) || path.startsWith(TEMPLATE_ROOT_UNIX));
    }

    private boolean foundTemplate, isDirectory;

    private Map pathVariables, savedPathVariables, metaPathVariables;

    private ArrayList pathVariableNames, savedPathVariableNames, metaPathVariableNames;

    /** Backup the pathVariable settings in case we need them later. */
    private void savePathInfo() {
        savedPathVariables = pathVariables;
        savedPathVariableNames = pathVariableNames;
    }

    /** restore the pathVariable settings from the backup. */
    private void restorePathInfo() {
        pathVariables = savedPathVariables;
        pathVariableNames = savedPathVariableNames;
    }

    /** Were any path variables involved in the last computePath operation? */
    private boolean needPathInfo() {
        return !pathVariableNames.isEmpty();
    }

    /** Compute the path for the the file pointed to by <code>n</code> */
    private File computePath(Node n, boolean lookForTemplate) {
        if (n == null) {
            pathVariables = new HashMap();
            pathVariableNames = new ArrayList();
            metaPathVariables = new HashMap();
            metaPathVariableNames = new ArrayList();
            foundTemplate = false;
            return null;
        }
        File parentPath = computePath(n.getParentNode(), lookForTemplate);
        String pathVal = null;
        boolean isTemplate = false;
        if (n instanceof Element) {
            if (lookForTemplate) {
                pathVal = ((Element) n).getAttribute(TEMPLATE_PATH_ATTR);
                setTemplatePathVariables((Element) n);
            }
            if (XMLUtils.hasValue(pathVal)) {
                if ("none".equals(pathVal)) {
                    foundTemplate = isTemplate = false;
                    return null;
                } else isTemplate = true;
            } else pathVal = ((Element) n).getAttribute(PATH_ATTR);
        }
        if (!XMLUtils.hasValue(pathVal)) return parentPath;
        isDirectory = DIRECTORY_TAG_NAME.equals(((Element) n).getTagName());
        String defaultPath = ((Element) n).getAttribute(DEFAULT_PATH_ATTR);
        StringTokenizer tok = new StringTokenizer(pathVal, "[]", true);
        StringBuffer path = new StringBuffer();
        String token;
        PathVariable pathVar = null;
        boolean unknownsPresent = false, firstItem = true;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if ("[".equals(token)) {
                token = tok.nextToken();
                tok.nextToken();
                String impliedPath = null;
                if (firstItem && parentPath != null) impliedPath = parentPath.getPath();
                String defaultValue = null;
                if (firstItem && !tok.hasMoreTokens() && !isTemplate && XMLUtils.hasValue(defaultPath)) defaultValue = defaultPath;
                pathVar = getPathVariable(token, impliedPath, defaultValue);
                if (pathVar.isUnknown()) unknownsPresent = true; else path.append(pathVar.getValue());
            } else {
                path.append(token);
            }
            firstItem = false;
        }
        String selfPath = path.toString();
        if (unknownsPresent) {
            if (!isTemplate && XMLUtils.hasValue(defaultPath)) selfPath = defaultPath; else {
                foundTemplate = (foundTemplate || isTemplate);
                return null;
            }
        }
        File f = new File(selfPath);
        if (f.isAbsolute() || isTemplateURL(f)) foundTemplate = isTemplate; else {
            foundTemplate = (foundTemplate || isTemplate);
            if (parentPath == null) f = null; else f = new File(parentPath, selfPath);
        }
        return f;
    }

    private void setTemplatePathVariables(Element n) {
        String attrVal = n.getAttribute(TEMPLATE_VAL_ATTR);
        if (!XMLUtils.hasValue(attrVal)) return;
        StringTokenizer values = new StringTokenizer(attrVal, ";");
        while (values.hasMoreTokens()) setTemplatePathVariable(values.nextToken());
    }

    private void setTemplatePathVariable(String valSetting) {
        int bracePos = valSetting.indexOf(']');
        if (bracePos < 1) return;
        String valName = valSetting.substring(1, bracePos);
        int equalsPos = valSetting.indexOf('=', bracePos);
        if (equalsPos == -1) return;
        String setting = valSetting.substring(equalsPos + 1).trim();
        PathVariable pathVar = getPathVariable(valName);
        pathVar.dataName = null;
        pathVar.value = setting;
    }

    /** Lookup a cached PathVariable, or create one if no cached one exists.
     */
    private PathVariable getPathVariable(String mname, String impliedPath, String defaultValue) {
        String name = resolveMetaReferences(mname);
        PathVariable result = (PathVariable) pathVariables.get(name);
        if (result == null) {
            result = new PathVariable(name, mname, impliedPath, defaultValue);
            pathVariables.put(name, result);
            pathVariableNames.add(name);
        }
        return result;
    }

    private PathVariable getPathVariable(String name) {
        return getPathVariable(name, null, null);
    }

    /** Resolve meta references within <code>name</code> */
    private String resolveMetaReferences(String name) {
        int beg, end;
        String metaName;
        while ((beg = name.indexOf('{')) != -1) {
            end = name.indexOf('}', beg);
            metaName = name.substring(beg + 1, end);
            PathVariable pv = (PathVariable) metaPathVariables.get(metaName);
            if (pv == null) pv = new PathVariable(metaName, metaName, null, "");
            if (pv.isUnknown()) {
                metaPathVariables.put(metaName, pv);
                metaPathVariableNames.add(metaName);
            }
            name = name.substring(0, beg) + pv.getValue() + name.substring(end + 1);
        }
        name = name.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        while (name.indexOf("  ") != -1) name = StringUtils.findAndReplace(name, "  ", " ");
        return name;
    }

    /** Holds information about a data element referenced from a path.
     */
    class PathVariable {

        String metaName, dataName, value = null;

        public PathVariable(String name, String impliedPath) {
            this(name, name, impliedPath, null);
        }

        public PathVariable(String name) {
            this(name, name, null, null);
        }

        public PathVariable(String name, String metaName, String impliedPath, String defaultValue) {
            this.metaName = metaName;
            SaveableData val = null;
            DataRepository data = getDataRepository();
            if (name.startsWith("/")) {
                val = data.getSimpleValue(dataName = name);
            } else {
                StringBuffer prefix = new StringBuffer(getPrefix());
                val = data.getInheritableValue(prefix, name);
                if (val != null && !(val instanceof SimpleData)) val = val.getSimpleValue();
                dataName = data.createDataName(prefix.toString(), name);
            }
            String postedValue = getParameter(name);
            if (postedValue != null) {
                value = postedValue;
                if (pathStartsWith(value, impliedPath)) {
                    value = value.substring(impliedPath.length());
                    if (value.startsWith(File.separator)) value = value.substring(1);
                }
                if (!pathEqual(value, defaultValue)) {
                    data.userPutValue(dataName, StringData.create(value));
                }
            } else if (val instanceof SimpleData) value = ((SimpleData) val).format();
            if (isUnknown() && defaultValue != null) value = defaultValue;
        }

        public String getDataname() {
            return dataName;
        }

        private String getValue() {
            return value;
        }

        private boolean isUnknown() {
            return (value == null || value.length() == 0 || value.indexOf('?') != -1);
        }

        private boolean pathStartsWith(String path, String prefix) {
            if (path == null || prefix == null) return false;
            if (path.length() < prefix.length()) return false;
            return pathEqual(path.substring(0, prefix.length()), prefix);
        }

        private boolean pathEqual(String a, String b) {
            if (a == null || b == null) return false;
            File aa = new File(a), bb = new File(b);
            return aa.equals(bb);
        }

        private String displayName = null;

        private String commentText = null;

        public void lookupExtraInfo(Element e) {
            if (e == null) return;
            Document doc = e.getOwnerDocument();
            if (doc == null) return;
            e = (new FileFinder("[" + metaName + "]", doc)).file;
            if (e != null) {
                displayName = e.getAttribute("displayName");
                commentText = XMLUtils.getTextContents(e);
            }
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCommentText() {
            return commentText;
        }
    }

    /** Display an error message, stating that the XML document list does
     * not contain any file with the requested name.
     */
    private void sendNoSuchFileMessage(String filename) {
        super.writeHeader();
        out.print("<html><head><title>No such file</title></head>\n" + "<body><h1>No such file</h1>\n" + "None of the document description files\n" + "contain an entry for any file named &quot;");
        out.print(filename);
        out.print("&quot;.</body></html>");
    }

    private void sendCopyTemplateError(String message) {
        super.writeHeader();
        out.print("<html><head><title>Problem copying template</title></head>\n" + "<body><h1>Problem copying template</h1>\n");
        out.print(message);
        out.print("</body></html>");
    }

    /** When we are unable to locate a file, display a form requesting
     * information from the user.
     */
    private void displayNeedInfoForm(String filename, File file, boolean isTemplate, int reason, Element e) {
        super.writeHeader();
        out.print("<html><head><title>Enter File Information</title></head>\n" + "<body><h1>Enter File Information</h1>\n");
        if (file != null && reason != CREATE_CONFIRM) {
            out.print("The dashboard tried to find the ");
            out.print(isTemplate ? "<b>template</b>" : "file");
            out.print(" in the following location: <PRE>        ");
            out.print(file.getPath());
            out.println("</PRE>but no such file exists.<P>");
        }
        out.print("Please provide the following information to ");
        out.print(reason == CREATE_CONFIRM ? "create" : "help locate");
        out.print(" the '");
        out.print(filename);
        out.println(isTemplate ? "' template." : "'.");
        out.print("<form method='POST' action='");
        out.print((String) env.get("SCRIPT_PATH"));
        out.println("'><table>");
        for (int i = 0; i < pathVariableNames.size(); i++) {
            String varName = (String) pathVariableNames.get(i);
            PathVariable pathVar = getPathVariable(varName);
            if (pathVar.getDataname() == null) continue;
            pathVar.lookupExtraInfo(e);
            out.print("<tr><td valign='top'>");
            String displayName = pathVar.getDisplayName();
            if (!XMLUtils.hasValue(displayName)) displayName = varName;
            if (displayName.startsWith("/")) displayName = displayName.substring(1);
            out.print(TinyWebServer.encodeHtmlEntities(displayName));
            out.print("&nbsp;</td><td valign='top'>" + "<input size='40' type='text' name='");
            out.print(TinyWebServer.encodeHtmlEntities(varName));
            String value = pathVar.getValue();
            if (value != null) {
                out.print("' value='");
                out.print(TinyWebServer.encodeHtmlEntities(value));
            }
            out.print("'>");
            String comment = pathVar.getCommentText();
            if (XMLUtils.hasValue(comment)) {
                out.print("<br><i>");
                out.print(comment);
                out.print("</i><br>&nbsp;");
            }
            out.println("</td></tr>");
        }
        out.println("</table>");
        if (!(isTemplate == false && reason == MISSING_META)) out.print("<input type='hidden' name='" + CONFIRM_PARAM + "' " + "value='1'>\n");
        String pageCount = getParameter(PAGE_COUNT_PARAM);
        pageCount = (pageCount == null ? "x" : pageCount + "x");
        out.print("<input type='hidden' name='" + PAGE_COUNT_PARAM + "' value='");
        out.print(pageCount);
        out.print("'>\n" + "<input type='hidden' name='" + FILE_PARAM + "' value='");
        out.print(TinyWebServer.encodeHtmlEntities(filename));
        out.print("'>\n" + "<input type='submit' name='OK' value='OK'>\n" + "</form></body></html>\n");
    }

    private static final int MISSING_META = 0;

    private static final int MISSING_INFO = 1;

    private static final int CANNOT_LOCATE = 2;

    private static final int CREATE_CONFIRM = 3;

    protected static String docOpenSetting = Settings.getVal("extDoc.openMethod");

    /** an XML document describing the various files that can be served up
     *  by this CGI script.
     */
    protected static Hashtable documentMap = new Hashtable();

    protected Document getDocumentTree(String url) throws IOException {
        Document result = null;
        if (parameters.get("init") == null) result = (Document) documentMap.get(url);
        if (result == null) try {
            result = XMLUtils.parse(new ByteArrayInputStream(getRequest(url, true)));
            documentMap.put(url, result);
        } catch (SAXException se) {
            return null;
        }
        return result;
    }

    /** Find a file in the document list.
     * @param name the name of the file to find
     * @return the XML element corresponding to the named document.
     */
    protected Element findFile(String name) throws IOException {
        DataRepository data = getDataRepository();
        String pfx = getPrefix();
        if (pfx == null) pfx = "/";
        StringBuffer prefix = new StringBuffer(pfx);
        ListData list;
        Element result = null;
        SaveableData val;
        for (val = data.getInheritableValue(prefix, FILE_XML_DATANAME); val != null; val = data.getInheritableValue(chop(prefix), FILE_XML_DATANAME)) {
            if (val != null && !(val instanceof SimpleData)) val = val.getSimpleValue();
            if (val instanceof StringData) list = ((StringData) val).asList(); else if (val instanceof ListData) list = (ListData) val; else list = null;
            if (list != null) for (int i = 0; i < list.size(); i++) {
                String url = (String) list.get(i);
                Document docList = getDocumentTree(url);
                if (docList != null) {
                    result = (new FileFinder(name, docList)).file;
                    if (result != null) return result;
                }
            }
            if (prefix.length() == 0) break;
        }
        return null;
    }

    private StringBuffer chop(StringBuffer buf) {
        int slashPos = buf.toString().lastIndexOf('/');
        buf.setLength(slashPos == -1 ? 0 : slashPos);
        return buf;
    }

    class FileFinder extends XMLDepthFirstIterator {

        String name;

        Element file = null;

        public FileFinder(String name, Document docTree) {
            this.name = name;
            run(docTree);
        }

        public void caseElement(Element e, List path) {
            if (name.equalsIgnoreCase(e.getAttribute(NAME_ATTR)) || name.equalsIgnoreCase(e.getAttribute(TEMPLATE_PATH_ATTR))) file = e;
        }
    }
}
