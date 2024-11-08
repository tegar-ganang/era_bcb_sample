import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Simple HTTP 2.0 client
 */
public class HttpClient {

    private static final String CRLF = "\r\n";

    private String address;

    private String location;

    private int port;

    private HttpHeaders headers;

    private HtmlParser parser;

    /**
     * Constructor
     */
    public HttpClient(String url) {
        setUrl(url);
    }

    /**
     * Constructor
     */
    public HttpClient(String address, String location) {
        this(address, location, 80);
    }

    /**
     * Constructor
     */
    public HttpClient(String address, String location, int port) {
        this.address = address;
        this.port = port;
        this.location = location;
    }

    /**
     * Returns HTML tags
     * @return HTML tags
     */
    public List<Tag> getTags() {
        return this.parser.getTags();
    }

    /**
     * Returns all HTML link tags
     * @return HTML link tags
     */
    public List<String> getLinks() {
        List<String> links = new ArrayList<String>();
        List<Tag> tags = this.parser.getTags();
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);
                if (tag.getName().equals("A")) {
                    String href = tag.getAttribValue("HREF");
                    if (href != null) {
                        links.add(href);
                    }
                }
            }
        }
        return links;
    }

    public void dump() {
        if (parser != null) {
            List<Tag> tags = this.parser.getTags();
            for (int i = 0; i < tags.size(); i++) {
                System.out.println(tags.get(i));
            }
        }
    }

    /**
     * Sets URL
     * @param URL
     */
    private void setUrl(String url) {
        UrlParser urlParser = new UrlParser(url);
        this.address = urlParser.getAddress();
        this.location = urlParser.getLocation();
        this.port = urlParser.getPort();
    }

    /**
     * Manages HTTP redirects
     * @return status of redirected get
     */
    public int redirect() {
        String moved = headers.getLocation();
        if (moved.startsWith("http://")) {
            setUrl(moved);
        } else {
            this.location = moved;
        }
        return get();
    }

    /**
     * Performs a HTML get
     * @return status of get
     */
    public int get() {
        Socket socket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            if (this.address != null) {
                socket = new Socket(this.address, this.port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                sendGet(reader, writer);
                parseHeaders(reader);
                String html = parseBody(reader);
                storeCache(html);
                if (getContentType().equals("text/html")) {
                    long start = System.currentTimeMillis();
                    parser = new HtmlParser(html);
                    long time = System.currentTimeMillis() - start;
                    System.out.println("#= parse time: " + (((double) time) / 1000.0) + " second(s) =#");
                }
            } else {
                long start = System.currentTimeMillis();
                parser = new HtmlParser(readCache());
                long time = System.currentTimeMillis() - start;
                System.out.println("#= parse time: " + (((double) time) / 1000.0) + " second(s) =#");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (headers != null) {
            return headers.getCode();
        } else {
            return 200;
        }
    }

    /**
     * Writes HTML to disk
     * @param HTML
     */
    private void storeCache(String html) throws Exception {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("in.html"));
            writer.write(html);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads HTML from disk
     * @return HTML
     */
    private String readCache() throws Exception {
        BufferedReader reader = null;
        StringBuffer buffer = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader("in.html"));
            String line = reader.readLine();
            while (line != null) {
                buffer.append(line);
                line = reader.readLine();
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

    /**
     * Sends a HTML get
     */
    private void sendGet(final BufferedReader reader, final BufferedWriter writer) throws IOException {
        StringBuffer getBuffer = new StringBuffer();
        getBuffer.append("GET ");
        getBuffer.append(this.location);
        getBuffer.append(" HTTP/1.1");
        getBuffer.append(CRLF);
        getBuffer.append("Host: ");
        getBuffer.append(this.address);
        getBuffer.append(CRLF);
        getBuffer.append(CRLF);
        writer.write(getBuffer.toString());
        writer.flush();
    }

    /**
     * Parsers HTML header
     */
    private void parseHeaders(final BufferedReader reader) throws IOException {
        final String delim = ": ";
        String responseType = null;
        Map<String, String> headers = new HashMap<String, String>();
        System.out.println("================= HEADERS =================");
        String header = reader.readLine();
        while (header != null && header.length() > 0) {
            int delimIndex = header.indexOf(delim);
            if (delimIndex > -1) {
                String name = header.substring(0, delimIndex);
                String value = header.substring(delimIndex + delim.length(), header.length());
                headers.put(name.toUpperCase(), value);
                System.out.println("header - name: |" + name + "| value: |" + value + "|");
            } else if (header.startsWith("HTTP")) {
                responseType = header;
                System.out.println("response - |" + responseType + "|");
            }
            header = reader.readLine();
        }
        if (responseType != null) {
            this.headers = new HttpHeaders(responseType, headers);
        }
    }

    /**
     * Parsers HTML body
     */
    private String parseBody(final BufferedReader reader) throws IOException {
        String body = null;
        if (headers.getTransferEncoding() != null) {
            StringBuffer buffer = new StringBuffer();
            String line = reader.readLine();
            while (!line.equals("0")) {
                String chunkSize = line.trim();
                int extIndex = chunkSize.indexOf(';');
                if (extIndex > -1) {
                    chunkSize = chunkSize.substring(0, extIndex);
                }
                buffer.append(read(reader, Integer.parseInt(chunkSize, 16)));
                reader.readLine();
                line = reader.readLine();
            }
            body = buffer.toString().trim();
        } else if (headers.getContentLength() != null) {
            final int bodySize = Integer.parseInt(headers.getContentLength());
            body = read(reader, bodySize);
        } else {
            StringBuffer buffer = new StringBuffer();
            String line = reader.readLine();
            while (line != null && line.length() > 0) {
                buffer.append(line);
                line = reader.readLine();
            }
            body = buffer.toString();
        }
        return body;
    }

    /**
     * Reads HTML from server
     */
    private String read(final BufferedReader reader, final int chunkSize) throws IOException {
        char chunk[] = new char[chunkSize];
        StringBuffer buffer = new StringBuffer();
        int read = 0;
        while (read != chunkSize) {
            int start = read;
            read += reader.read(chunk, 0, chunkSize - read);
            int end = read - start;
            buffer.append(chunk, 0, end);
        }
        return buffer.toString();
    }

    /**
     * Returns content type
     * @return content type
     */
    public String getContentType() {
        if (headers != null) {
            return headers.getContentType();
        } else {
            return "text/html";
        }
    }
}
