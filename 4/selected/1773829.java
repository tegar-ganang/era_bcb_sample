package org.simpleframework.tool.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.security.KeyStore;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.wicket.util.upload.DiskFileItemFactory;
import org.apache.wicket.util.upload.FileItem;
import org.apache.wicket.util.upload.FileItemFactory;
import org.apache.wicket.util.upload.FileUpload;
import org.simpleframework.http.Part;
import org.simpleframework.http.Principal;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.http.parse.PrincipalParser;
import org.simpleframework.http.resource.Context;
import org.simpleframework.http.resource.FileContext;
import org.simpleframework.tool.code.java.FindJavaSources;
import org.simpleframework.tool.code.sql.FindStoredProcs;
import org.simpleframework.tool.cp.Config;
import org.simpleframework.tool.cp.Searchable;
import org.simpleframework.tool.cp.ServerDetails;
import org.simpleframework.tool.html.FileSystemDirectory;
import org.simpleframework.tool.svn.Change;
import org.simpleframework.tool.svn.Info;
import org.simpleframework.tool.svn.Repository;
import org.simpleframework.tool.svn.Scheme;
import org.simpleframework.tool.svn.Subversion;
import org.simpleframework.tool.util.Digest;
import org.simpleframework.tool.util.LineStripper;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.simpleframework.xml.core.Persister;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class SearchServer implements Container {

    private static final Comparator<String> LONGEST_FIRST = new Comparator<String>() {

        public int compare(String o1, String o2) {
            Integer l1 = o1.length();
            Integer l2 = o2.length();
            return l2.compareTo(l1);
        }
    };

    public static String getRelativeURL(File basePath, File path) throws IOException {
        String scanPath = basePath.getCanonicalPath();
        String namePath = path.getCanonicalPath();
        String relativePath = namePath.substring(scanPath.length() + 1, namePath.length());
        return relativePath.replace('\\', '/');
    }

    public static String matchesFirst(String regex, String line) {
        List<String> list = matches(regex, line);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public static List<String> matches(String regex, String line) {
        Matcher matcher = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE).matcher(line);
        List<String> list = new ArrayList<String>();
        if (matcher.matches()) {
            int length = matcher.groupCount();
            for (int i = 1; i <= length; i++) {
                list.add(matcher.group(i));
            }
        }
        return list;
    }

    public File getJavaIndexFile(File root, String index) {
        return new File(root, "java_index.xml");
    }

    public File getStoredProcIndexFile(File root, String index) {
        return new File(root, "sql_index.xml");
    }

    public static String format(String unformattedXml) {
        try {
            org.w3c.dom.Document document = parseXmlFile(unformattedXml);
            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(600);
            format.setIndenting(true);
            format.setIndent(3);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static org.w3c.dom.Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeHtml(String value) {
        return value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    static String getJavaScript(String docName) {
        if (docName.endsWith(".java")) {
            return " class='sh_java'";
        } else if (docName.endsWith(".xml")) {
            return " class='sh_xml'";
        } else if (docName.endsWith(".sql")) {
            return " class='sh_sql'";
        } else if (docName.endsWith(".cs")) {
            return " class='sh_csharp'";
        } else if (docName.endsWith(".properties")) {
            return " class='sh_properties'";
        }
        return "";
    }

    private final Map<File, MappedByteBuffer> fileCache;

    private final Persister persister;

    private final SearchEngineManager searchEngine;

    private final Context context;

    private final Config config;

    private final Executor pool;

    public SearchServer(Config config, SearchEngineManager searchEngine, Persister persister) {
        this.fileCache = new ConcurrentHashMap<File, MappedByteBuffer>();
        this.pool = Executors.newFixedThreadPool(10);
        this.context = new FileContext();
        this.persister = persister;
        this.searchEngine = searchEngine;
        this.config = config;
    }

    public void handle(final Request req, final Response resp) {
        pool.execute(new Runnable() {

            public void run() {
                resp.setDate("Date", System.currentTimeMillis());
                resp.set("Server", SearchServer.class.getName());
                service(req, resp);
            }
        });
    }

    public void service(Request req, Response resp) {
        PrintStream out = null;
        try {
            out = resp.getPrintStream(8192);
            String env = req.getParameter("env");
            String regex = req.getParameter("regex");
            String deep = req.getParameter("deep");
            String term = req.getParameter("term");
            String index = req.getParameter("index");
            String refresh = req.getParameter("refresh");
            String searcher = req.getParameter("searcher");
            String grep = req.getParameter("grep");
            String fiServerDetails = req.getParameter("fi_server_details");
            String serverDetails = req.getParameter("server_details");
            String hostDetails = req.getParameter("host_details");
            String name = req.getParameter("name");
            String show = req.getParameter("show");
            String path = req.getPath().getPath();
            int page = req.getForm().getInteger("page");
            if (path.startsWith("/fs")) {
                String fsPath = path.replaceAll("^/fs", "");
                File realPath = new File("C:\\", fsPath.replace('/', File.separatorChar));
                if (realPath.isDirectory()) {
                    out.write(FileSystemDirectory.getContents(new File("c:\\"), fsPath, "/fs"));
                } else {
                    resp.set("Cache", "no-cache");
                    FileInputStream fin = new FileInputStream(realPath);
                    FileChannel channel = fin.getChannel();
                    WritableByteChannel channelOut = resp.getByteChannel();
                    channel.transferTo(0, realPath.length(), channelOut);
                    channel.close();
                    fin.close();
                    System.err.println("Serving " + path + " as " + realPath.getCanonicalPath());
                }
            } else if (path.startsWith("/files/")) {
                String[] segments = req.getPath().getSegments();
                boolean done = false;
                if (segments.length > 1) {
                    String realPath = req.getPath().getPath(1);
                    File file = context.getFile(realPath);
                    if (file.isFile()) {
                        resp.set("Content-Type", context.getContentType(realPath));
                        FileInputStream fin = new FileInputStream(file);
                        FileChannel channel = fin.getChannel();
                        WritableByteChannel channelOut = resp.getByteChannel();
                        long start = System.currentTimeMillis();
                        channel.transferTo(0, realPath.length(), channelOut);
                        channel.close();
                        fin.close();
                        System.err.println("Time take to write [" + realPath + "] was [" + (System.currentTimeMillis() - start) + "] of size [" + file.length() + "]");
                        done = true;
                    }
                }
                if (!done) {
                    resp.set("Content-Type", "text/plain");
                    out.println("Can not serve directory: path");
                }
            } else if (path.startsWith("/upload")) {
                FileItemFactory factory = new DiskFileItemFactory();
                FileUpload upload = new FileUpload(factory);
                RequestAdapter adapter = new RequestAdapter(req);
                List<FileItem> list = upload.parseRequest(adapter);
                Map<String, FileItem> map = new HashMap<String, FileItem>();
                for (FileItem entry : list) {
                    String fileName = entry.getFieldName();
                    map.put(fileName, entry);
                }
                resp.set("Content-Type", "text/html");
                out.println("<html>");
                out.println("<body>");
                for (int i = 0; i < 10; i++) {
                    Part file = req.getPart("datafile" + (i + 1));
                    if (file != null && file.isFile()) {
                        String partName = file.getName();
                        String partFileName = file.getFileName();
                        File partFile = new File(partFileName);
                        FileItem item = map.get(partName);
                        InputStream in = file.getInputStream();
                        String fileName = file.getFileName().replaceAll("\\\\", "_").replaceAll(":", "_");
                        File filePath = new File(fileName);
                        OutputStream fileOut = new FileOutputStream(filePath);
                        byte[] chunk = new byte[8192];
                        int count = 0;
                        while ((count = in.read(chunk)) != -1) {
                            fileOut.write(chunk, 0, count);
                        }
                        fileOut.close();
                        in.close();
                        out.println("<table border='1'>");
                        out.println("<tr><td><b>File</b></td><td>");
                        out.println(filePath.getCanonicalPath());
                        out.println("</tr></td>");
                        out.println("<tr><td><b>Size</b></td><td>");
                        out.println(filePath.length());
                        out.println("</tr></td>");
                        out.println("<tr><td><b>MD5</b></td><td>");
                        out.println(Digest.getSignature(Digest.Algorithm.MD5, file.getInputStream()));
                        out.println("<br>");
                        out.println(Digest.getSignature(Digest.Algorithm.MD5, item.getInputStream()));
                        if (partFile.exists()) {
                            out.println("<br>");
                            out.println(Digest.getSignature(Digest.Algorithm.MD5, new FileInputStream(partFile)));
                        }
                        out.println("</tr></td>");
                        out.println("<tr><td><b>SHA1</b></td><td>");
                        out.println(Digest.getSignature(Digest.Algorithm.SHA1, file.getInputStream()));
                        out.println("<br>");
                        out.println(Digest.getSignature(Digest.Algorithm.SHA1, item.getInputStream()));
                        if (partFile.exists()) {
                            out.println("<br>");
                            out.println(Digest.getSignature(Digest.Algorithm.SHA1, new FileInputStream(partFile)));
                        }
                        out.println("</tr></td>");
                        out.println("<tr><td><b>Header</b></td><td><pre>");
                        out.println(file.toString().trim());
                        out.println("</pre></tr></td>");
                        if (partFileName.toLowerCase().endsWith(".xml")) {
                            String xml = file.getContent();
                            String formatted = format(xml);
                            String fileFormatName = fileName + ".formatted";
                            File fileFormatOut = new File(fileFormatName);
                            FileOutputStream formatOut = new FileOutputStream(fileFormatOut);
                            formatOut.write(formatted.getBytes("UTF-8"));
                            out.println("<tr><td><b>Formatted XML</b></td><td><pre>");
                            out.println("<a href='/" + (fileFormatName) + "'>" + partFileName + "</a>");
                            out.println("</pre></tr></td>");
                            formatOut.close();
                        }
                        out.println("<table>");
                    }
                }
                out.println("</body>");
                out.println("</html>");
            } else if (path.startsWith("/sql/") && index != null && searcher != null) {
                String file = req.getPath().getPath(1);
                File root = searchEngine.index(searcher).getRoot();
                SearchEngine engine = searchEngine.index(searcher);
                File indexFile = getStoredProcIndexFile(engine.getRoot(), index);
                File search = new File(root, "cpsql");
                File source = new File(root, file.replace('/', File.separatorChar));
                FindStoredProcs.StoredProcProject storedProcProj = FindStoredProcs.getStoredProcProject(search, indexFile);
                FindStoredProcs.StoredProc proc = storedProcProj.getStoredProc(source.getName());
                resp.set("Content-Type", "text/html");
                out.println("<html>");
                out.println("<body><pre>");
                for (String procName : proc.getReferences()) {
                    FindStoredProcs.StoredProc theProc = storedProcProj.getStoredProc(procName);
                    if (theProc != null) {
                        String url = getRelativeURL(root, theProc.getFile());
                        out.println("<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'><b>" + theProc.getName() + "</b>");
                    }
                }
                out.println("</pre></body>");
                out.println("</html>");
            } else if (show != null && index != null && searcher != null) {
                String authentication = req.getValue("Authorization");
                if (authentication == null) {
                    resp.setCode(401);
                    resp.setText("Authorization Required");
                    resp.set("Content-Type", "text/html");
                    resp.set("WWW-Authenticate", "Basic realm=\"DTS Subversion Repository\"");
                    out.println("<html>");
                    out.println("<head>");
                    out.println("401 Authorization Required");
                    out.println("</head>");
                    out.println("<body>");
                    out.println("<h1>401 Authorization Required</h1>");
                    out.println("</body>");
                    out.println("</html>");
                } else {
                    resp.set("Content-Type", "text/html");
                    Principal principal = new PrincipalParser(authentication);
                    String file = show;
                    SearchEngine engine = searchEngine.index(searcher);
                    File root = engine.getRoot();
                    File javaIndexFile = getJavaIndexFile(root, index);
                    File storedProcIndexFile = getStoredProcIndexFile(root, index);
                    File sql = new File(root, "cpsql");
                    File source = new File(root, file.replace('/', File.separatorChar));
                    File javaSource = new File(root, file.replace('/', File.separatorChar));
                    File canonical = source.getCanonicalFile();
                    Repository repository = Subversion.login(Scheme.HTTP, principal.getName(), principal.getPassword());
                    Info info = null;
                    try {
                        info = repository.info(canonical);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    List<Change> logMessages = new ArrayList<Change>();
                    try {
                        logMessages = repository.log(canonical);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    FileInputStream in = new FileInputStream(canonical);
                    List<String> lines = LineStripper.stripLines(in);
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<!-- username='" + principal.getName() + "' password='" + principal.getPassword() + "' -->");
                    out.println("<link rel='stylesheet' type='text/css' href='style.css'>");
                    out.println("<script src='highlight.js'></script>");
                    out.println("</head>");
                    out.println("<body onload=\"sh_highlightDocument('lang/', '.js')\">");
                    if (info != null) {
                        out.println("<table border='1'>");
                        out.printf("<tr><td bgcolor=\"#C4C4C4\"><tt>Author</tt></td><td><tt>" + info.author + "</tt></td></tr>");
                        out.printf("<tr><td bgcolor=\"#C4C4C4\"><tt>Version</tt></td><td><tt>" + info.version + "</tt></td></tr>");
                        out.printf("<tr><td bgcolor=\"#C4C4C4\"><tt>URL</tt></td><td><tt>" + info.location + "</tt></td></tr>");
                        out.printf("<tr><td bgcolor=\"#C4C4C4\"><tt>Path</tt></td><td><tt>" + canonical + "</tt></td></tr>");
                        out.println("</table>");
                    }
                    out.println("<table border='1''>");
                    out.println("<tr>");
                    out.println("<td valign='top' bgcolor=\"#C4C4C4\"><pre>");
                    FindStoredProcs.StoredProcProject storedProcProj = FindStoredProcs.getStoredProcProject(sql, storedProcIndexFile);
                    FindStoredProcs.StoredProc storedProc = null;
                    FindJavaSources.JavaProject project = null;
                    FindJavaSources.JavaClass javaClass = null;
                    List<FindJavaSources.JavaClass> importList = null;
                    if (file.endsWith(".sql")) {
                        storedProc = storedProcProj.getStoredProc(canonical.getName());
                    } else if (file.endsWith(".java")) {
                        project = FindJavaSources.getProject(root, javaIndexFile);
                        javaClass = project.getClass(source);
                        importList = project.getImports(javaSource);
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        out.println(i);
                    }
                    out.println("</pre></td>");
                    out.print("<td valign='top'><pre");
                    out.print(getJavaScript(file));
                    out.println(">");
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String escaped = escapeHtml(line);
                        if (project != null) {
                            for (FindJavaSources.JavaClass entry : importList) {
                                String className = entry.getClassName();
                                String fullyQualifiedName = entry.getFullyQualifiedName();
                                if (line.startsWith("import") && line.indexOf(fullyQualifiedName) > -1) {
                                    File classFile = entry.getSourceFile();
                                    String url = getRelativeURL(root, classFile);
                                    escaped = escaped.replaceAll(fullyQualifiedName + ";", "<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + fullyQualifiedName + "</a>;");
                                } else if (line.indexOf(className) > -1) {
                                    File classFile = entry.getSourceFile();
                                    String url = getRelativeURL(root, classFile);
                                    escaped = escaped.replaceAll("\\s" + className + ",", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>,");
                                    escaped = escaped.replaceAll("\\s" + className + "\\{", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>{");
                                    escaped = escaped.replaceAll("," + className + ",", ",<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>,");
                                    escaped = escaped.replaceAll("," + className + "\\{", ",<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>{");
                                    escaped = escaped.replaceAll("\\s" + className + "\\s", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a> ");
                                    escaped = escaped.replaceAll("\\(" + className + "\\s", "(<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a> ");
                                    escaped = escaped.replaceAll("\\s" + className + "\\.", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>.");
                                    escaped = escaped.replaceAll("\\(" + className + "\\.", "(<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>.");
                                    escaped = escaped.replaceAll("\\s" + className + "\\(", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>(");
                                    escaped = escaped.replaceAll("\\(" + className + "\\(", "(<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>(");
                                    escaped = escaped.replaceAll("&gt;" + className + ",", "&gt;<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>,");
                                    escaped = escaped.replaceAll("&gt;" + className + "\\s", "&gt;<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a> ");
                                    escaped = escaped.replaceAll("&gt;" + className + "&lt;", "&gt;<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>&lt;");
                                    escaped = escaped.replaceAll("\\(" + className + "\\);", "(<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + className + "</a>)");
                                }
                            }
                        } else if (storedProc != null) {
                            Set<String> procSet = storedProc.getTopReferences();
                            List<String> sortedProcs = new ArrayList(procSet);
                            Collections.sort(sortedProcs, LONGEST_FIRST);
                            for (String procFound : sortedProcs) {
                                if (escaped.indexOf(procFound) != -1) {
                                    File nameFile = storedProcProj.getLocation(procFound);
                                    if (nameFile != null) {
                                        String url = getRelativeURL(root, nameFile);
                                        escaped = escaped.replaceAll("\\s" + procFound + "\\s", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a> ");
                                        escaped = escaped.replaceAll("\\s" + procFound + ",", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>,");
                                        escaped = escaped.replaceAll("\\s" + procFound + ";", " <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>;");
                                        escaped = escaped.replaceAll("," + procFound + "\\s", ",<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a> ");
                                        escaped = escaped.replaceAll("," + procFound + ",", ",<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>,");
                                        escaped = escaped.replaceAll("," + procFound + ";", ",<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>;");
                                        escaped = escaped.replaceAll("=" + procFound + "\\s", "=<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a> ");
                                        escaped = escaped.replaceAll("=" + procFound + ",", "=<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>,");
                                        escaped = escaped.replaceAll("=" + procFound + ";", "=<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>;");
                                        escaped = escaped.replaceAll("." + procFound + "\\s", ".<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a> ");
                                        escaped = escaped.replaceAll("." + procFound + ",", ".<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>,");
                                        escaped = escaped.replaceAll("." + procFound + ";", ".<a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + procFound + "</a>;");
                                    } else {
                                        System.err.println("NOT FOUND: " + procFound);
                                    }
                                }
                            }
                        }
                        out.println(escaped);
                    }
                    out.println("</pre></td>");
                    out.println("</tr>");
                    out.println("</table>");
                    out.println("<table border='1'>");
                    out.printf("<tr><td bgcolor=\"#C4C4C4\"><tt>Revision</tt></td><td bgcolor=\"#C4C4C4\"><tt>Date</tt></td><td bgcolor=\"#C4C4C4\"><tt>Author</tt></td><td bgcolor=\"#C4C4C4\"><tt>Comment</tt></td></tr>");
                    DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    for (Change message : logMessages) {
                        out.printf("<tr><td><tt>%s</tt></td><td><tt>%s</tt></td><td><tt>%s</tt></td><td><tt>%s</tt></td></tr>%n", message.version, format.format(message.date).replaceAll("\\s", "&nbsp;"), message.author, message.message);
                    }
                    out.println("</table>");
                    if (project != null) {
                        out.println("<pre>");
                        for (FindJavaSources.JavaClass entry : importList) {
                            String url = getRelativeURL(root, entry.getSourceFile());
                            out.println("import <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + entry.getFullyQualifiedName() + "</a> as " + entry.getClassName());
                        }
                        out.println("</pre>");
                    }
                    if (storedProc != null) {
                        out.println("<pre>");
                        for (String procName : storedProc.getReferences()) {
                            FindStoredProcs.StoredProc proc = storedProcProj.getStoredProc(procName);
                            if (proc != null) {
                                String url = getRelativeURL(root, proc.getFile());
                                out.println("using <a href='/?show=" + url + "&index=" + index + "&searcher=" + searcher + "'>" + proc.getName() + "</a>");
                            }
                        }
                        out.println("</pre>");
                    }
                    out.println("</form>");
                    out.println("</body>");
                    out.println("</html>");
                }
            } else if (path.endsWith(".js") || path.endsWith(".css") || path.endsWith(".formatted")) {
                path = path.replace('/', File.separatorChar);
                if (path.endsWith(".formatted")) {
                    resp.set("Content-Type", "text/plain");
                } else if (path.endsWith(".js")) {
                    resp.set("Content-Type", "application/javascript");
                } else {
                    resp.set("Content-Type", "text/css");
                }
                resp.set("Cache", "no-cache");
                WritableByteChannel channelOut = resp.getByteChannel();
                File file = new File(".", path).getCanonicalFile();
                System.err.println("Serving " + path + " as " + file.getCanonicalPath());
                FileChannel sourceChannel = new FileInputStream(file).getChannel();
                sourceChannel.transferTo(0, file.length(), channelOut);
                sourceChannel.close();
                channelOut.close();
            } else if (env != null && regex != null) {
                ServerDetails details = config.getEnvironment(env).load(persister, serverDetails != null, fiServerDetails != null, hostDetails != null);
                List<String> tokens = new ArrayList<String>();
                List<Searchable> list = details.search(regex, deep != null, tokens);
                Collections.sort(tokens, LONGEST_FIRST);
                for (String token : tokens) {
                    System.out.println("TOKEN: " + token);
                }
                resp.set("Content-Type", "text/html");
                out.println("<html>");
                out.println("<head>");
                out.println("<link rel='stylesheet' type='text/css' href='style.css'>");
                out.println("<script src='highlight.js'></script>");
                out.println("</head>");
                out.println("<body onload=\"sh_highlightDocument('lang/', '.js')\">");
                writeSearchBox(out, searcher, null, null, regex);
                out.println("<br>Found " + list.size() + " hits for <b>" + regex + "</b>");
                out.println("<table border='1''>");
                int countIndex = 1;
                for (Searchable value : list) {
                    out.println("    <tr><td>" + countIndex++ + "&nbsp;<a href='" + value.getSource() + "'><b>" + value.getSource() + "</b></a></td></tr>");
                    out.println("    <tr><td><pre class='sh_xml'>");
                    StringWriter buffer = new StringWriter();
                    persister.write(value, buffer);
                    String text = buffer.toString();
                    text = escapeHtml(text);
                    for (String token : tokens) {
                        text = text.replaceAll(token, "<font style='BACKGROUND-COLOR: yellow'>" + token + "</font>");
                    }
                    out.println(text);
                    out.println("    </pre></td></tr>");
                }
                out.println("</table>");
                out.println("</form>");
                out.println("</body>");
                out.println("</html>");
            } else if (index != null && term != null && term.length() > 0) {
                out.println("<html>");
                out.println("<head>");
                out.println("<link rel='stylesheet' type='text/css' href='style.css'>");
                out.println("<script src='highlight.js'></script>");
                out.println("</head>");
                out.println("<body onload=\"sh_highlightDocument('lang/', '.js')\">");
                writeSearchBox(out, searcher, term, index, null);
                if (searcher == null) {
                    searcher = searchEngine.getDefaultSearcher();
                }
                if (refresh != null) {
                    SearchEngine engine = searchEngine.index(searcher);
                    File root = engine.getRoot();
                    File searchIndex = getJavaIndexFile(root, index);
                    FindJavaSources.deleteProject(root, searchIndex);
                }
                boolean isRefresh = refresh != null;
                boolean isGrep = grep != null;
                boolean isSearchNames = name != null;
                SearchQuery query = new SearchQuery(index, term, page, isRefresh, isGrep, isSearchNames);
                List<SearchResult> results = searchEngine.index(searcher).search(query);
                writeSearchResults(query, searcher, results, out);
                out.println("</body>");
                out.println("</html>");
            } else {
                out.println("<html>");
                out.println("<body>");
                writeSearchBox(out, searcher, null, null, null);
                out.println("</body>");
                out.println("</html>");
            }
            out.close();
        } catch (Exception e) {
            try {
                e.printStackTrace();
                resp.reset();
                resp.setCode(500);
                resp.setText("Internal Server Error");
                resp.set("Content-Type", "text/html");
                out.println("<html>");
                out.println("<body><h1>Internal Server Error</h1><pre>");
                e.printStackTrace(out);
                out.println("</pre></body>");
                out.println("</html>");
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void writeSearchResults(SearchQuery query, String searchIndex, List<SearchResult> results, PrintStream out) throws Exception {
        out.print("<br>Found " + results.size() + " hits for <b>" + query.getTerm() + "</b>");
        out.print("<table cellpadding='5'><tr>");
        if (results.size() > 100) {
            out.print("<td>");
            out.print("<a href='/?searcher=" + searchIndex + "&index=" + query.getFilter() + "&term=" + query.getTerm() + "&page=" + (query.getPage() + 100) + (query.isGrep() ? "&grep=on" : "") + (query.isSearchNames() ? "&name=on" : "") + "'>Next 100</a>");
            out.print("</td>");
        }
        if (query.getPage() >= 100) {
            out.print("<td>");
            out.print("<a href='/?searcher=" + searchIndex + "&index=" + query.getFilter() + "&term=" + query.getTerm() + "&page=" + (query.getPage() - 100) + (query.isGrep() ? "&grep=on" : "") + (query.isSearchNames() ? "&name=on" : "") + "'>Previous 100</a>");
            out.print("</td>");
        }
        out.println("</tr></table>");
        out.println("<table border='1'>");
        for (SearchResult result : results) {
            int percent = result.getPercent();
            String docName = result.getFile();
            List<String> snippet = result.getSnippet();
            out.println("<tr><td>");
            out.println("<table border='0'>");
            out.println("<tr><td>" + result.getPosition() + "</td><td>");
            out.println("<table>\n");
            out.println(" <tr>\n");
            out.println("    <td>\n");
            out.println("    <table cellpadding='1' cellspacing='0' width='60' border='0' bgcolor='#000000'>\n");
            out.println("    <tr>\n");
            out.println("       <td>\n");
            out.println("       <table cellpadding='0' cellspacing='0' border='0' width='100%'>\n");
            out.println("       <tr>\n");
            out.println("          <td bgcolor='#00ff00' height=10 style='border: 0' width='" + percent + "%'>\n");
            out.println("             <spacer type=block width=2 height=8>\n");
            out.println("          </td>\n");
            out.println("          <td bgcolor='#ffffff' height=10 style='border: 0' width='" + (100 - percent) + "%'>\n");
            out.println("             <spacer type=block width=2 height=8>\n");
            out.println("          </td>\n");
            out.println("       </tr>\n");
            out.println("       </table>\n");
            out.println("       </td>\n");
            out.println("    </tr>\n");
            out.println("    </table>\n");
            out.println("    </td>\n");
            out.println(" <tr>\n");
            out.println(" </table>\n");
            out.print("</td><td><a href='/?show=" + docName + "&index=" + query.getFilter() + "&searcher=" + searchIndex + "'><b>" + docName + "</b></a>");
            if (docName.endsWith(".sql")) {
                out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a style='FOREGROUND-COLOR: red' href='/sql/" + docName + "?index=" + query.getFilter() + "&searcher=" + searchIndex + "'><b>REFERENCES</b>");
            }
            out.println("</td></tr>");
            out.println("</table>");
            out.println("</td></tr>");
            if (snippet.size() > 0) {
                out.println("<tr><td>");
                out.println("<table border='0'>");
                String preClass = SearchServer.getJavaScript(docName);
                out.println("<tr><td><pre" + preClass + ">");
                for (String line : snippet) {
                    line = line.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                    line = line.replaceAll(query.getTerm(), "<font style='BACKGROUND-COLOR: yellow'>" + query.getTerm() + "</font>");
                    out.println(line);
                }
                out.println("</pre></td></tr>");
                out.println("</table>");
                out.println("</td></tr>");
            }
        }
        out.println("</table>");
    }

    public void writeSearchBox(PrintStream out, String searcherToUse, String searchTerm, String previousType, String previousRegex) throws Exception {
        out.println("<a href='/fs/'>File System Browser</a>");
        out.println("<table border='1'><tr><td valign='top'>");
        out.println("<tr><td valign='top'>");
        writeXmlSearchBox(out, previousRegex);
        out.println("</td><td valign='top'>");
        writeCodeSearchBox(out, searcherToUse, searchTerm, previousType);
        out.println("</td><td valign='top'>");
        writeUploadBox(out);
        out.println("</td></tr></table>");
    }

    private void writeUploadBox(PrintStream out) throws Exception {
        out.println("<form action='/upload' enctype='multipart/form-data' method='post'>");
        out.println("<table border='1'>");
        out.println("    <tr><td><input type='file' name='datafile1'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile2'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile3'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile4'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile5'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile6'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile7'></td></tr>");
        out.println("    <tr><td><input type='file' name='datafile8'></td></tr>");
        out.println("    <tr><td><input type='submit' value='Upload'></td></tr>");
        out.println("</table>");
        out.println("</form>");
    }

    private void writeXmlSearchBox(PrintStream out, String previousRegex) throws Exception {
        out.println("<form action='/search' method='POST'>");
        out.println("<table border='0'>");
        out.println("<tr><td valign='top'>");
        out.println("<table border='1'>");
        String checked = "checked";
        for (Config.Environment entry : config.getEnvironments()) {
            out.println("    <tr><td>" + entry.getName().toUpperCase() + ": <input type='radio' name='env' value='" + entry.getName() + "' " + checked + "/></td></tr>");
            checked = "";
        }
        out.println("    <tr><td><input type='text' name='regex' " + (previousRegex == null ? "" : "value='" + previousRegex + "'") + "/><input type='submit' value='Search' /></td></tr>");
        out.println("</table>");
        out.println("</td></tr><tr><td valign='top'>");
        out.println("<table border='1'>");
        out.println("    <tr><td>Search fi_server_details</td><td><input type='checkbox' name='fi_server_details' checked></td></tr>");
        out.println("    <tr><td>Search server_details</td><td><input type='checkbox' name='server_details' checked></td></tr>");
        out.println("    <tr><td>Search host_details</td><td><input type='checkbox' name='host_details' checked></td></tr>");
        out.println("    <tr><td>Deep Search</td><td><input type='checkbox' name='deep'></td></tr>");
        out.println("</table>");
        out.println("</table>");
        out.println("</form>");
    }

    private void writeCodeSearchBox(PrintStream out, String searcherToUse, String previousTerm, String previousType) throws Exception {
        out.println("<form name='searchForm' action='/search' method='GET'>");
        out.println("<table border='1'>");
        String defaultSearcher = searchEngine.getDefaultSearcher();
        if (searcherToUse != null) {
            defaultSearcher = searcherToUse;
        }
        out.println("<tr><td><select name='searcher' onChange='searchForm.submit();'>");
        for (SearchEngine searcher : searchEngine.all()) {
            String name = searcher.getName();
            String selected = "";
            if (name.equals(defaultSearcher)) {
                selected = "selected";
            }
            out.println("<option value='" + name + "' " + selected + ">" + name + "</option>");
        }
        out.println("</select></td></tr>");
        List<String> indexes = searchEngine.getIndexes();
        for (SearchEngine searcher : searchEngine.all()) {
            String name = searcher.getName();
            if (name.equals(defaultSearcher)) {
                String defaultIndex = searcher.getDefaultFilter();
                if (previousType != null) {
                    defaultIndex = previousType;
                }
                for (String index : indexes) {
                    String checked = "";
                    if (index.equals(defaultIndex)) {
                        checked = "checked";
                    }
                    out.println("<tr><td>" + index + ": <input type='radio' name='index' value='" + index + "' " + checked + "/></td></tr>");
                }
            }
        }
        out.println("<tr><td><input type='text' name='term' " + (previousTerm == null ? "" : "value='" + previousTerm + "'") + "/><input type='submit' value='Search' /></td></tr>");
        out.println("<tr><td>");
        out.println("<table border='0'>");
        out.println("<tr><td>Match Exactly</td><td><input type='checkbox' name='grep' checked></td></tr>");
        out.println("<tr><td>Refresh Index</td><td><input type='checkbox' name='refresh'></td></tr>");
        out.println("<tr><td>Search File Names</td><td><input type='checkbox' name='name'></td></tr>");
        out.println("</table>");
        out.println("</td></tr>");
        out.println("</table>");
        out.println("</form>");
    }

    private static SSLContext getContext(File keyStore) throws Exception {
        char[] storePassword = "password".toCharArray();
        char[] keyPassword = "password".toCharArray();
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(keyStore), storePassword);
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance("SunX509");
        keyFactory.init(store, keyPassword);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    public static void main(String[] list) throws Exception {
        int port = new Integer(list[0]).intValue();
        File configFile = new File(list[1]);
        File searchConfigFile = new File(list[2]);
        File keyStoreFile = new File(list[3]);
        Persister persister = new Persister();
        SearchEngineManager searchEngine = persister.read(SearchEngineManager.class, searchConfigFile);
        Config config = persister.read(Config.class, configFile);
        Container container = new SearchServer(config, searchEngine, persister);
        Container proxyContainer = new ProxyContainer();
        ContainerServer proxyEngine = new ContainerServer(proxyContainer, 2);
        ContainerServer engine = new ContainerServer(container, 2);
        Connection connection = new SocketConnection(engine);
        Connection plain = new SocketConnection(proxyEngine);
        SocketAddress address = new InetSocketAddress(port);
        plain.connect(new InetSocketAddress(9999));
        connection.connect(address, getContext(keyStoreFile));
    }
}
