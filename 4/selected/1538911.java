package com.jcorporate.expresso.ext.controller;

import com.jcorporate.expresso.core.controller.ControllerException;
import com.jcorporate.expresso.core.controller.ControllerRequest;
import com.jcorporate.expresso.core.controller.ControllerResponse;
import com.jcorporate.expresso.core.controller.DBController;
import com.jcorporate.expresso.core.controller.ServletControllerRequest;
import com.jcorporate.expresso.core.controller.State;
import com.jcorporate.expresso.core.misc.ConfigManager;
import com.jcorporate.expresso.core.security.filters.Filter;
import org.apache.log4j.Logger;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This controller is used to serve up text files.  It helps provide a particularly
 * useful mechanism for tutorial purposes.
 * <p><b>PLEASE NOTE!</b>: It is DEFINITELY not recommended that this controller
 * should be opened up to anybody but the Administrator in a production
 * environment!  It could be used by malicious attackers to cause the system
 * to cough up password files, and many other things that they would not normally
 * have access to </p>
 *
 * @author Michael Rimov - Adapted from code created by Peter Pilgrim.
 * @version $Revision: 3 $ on  $Date: 2006-03-01 06:17:08 -0500 (Wed, 01 Mar 2006) $
 */
public class ServeTextFile extends DBController {

    /**
     * List of locations to search for this file
     */
    private static Vector rootDirList = new Vector();

    /**
     * Our Log4J logger.  See <a href="http://jakarta.apache.org/log4j/">The Log4j Website</a>
     * for more information.
     */
    private static transient Logger log = Logger.getLogger("expresso.ext.controller.ServeTextFile");

    /**
     * A filter that efficiently removes (hopefully) all URL trickery that could
     * result in unnecessary security problems
     */
    private static transient Filter fileNameFilter = null;

    /**
     * A filter that efficiently goes through a string and color codes the
     * appropriate stuff for nice eye candy
     */
    private static transient Filter javaCodeFilter = null;

    public static final String DEFAULT_RESERVED_KEYWORD_COLOR = "#9900CC";

    public static final String DEFAULT_PRIMITIVE_VAR_COLOR = "#008000";

    public static final String DEFAULT_SPECIAL_KEYWORD_COLOR = "#4682B4";

    public static final String DEFAULT_SINGLE_QUOTE_COLOR = "#0066FF";

    public static final String DEFAULT_DOUBLE_QUOTE_COLOR = "#0000CC";

    public static final String DEFAULT_CSTYLE_COMMENT_COLOR = "#CC0033";

    public static final String DEFAULT_CPLUS_COMMENT_COLOR = "B22222";

    public static final String DEFAULT_DECIMAL_NUMBER_COLOR = "#996600";

    protected static final String reservedKeywordColor = DEFAULT_RESERVED_KEYWORD_COLOR;

    protected static final String primitiveVarColor = DEFAULT_PRIMITIVE_VAR_COLOR;

    protected static final String specialKeywordColor = DEFAULT_SPECIAL_KEYWORD_COLOR;

    protected static final String singleQuoteColor = DEFAULT_SINGLE_QUOTE_COLOR;

    protected static final String doubleQuoteColor = DEFAULT_DOUBLE_QUOTE_COLOR;

    protected static final String cstyleCommentColor = DEFAULT_CSTYLE_COMMENT_COLOR;

    protected static final String cplusCommentColor = DEFAULT_CPLUS_COMMENT_COLOR;

    protected static final String decimalNumberColor = DEFAULT_DECIMAL_NUMBER_COLOR;

    /** Class used a structure to pass info around calls */
    class Parameters {

        HttpServletRequest req;

        HttpServletResponse res;

        HttpSession session;

        String inputFilename;

        String filename;

        PrintStream out;
    }

    /**
     * A simple structure class to map a Java
     * reserved keyword to a HTML colour code
     */
    static class ReservedWord {

        String keyword;

        String htmlcolor;

        ReservedWord(String keyword, String htmlcolor) {
            this.keyword = keyword;
            this.htmlcolor = htmlcolor;
        }
    }

    protected static final ServeTextFile.ReservedWord[] java_reserved_keywords = { new ReservedWord("class", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("interface", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("extends", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("implements", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("goto", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("for", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("return", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("if", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("then", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("else", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("while", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("do", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("switch", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("case", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("default", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("instanceof", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("package", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("import", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("public", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("protected", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("private", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("super", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("new", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("this", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("try", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("catch", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("throw", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("throws", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("final", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("abstract", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("native", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("static", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("transient", DEFAULT_RESERVED_KEYWORD_COLOR), new ReservedWord("void", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("boolean", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("char", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("int", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("short", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("long", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("float", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("double", DEFAULT_PRIMITIVE_VAR_COLOR), new ReservedWord("null", DEFAULT_SPECIAL_KEYWORD_COLOR), new ReservedWord("true", DEFAULT_SPECIAL_KEYWORD_COLOR), new ReservedWord("false", DEFAULT_SPECIAL_KEYWORD_COLOR) };

    protected static Hashtable fast_keyword_map = null;

    public ServeTextFile() {
        State s = new State("serveTextFile", "Serve A Text File");
        s.addRequiredParameter("filename");
        this.addState(s);
        s = new State("serveJavaFile", "Serve a Java File");
        s.addRequiredParameter("filename");
        this.addState(s);
        String baseRootDir = ConfigManager.getWebAppDir() + "/WEB-INF/src";
        rootDirList.add(baseRootDir);
        baseRootDir = ConfigManager.getWebAppDir();
        rootDirList.add(baseRootDir);
        if (fileNameFilter == null) {
            fileNameFilter = new Filter(new String[] { "|", "..", ">", "<" }, new String[] { "", "", "", "" });
        }
        if (fast_keyword_map == null) {
            fast_keyword_map = new Hashtable(java_reserved_keywords.length);
            for (int k = 0; k < java_reserved_keywords.length; ++k) {
                fast_keyword_map.put(java_reserved_keywords[k].keyword, java_reserved_keywords[k]);
            }
        }
        this.setInitialState("serveJavaFile");
        this.setSchema(com.jcorporate.expresso.core.ExpressoSchema.class);
    }

    /**
     * Serves up a basic text file as specified by the parameter.  Our goal
     * here is to provide something that is reasonably secure in that we remove
     * all URL trickery.
     * @param request The <code>ControllerRequest</code> Object
     * @param response The <code>ControllerResponse</code> Object
     * @return ControllerResponse
     * @throws ControllerException upon error
     */
    protected ControllerResponse runServeTextFileState(ControllerRequest request, ControllerResponse response) throws ControllerException {
        ServletControllerRequest servRequest;
        try {
            servRequest = (ServletControllerRequest) request;
        } catch (ClassCastException ex) {
            throw new ControllerException("This controller must be run within only a http environment");
        }
        HttpServletRequest req = (HttpServletRequest) servRequest.getServletRequest();
        HttpServletResponse res = (HttpServletResponse) servRequest.getServletResponse();
        Servlet servlet = servRequest.getCallingServlet();
        String fileSep = System.getProperty("file.separator");
        String inputFilename = req.getParameter("filename");
        try {
            if (inputFilename == null) {
                throw new ControllerException("filename parameter is not set.");
            }
            inputFilename = fileNameFilter.stripFilter(inputFilename);
            if (inputFilename.length() == 0) {
                throw new ControllerException("filename parameter is not set.");
            }
            if (!(inputFilename.endsWith(".java") || inputFilename.endsWith(".cpp") || inputFilename.endsWith(".cc") || inputFilename.endsWith(".jpg") || inputFilename.endsWith(".jpeg") || inputFilename.endsWith(".png") || inputFilename.endsWith(".jsp") || inputFilename.endsWith(".txt") || inputFilename.endsWith(".wm"))) {
                throw new ControllerException("Sorry, it is forbidden to serve this type of file.");
            }
            boolean fileWasServed = false;
            for (int k = 0; k < rootDirList.size(); ++k) {
                String rootDir = (String) rootDirList.elementAt(k);
                File file = new File(rootDir, inputFilename);
                String filename = file.getPath();
                if (file.exists()) {
                    if (!file.canRead()) {
                        throw new IOException("not readable filename:`" + inputFilename + "'");
                    }
                    if (file.isDirectory()) {
                        throw new IOException("cannot serve a directory as filename:`" + inputFilename + "'");
                    }
                    fileWasServed = true;
                    response.setCustomResponse(true);
                    String contentType = servlet.getServletConfig().getServletContext().getMimeType(filename);
                    if (contentType == null) {
                        contentType = "text/plain";
                    }
                    if (log.isInfoEnabled()) {
                        log.info("BasicFileServeServlet: Serving file:`" + filename + "'  type:" + contentType);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
                    returnFile(filename, baos);
                    OutputStream out = res.getOutputStream();
                    res.setContentType(contentType);
                    baos.writeTo(out);
                    out.flush();
                    out.close();
                    break;
                }
            }
            if (!fileWasServed) {
                throw new FileNotFoundException("no such file: `" + inputFilename + "'");
            }
        } catch (FileNotFoundException ex) {
            log.error("FileNotFoundException locating file file", ex);
            throw new ControllerException("FileNotFoundException Error transferring file", ex);
        } catch (java.io.IOException ioe) {
            log.error("I/O Error transferring file", ioe);
            throw new ControllerException("I/O Error transferring file", ioe);
        }
        return response;
    }

    protected void returnFile(String filename, OutputStream out) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * Serves up a java source file as specified by the parameter.  This
     * state is different from that of servTextFile in that it color codes
     * all the keywords, etc in the java file.
     *
     * Our goal here is to provide something that is reasonably secure in that we remove
     * all URL trickery, and we also only serve up NON source code files.  Again,
     * this should be NOT used in a production environment and only exists for
     * teaching purposes.
     *
     * @param request The <code>ControllerRequest</code> Object
     * @param response The <code>ControllerResponse</code> Object
     * @return ControllerResponse
     * @throws ControllerException upon error
     */
    protected ControllerResponse runServeJavaFileState(ControllerRequest request, ControllerResponse response) throws ControllerException {
        ServletControllerRequest servRequest;
        try {
            servRequest = (ServletControllerRequest) request;
        } catch (ClassCastException ex) {
            throw new ControllerException("This controller must be run within only a http environment");
        }
        HttpServletRequest req = (HttpServletRequest) servRequest.getServletRequest();
        HttpSession session = req.getSession(true);
        HttpServletResponse res = (HttpServletResponse) servRequest.getServletResponse();
        Servlet servlet = servRequest.getCallingServlet();
        String fileSep = System.getProperty("file.separator");
        String inputFilename = req.getParameter("filename");
        try {
            if (inputFilename == null) {
                throw new ControllerException("filename parameter is not set.");
            }
            if (inputFilename == null) {
                throw new ControllerException("filename parameter is not set.");
            }
            inputFilename = fileNameFilter.stripFilter(inputFilename);
            if (inputFilename.length() == 0) {
                throw new ControllerException("filename parameter is not set.");
            }
            if (!(inputFilename.endsWith(".java"))) {
                throw new ControllerException("Sorry, it is forbidden to serve this type of file. This servlet only serves Java source files!");
            }
            boolean fileWasServed = false;
            for (int k = 0; k < rootDirList.size(); ++k) {
                String rootDir = (String) rootDirList.elementAt(k);
                File file = new File(rootDir, inputFilename);
                String filename = file.getPath();
                if (file.exists()) {
                    if (!file.canRead()) {
                        throw new IOException("not readable filename:`" + inputFilename + "'");
                    }
                    if (file.isDirectory()) {
                        throw new IOException("cannot serve a directory as filename:`" + inputFilename + "'");
                    }
                    fileWasServed = true;
                    response.setCustomResponse(true);
                    String contentType = "text/html";
                    if (log.isInfoEnabled()) {
                        log.info("JavaFileServeServlet: Serving file:`" + filename + "'  type:" + contentType);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(32678);
                    PrintStream prs = new PrintStream(baos);
                    Parameters params = new Parameters();
                    params.req = req;
                    params.res = res;
                    params.session = session;
                    params.inputFilename = inputFilename;
                    params.filename = filename;
                    params.out = prs;
                    returnHTMLFormattedFile(params);
                    prs.flush();
                    OutputStream out = res.getOutputStream();
                    res.setContentType(contentType);
                    baos.writeTo(out);
                    out.flush();
                    out.close();
                    break;
                }
            }
            if (!fileWasServed) {
                throw new FileNotFoundException("no such file: `" + inputFilename + "'");
            }
        } catch (FileNotFoundException ex) {
            log.error("FileNotFoundException locating file file", ex);
            throw new ControllerException("FileNotFoundException Error transferring file", ex);
        } catch (java.io.IOException ioe) {
            log.error("I/O Error transferring file", ioe);
            throw new ControllerException("I/O Error transferring file", ioe);
        }
        return response;
    }

    public void returnHTMLFormattedFile(Parameters params) throws FileNotFoundException, IOException {
        writeHeader(params);
        writeContent(params);
        writeFooter(params);
    }

    public void writeHeader(Parameters params) throws FileNotFoundException, IOException {
        PrintStream out = params.out;
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Java Source File: `" + params.inputFilename + "'</title>");
        out.println("<meta name=\"generator\" value=\"" + getClass().getName() + "\" >");
        out.println("<meta name=\"published_date\" value=\"" + new Date() + "\" >");
        out.println("</head>");
        out.println("<body bgcolor=\"#FFFFFF\" >");
        File file = new File(params.filename);
        out.println("<p><font face=\"Lucida, Georgia, Arial,Helvetica\" size=\"-1\" color=\"#000000\" >filename: <b>" + params.inputFilename + "</b><br>");
        out.println("file size: <b>" + file.length() + "</b><br>");
        out.println("last modified: <b>" + new Date(file.lastModified()) + "</b><br>");
        out.println("</font></p>");
    }

    protected void writeFooter(Parameters params) throws FileNotFoundException, IOException {
        PrintStream out = params.out;
        out.println("</body>");
        out.println("</html>");
    }

    protected void writeContent(Parameters params) throws FileNotFoundException, IOException {
        PrintStream out = params.out;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        PushbackInputStream pis = null;
        boolean insideDQuote = false;
        boolean insideSQuote = false;
        boolean cstyleComment = false;
        try {
            fis = new FileInputStream(params.filename);
            bis = new BufferedInputStream(fis, 8192);
            pis = new PushbackInputStream(bis, 256);
            out.println("<pre>");
            out.println("<font face=\"Courier New, Monospace, Helvetica, San-serif\" size=\"+0\" color=\"#000000\" >");
            int c1 = pis.read();
            while (c1 >= 0) {
                while (Character.isWhitespace((char) c1)) {
                    out.print((char) c1);
                    c1 = pis.read();
                }
                if (c1 < 0) {
                    break;
                }
                if (log.isDebugEnabled()) {
                    System.out.print((char) c1);
                }
                if (c1 == '/') {
                    int c2 = pis.read();
                    if (c2 == '/') {
                        out.print("<font color=\"" + cplusCommentColor + "\">//");
                        if (log.isDebugEnabled()) {
                            System.out.println("C++ comment");
                        }
                        c2 = pis.read();
                        while (c2 != -1 && c2 != '\n') {
                            out.print(substituteEntity((char) c2));
                            c2 = pis.read();
                        }
                        out.print("</font>\n");
                        c1 = pis.read();
                        continue;
                    } else if (c2 == '*') {
                        if (log.isDebugEnabled()) {
                            System.out.println("C comment start");
                        }
                        cstyleComment = true;
                        out.print("<font color=\"" + cstyleCommentColor + "\">/*");
                        c1 = pis.read();
                        continue;
                    } else {
                        pis.unread(c2);
                    }
                } else if (c1 == '*') {
                    int c2 = pis.read();
                    if (c2 == '/') {
                        if (log.isDebugEnabled()) {
                            System.out.println("C comment end");
                        }
                        cstyleComment = false;
                        out.print("*/</font>");
                        c1 = pis.read();
                        continue;
                    } else {
                        pis.unread(c2);
                    }
                }
                if (c1 == '\"' && !cstyleComment) {
                    if (!insideDQuote) {
                        out.print("<font color=\"" + doubleQuoteColor + "\">&quot;");
                    } else {
                        out.print("&quot;</font>");
                    }
                    insideDQuote = !insideDQuote;
                    if (log.isDebugEnabled()) {
                        System.out.println("double quotes:" + insideDQuote);
                    }
                } else if (c1 == '\'' && !cstyleComment && !insideDQuote) {
                    if (!insideSQuote) {
                        out.print("<font color=\"" + singleQuoteColor + "\">&#039;");
                    } else {
                        out.print("&#039;</font>");
                    }
                    insideSQuote = !insideSQuote;
                    if (log.isDebugEnabled()) {
                        System.out.println("single quotes:" + insideSQuote);
                    }
                } else if (Character.isDigit((char) c1) && !cstyleComment && !insideSQuote && !insideDQuote) {
                    boolean valid = true;
                    StringBuffer tokenBuffer = new StringBuffer();
                    tokenBuffer.append((char) c1);
                    int c2 = pis.read();
                    while (Character.isDigit((char) c2)) {
                        tokenBuffer.append((char) c2);
                        c2 = pis.read();
                    }
                    pis.mark(256);
                    if (log.isDebugEnabled()) {
                        System.out.println("(1) token=`" + tokenBuffer.toString() + "' (c2:" + c2 + "<" + (char) c2 + ")");
                    }
                    if (c2 == '.') {
                        tokenBuffer.append((char) c2);
                        c2 = pis.read();
                        if (log.isDebugEnabled()) {
                            System.out.println("(2) token=`" + tokenBuffer.toString() + "' (c2:" + c2 + "<" + (char) c2 + ")");
                        }
                        if (c2 == 'e' || c2 == 'E') {
                            tokenBuffer.append((char) c2);
                            c2 = pis.read();
                            if (log.isDebugEnabled()) {
                                System.out.println("(3) token=`" + tokenBuffer.toString() + "' (c2:" + c2 + "<" + (char) c2 + ")");
                            }
                        }
                        System.out.println("(4) token=`" + tokenBuffer.toString() + "' (c2:" + c2 + "<" + (char) c2 + ")");
                        if (c2 == '+' || c2 == '-') {
                            tokenBuffer.append((char) c2);
                            c2 = pis.read();
                        }
                        if (Character.isDigit((char) c2)) {
                            while (Character.isDigit((char) c2)) {
                                tokenBuffer.append((char) c2);
                                c2 = pis.read();
                            }
                        } else {
                            valid = false;
                            pis.reset();
                        }
                    }
                    pis.unread(c2);
                    if (valid) {
                        out.print("<font color=\"" + decimalNumberColor + "\" >");
                    }
                    out.print(tokenBuffer.toString());
                    if (valid) {
                        out.print("</font>");
                    }
                } else if (Character.isLetter((char) c1) && !cstyleComment && !insideSQuote && !insideDQuote) {
                    StringBuffer tokenBuffer = new StringBuffer();
                    tokenBuffer.append((char) c1);
                    int c2 = pis.read();
                    while (Character.isLetter((char) c2)) {
                        tokenBuffer.append((char) c2);
                        c2 = pis.read();
                    }
                    pis.unread(c2);
                    boolean wasMatched = false;
                    String token = tokenBuffer.toString();
                    if (log.isDebugEnabled()) {
                        System.out.println("<" + token + ">(" + c2 + "/`" + (char) c2 + "')");
                    }
                    ReservedWord reservedWord = (ReservedWord) fast_keyword_map.get(token);
                    if (reservedWord != null) {
                        wasMatched = true;
                    }
                    if (wasMatched) {
                        out.print("<font color=\"" + reservedWord.htmlcolor + "\" >");
                    }
                    out.print(token);
                    if (wasMatched) {
                        out.print("</font>");
                    }
                } else {
                    out.print(substituteEntity((char) c1));
                }
                c1 = pis.read();
            }
            out.println("</font>");
            out.println("</pre>");
        } finally {
            if (pis != null) {
                pis.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
    }

    protected static final String substituteEntity(char c9) {
        switch(c9) {
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '&':
                return "&amp;";
            case '\"':
                return "&quot;";
        }
        return new String(new char[] { c9 });
    }

    public String getTitle() {
        return "Serve Text File";
    }
}
