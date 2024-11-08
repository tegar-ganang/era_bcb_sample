package blueprint4j.utils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;

public class Log {

    public static int HTML_LOG_PORT = Settings.getInt("log.html_port", 25001);

    public static String HTML_LOG_FILE_LOCATION = Settings.getString("log.html_file_location", "html_logs/");

    static final String LS = System.getProperty("line.separator");

    static final ThreadRoom cleanup_room = new ThreadRoom(ThreadRoom.INFINITE);

    private static final SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd 'at' HH'h'mm.ss SSS");

    private static final SimpleDateFormat http_date_format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    public static final Type info = new Info("Info");

    public static final Type trace = new Trace("Trace");

    public static final Type dev = new Trace("Dev");

    public static final Type sql = new Trace("SQL");

    public static final Type message = new Trace("Message");

    public static final Type comms = new Trace("Comms");

    public static final Type debug = new Debug("Debug");

    public static final Type critical = new Critical("Critical");

    private static final Vector html_loggers = new Vector();

    private static boolean trace_to_screen = false;

    private static LoggingFileHTML html_out = null;

    static {
        http_date_format.setTimeZone(TimeZone.getTimeZone("GMT"));
        trace_to_screen = Settings.getBoolean("log.trace", false);
        int port = HTML_LOG_PORT;
        if (port != -1) {
            new HTTPLog(port).start();
        }
        if (HTML_LOG_FILE_LOCATION != null && HTML_LOG_FILE_LOCATION.length() > 0) {
            html_out = new LoggingFileHTML(HTML_LOG_FILE_LOCATION);
        }
    }

    public static class LogBindable {

        public Integer id = null, thread_id = null;

        public String type = null, subtype = null, description = null, details = null;

        public Date timestamp = null;

        public String stack = null;

        public LogBindable(Integer id, Integer thread_id, String type, String subtype, String description, String details, Date timestamp, String stack) {
            this.id = id;
            this.thread_id = thread_id;
            this.type = type;
            this.subtype = subtype;
            this.description = description;
            this.details = details;
            this.timestamp = timestamp;
            this.stack = stack;
        }
    }

    public static class VectorLogBindable {

        private Vector vector = new Vector();

        public VectorLogBindable() {
        }

        public LogBindable get(int i) {
            return (LogBindable) vector.get(i);
        }

        public void add(LogBindable data_unit) {
            vector.add(data_unit);
        }

        public int size() {
            return vector.size();
        }
    }

    private Log() {
    }

    private static synchronized void out(Type lvl, String description, String details) {
        cleanup_room.enter();
        System.out.flush();
        try {
            long thread_id = ThreadId.getCurrentId();
            Date timestamp = new Date();
            if (trace_to_screen || lvl.value > trace.value) {
                System.out.println(lvl + "[" + thread_id + "] " + date_format.format(timestamp) + LS + "Description: " + description + LS + details + LS + "-------------------------------------");
            }
            for (int t = 0; t < html_loggers.size(); t++) {
                ((HTMLLogger) html_loggers.get(t)).out(thread_id, lvl, description, details, timestamp);
            }
            if (lvl.value > trace.value && html_out != null) {
                html_out.write(lvl.getHTMLForLog(thread_id, description, details, timestamp));
            }
        } finally {
            System.out.flush();
            cleanup_room.exit();
        }
    }

    static String getTraceString(Throwable th, String line_seperator) {
        StackTraceElement[] trace = th.getStackTrace();
        StringBuffer sb = new StringBuffer();
        for (int t = 0; t < trace.length; t++) {
            sb.append(trace[t].toString());
            sb.append(line_seperator);
        }
        return sb.toString();
    }

    public static class Type {

        String base;

        public String type;

        int value;

        String html_heading;

        String html_text;

        public Type(String base, String type, int value, Color html_heading, Color html_text) {
            this.base = base;
            this.type = type;
            this.value = value;
            this.html_heading = htmlColor(html_heading);
            this.html_text = htmlColor(html_text);
        }

        String htmlColor(Color color) {
            int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
            return "\"#" + ((r < 16) ? "0" : "") + Integer.toHexString(r) + ((g < 16) ? "0" : "") + Integer.toHexString(g) + ((b < 16) ? "0" : "") + Integer.toHexString(b) + "\"";
        }

        boolean isActive() {
            return true;
        }

        public String toString() {
            return base + ":" + type;
        }

        public void out(Object object) {
            if (isActive()) {
                Log.out(this, object.toString(), "");
            }
        }

        public void out(String description) {
            if (isActive()) {
                Log.out(this, description, "");
            }
        }

        public void out(String description, String details) {
            if (isActive()) {
                Log.out(this, description, details);
            }
        }

        public void out(Throwable details) {
            if (isActive()) {
                out(details.getClass() + ":" + details.getMessage(), getTraceString(details, LS));
            }
        }

        public void out(String description, Throwable exception) {
            if (isActive()) {
                out(description, exception.getMessage() + LS + getTraceString(exception, LS));
            }
        }

        public void out(String description, Properties props) {
            if (isActive()) {
                String propstr = "";
                try {
                    ByteArrayOutputStream ba = new ByteArrayOutputStream();
                    props.store(ba, "");
                    propstr = ba.toString();
                    ba.close();
                } catch (IOException e) {
                    out("Error writting properties log.", e);
                    return;
                }
                out(description, propstr);
            }
        }

        public void out(String description, byte[] data) {
            if (isActive()) {
                StringBuffer strdata = new StringBuffer();
                StringBuffer pos = new StringBuffer();
                StringBuffer hex = new StringBuffer();
                StringBuffer str = new StringBuffer();
                for (int b = 0; b < data.length; b += 16) {
                    String one;
                    pos.delete(0, pos.length());
                    hex.delete(0, hex.length());
                    str.delete(0, str.length());
                    while (pos.length() < 4) pos.insert(0, "0");
                    for (int u = 0; u < 16; u++) {
                        if (b + u < data.length) {
                            int value = data[b + u];
                            if (value < 0) value += 128;
                            one = new BigInteger("" + value).toString(16);
                            while (one.length() < 2) one = "0" + one;
                            hex.append(one);
                            if (u == 7) {
                                hex.append("-");
                            } else {
                                hex.append(" ");
                            }
                            if (value >= 32 && value <= 127) {
                                str.append(new String(new byte[] { (byte) value }));
                            } else {
                                str.append(".");
                            }
                        } else {
                            hex.append("   ");
                            str.append(" ");
                        }
                    }
                    strdata.append(pos + ":  " + hex + "  " + str + LS);
                }
                out(description, strdata.toString());
            }
        }

        String getHTMLForLog(long thread_id, String description, String details, Date timestamp) {
            return "<TABLE WIDTH=100% BORDER=0 CELLPADDING=0 CELLSPACING=0>" + "<TBODY>" + "<TR VALIGN=TOP BGCOLOR=" + html_heading + ">" + "<TD ALIGN=LEFT NOWRAP WIDTH=20%>" + "<FONT FACE=\"Times New Roman\" SIZE=4 COLOR=\"#FFFFFF\"><B>" + prepForHtml(base) + ":" + prepForHtml(type) + " [" + thread_id + "]" + "</B></FONT>" + "</TD>" + "<TD ALIGN=LEFT NOWRAP>" + "<FONT FACE=\"Times New Roman\" SIZE=4 COLOR=\"#FFFFFF\"><B>" + prepForHtml(description) + "</B></FONT>" + "</TD>" + "<TD ALIGN=RIGHT NOWRAP>" + "<FONT FACE=\"Times New Roman\" SIZE=4 COLOR=\"#FFFFFF\">" + "<i>" + date_format.format(timestamp) + "</i>" + "</FONT>" + "</TD>" + "</TR>" + "<TR VALIGN=TOP>" + "<TD WIDTH=100% ALIGN=LEFT COLSPAN=3  BGCOLOR=" + html_text + ">" + "<font color=\"#666666\"><pre>" + prepForHtml(details) + "</pre></font>" + "</TD>" + "</TR>" + "</TBODY>" + "</TABLE>";
        }

        String prepForHtml(String text) {
            if (text == null) {
                return "";
            }
            return text.replaceAll("&", "&amp;").replaceAll("<", "&LT;").replaceAll(">", "&GT;");
        }
    }

    public static class Info extends Type {

        public Info(String type) {
            super("Info", type, 1000, Color.BLACK, Color.WHITE);
        }
    }

    public static class Trace extends Type {

        public Trace(String type) {
            super("Trace", type, 100, new Color(0x00, 0x99, 0x33), new Color(0xcf, 0xfe, 0xde));
        }

        public boolean isActive() {
            return trace_to_screen || html_loggers.size() > 0;
        }
    }

    public static class Debug extends Type {

        public Debug(String type) {
            super("Debug", type, 200, new Color(0xff, 0x66, 0x00), new Color(0xff, 0xdd, 0xc7));
        }
    }

    public static class Critical extends Type {

        public Critical(String type) {
            super("Critical", type, 300, new Color(0xcc, 0x00, 0x33), new Color(0xff, 0xfe, 0xd2));
        }

        public void out(String description, String details) {
            if (isActive()) {
                Log.out(this, description, details);
            }
            System.out.println("LOG:CRITICAL\n" + description + "\n" + details);
            ThreadSchedule.haltBlocking();
            System.exit(-1);
        }
    }

    static class HTTPLog extends Thread {

        int port;

        boolean done = false;

        ServerSocket server;

        HTTPLog(int port) {
            try {
                this.port = port;
                server = new ServerSocket(port);
            } catch (IOException e) {
                debug.out(e);
                done = true;
            }
        }

        public void run() {
            try {
                while (!done) {
                    Socket socket = server.accept();
                    new HTMLLogger(socket);
                }
            } catch (Exception e) {
                if (!"socket closed".equals(e.getMessage())) {
                    Log.critical.out(e);
                }
            }
        }
    }

    static class HTMLLogger {

        static final Pattern TESTGET = Pattern.compile("GET .*", Pattern.DOTALL);

        static final Pattern TESTPOST = Pattern.compile("POST .*", Pattern.DOTALL);

        byte[] buffer = new byte[1024];

        Socket socket;

        InputStream input;

        OutputStream output;

        boolean done = false;

        HTMLLogger(Socket socket) {
            try {
                this.socket = socket;
                this.input = socket.getInputStream();
                this.output = socket.getOutputStream();
                int s = input.read(buffer);
                if (s != -1) {
                    String http = new String(buffer, 0, s);
                    if (TESTGET.matcher(http).matches()) {
                        Log.comms.out("HTTP Get", http);
                        String now = http_date_format.format(new Date());
                        String response = "HTTP/1.0 200 OK\n" + "Date: " + now + "\n" + "Content-Length: 99999999\n" + "Content-Type: text/html\n" + "Cache-Control: no-cache\n" + "Content-Encoding: token\n" + "Server: blueprint4j Logger\n" + "Connection: keep-alive\n" + "Last-Modified: " + now + "\n" + "\n";
                        output.write(response.getBytes());
                    } else if (TESTPOST.matcher(http).matches()) {
                        Log.trace.out("HTTP Post", http);
                        System.exit(-1);
                    } else {
                        Log.debug.out("HTTP Unknown", http);
                    }
                } else {
                    throw new IOException("Connections must be HTTP");
                }
            } catch (IOException e) {
                done = true;
                Log.debug.out(e);
            }
            if (!done) {
                html_loggers.add(this);
            }
        }

        boolean isDone() {
            return done;
        }

        void out(long thread_id, Type lvl, String description, String details, Date timestamp) {
            if (!done && socket.isConnected()) {
                try {
                    output.write(lvl.getHTMLForLog(thread_id, description, details, timestamp).getBytes());
                } catch (Exception e) {
                    done = true;
                }
            } else {
                done = true;
            }
        }
    }
}
