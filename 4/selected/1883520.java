package tool;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class ToyHttpd implements Runnable {

    public static final String STATUS_OK = "200 OK";

    public static final String STATUS_MOVED_PERM = "301 Moved Permanently";

    public static final String STATUS_BAD_REQUEST = "400 Bad Request";

    public static final String STATUS_FORBIDDEN = "403 Forbidden";

    public static final String STATUS_NOT_FOUND = "404 Not Found";

    public static final String STATUS_METHOD_NOT_IMPLEMENTED = "501 Method Not Implemented";

    public static final String CRLF = "\r\n";

    private static final Map<String, String> s_typeMap = new HashMap<String, String>();

    static {
        s_typeMap.put("html", "text/html");
        s_typeMap.put("htm", "text/html");
        s_typeMap.put("txt", "text/plain");
        s_typeMap.put("css", "text/css");
        s_typeMap.put("gif", "image/gif");
        s_typeMap.put("jpg", "image/jpeg");
        s_typeMap.put("jpeg", "image/jpeg");
        s_typeMap.put("png", "image/png");
    }

    public static boolean s_verbose = System.getProperty("toyhttpd.verbose") != null;

    private static File s_documentRoot;

    private Socket m_clientSocket;

    private BufferedReader m_input;

    private OutputStream m_output;

    private ToyHttpd(Socket clientSocket) {
        m_clientSocket = clientSocket;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: java ToyHttpd <port> <documentRoot>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String documentRoot = args[1];
        start(port, documentRoot);
    }

    public static void start(int port, String documentRoot) {
        try {
            s_documentRoot = new File(documentRoot).getCanonicalFile();
            if (!s_documentRoot.isDirectory()) {
                throw new IOException();
            }
        } catch (IOException ioe) {
            System.err.println("Bad documentRoot: " + documentRoot);
            System.exit(1);
        }
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.err.println("Could not listen on port " + port + ": " + ioe.toString());
            System.exit(-1);
        }
        System.out.println("Server started on port: " + port);
        while (true) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException ioe) {
                System.err.println("Accept failed: " + ioe.toString());
                continue;
            }
            ToyHttpd responder = new ToyHttpd(clientSocket);
            Thread thread = new Thread(responder);
            thread.start();
        }
    }

    public static void shut(int port) {
        BufferedWriter writer = null;
        try {
            Socket socket = new Socket("localhost", port);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("DELETE /\r\n");
            writer.flush();
            writer.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            if (ToyHttpd.s_verbose) System.out.println("Client connected: " + m_clientSocket);
            m_input = new BufferedReader(new InputStreamReader(m_clientSocket.getInputStream()));
            m_output = new BufferedOutputStream(m_clientSocket.getOutputStream(), 4096);
            processRequest();
            m_output.close();
            m_input.close();
            m_clientSocket.close();
            if (ToyHttpd.s_verbose) System.out.println("Connection closed: " + m_clientSocket);
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    private void processRequest() throws IOException {
        String inputLine;
        inputLine = m_input.readLine();
        if (inputLine == null) return;
        String method;
        String uri;
        try {
            StringTokenizer st = new StringTokenizer(inputLine);
            method = st.nextToken();
            uri = st.nextToken();
        } catch (NoSuchElementException nsee) {
            outputError(STATUS_BAD_REQUEST);
            return;
        }
        if (method.equals("GET")) {
            skipHeaders();
            System.out.println("GET " + uri);
            String unescapedUri = unescapeURI(uri);
            File file = getRealDocumentPath(unescapedUri);
            if (!file.getPath().startsWith(s_documentRoot.getPath())) {
                outputError(STATUS_BAD_REQUEST);
            } else if (file.isDirectory()) {
                if (uri.endsWith("/")) {
                    serveDirectoryIndex(file, unescapedUri);
                } else {
                    outputError(STATUS_MOVED_PERM, new String[] { "Location: " + uri + "/" });
                }
            } else if (file.isFile()) {
                if (file.canRead()) {
                    serveFile(file);
                } else {
                    outputError(STATUS_FORBIDDEN);
                }
            } else {
                outputError(STATUS_NOT_FOUND);
            }
        } else if (method.equals("DELETE")) {
            System.out.println("Server shut");
            System.exit(1);
        } else {
            outputError(STATUS_METHOD_NOT_IMPLEMENTED);
            return;
        }
    }

    /** URI �ɑΉ�����h�L�������g�̃p�X��Ԃ� */
    private File getRealDocumentPath(String uri) throws IOException {
        if (uri.startsWith("/")) uri = uri.substring(1);
        uri = uri.replace('/', File.separatorChar);
        return new File(ToyHttpd.s_documentRoot, uri).getCanonicalFile();
    }

    private void skipHeaders() throws IOException {
        String inputLine;
        do {
            inputLine = m_input.readLine();
            if (ToyHttpd.s_verbose) {
                if (inputLine != null && inputLine.length() > 0) System.out.println("skipping header: " + inputLine);
            }
        } while (inputLine != null && inputLine.length() > 0);
    }

    private void serveFile(File file) throws IOException {
        String type = getContentType(file);
        long length = file.length();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            outputError(STATUS_NOT_FOUND);
            return;
        }
        outputHeader(STATUS_OK, new String[] { "Content-Type: " + type, "Content-Length: " + length });
        copyStream(is, m_output);
        is.close();
        m_output.flush();
    }

    /** �t�@�C�������� Content Type �𓾂�B */
    private String getContentType(File file) {
        String name = file.getName();
        String ext = "";
        int idx = name.lastIndexOf('.');
        if (idx != -1) {
            ext = name.substring(idx + 1);
        }
        String type = (String) s_typeMap.get(ext.toLowerCase());
        if (type != null) {
            return type;
        } else {
            return "application/octet-stream";
        }
    }

    private void serveDirectoryIndex(File dir, String uri) throws IOException {
        String[] entries = dir.list();
        StringBuffer result = new StringBuffer();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.US);
        Arrays.sort(entries);
        result.append("<html></head><title>Index of " + escapeHTML(uri) + "</title></head><body>");
        result.append("<h1>Index of " + escapeHTML(uri) + "</h1>\n");
        result.append("<pre>\n");
        result.append("Last modified          Size  Name\n");
        result.append("<hr>\n");
        result.append("                             <a href=\"../\">Parent Directory</a>\n");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            File file = new File(dir, entry);
            String date = dateFmt.format(new Date(file.lastModified()));
            String size = format(String.valueOf(file.length()), 9, true);
            String path_uri = escapeURI(entry);
            if (file.isDirectory()) {
                path_uri = path_uri + "/";
                entry = entry + "/";
            }
            result.append(escapeHTML(date + " " + size) + "  <a href=\"" + path_uri + "\">" + escapeHTML(entry) + "</a>\n");
        }
        result.append("</pre><hr></body></html>\n");
        outputHeader(STATUS_OK, new String[] { "Content-Type: text/html" });
        outputString(result.toString());
        m_output.flush();
    }

    private void outputError(String status) throws IOException {
        outputError(status, null);
    }

    private void outputError(String status, String[] headers) throws IOException {
        if (headers == null) {
            headers = new String[] { "Content-Type: text/html" };
        } else {
            String[] newHeaders = new String[headers.length + 1];
            System.arraycopy(headers, 0, newHeaders, 1, headers.length);
            newHeaders[0] = "Content-Type: text/html";
            headers = newHeaders;
        }
        outputHeader(status, headers);
        outputString("<html><head><title>" + escapeHTML(status) + "</title></head><body><h1>" + escapeHTML(status) + "</h1></body>\n");
        m_output.flush();
    }

    private void outputHeader(String status, String[] headers) throws IOException {
        StringBuffer result = new StringBuffer();
        result.append("HTTP/1.0 " + status + CRLF);
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                result.append(headers[i]);
                result.append(CRLF);
            }
        }
        result.append(CRLF);
        outputString(result.toString());
        m_output.flush();
    }

    private void outputString(String str) throws IOException {
        try {
            m_output.write(str.getBytes());
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Can't happen: " + uee.toString());
        }
    }

    private static String escapeHTML(String str) {
        StringBuffer result = new StringBuffer();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            switch(c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }

    private static String escapeURI(String str) {
        byte[] bytes = str.getBytes();
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (('A' <= b && b <= 'Z') || ('a' <= b && b <= 'z') || ('0' <= b && b <= '9') || b == '_' || b == '.' || b == '-') {
                result.append((char) b);
            } else {
                result.append('%');
                result.append(Character.forDigit((b >> 4) & 0x0f, 16));
                result.append(Character.forDigit(b & 0x0f, 16));
            }
        }
        return result.toString();
    }

    private static String unescapeURI(String str) {
        str = str.replace('+', ' ');
        if (str.indexOf('%') == -1) {
            return str;
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            byte b = (byte) str.charAt(i);
            if (b == '%') {
                byte hi = (byte) Character.digit(str.charAt(++i), 16);
                byte lo = (byte) Character.digit(str.charAt(++i), 16);
                result.write(hi << 4 | lo);
            } else {
                result.write(b);
            }
        }
        return result.toString();
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
    }

    /** ��������t�H�[�}�b�g����B */
    private static String format(String str, int width, boolean rightJustified) {
        int len = str.length();
        int space = width - len;
        if (space <= 0) {
            return str;
        }
        StringBuffer buf = new StringBuffer();
        if (!rightJustified) {
            buf.append(str);
        }
        for (int i = 0; i < space; i++) {
            buf.append(' ');
        }
        if (rightJustified) {
            buf.append(str);
        }
        return buf.toString();
    }
}
