package jelb.www;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import jelb.TradeBot;
import jelb.common.ILogger;
import jelb.common.Parser;

public class MicroWWWServer extends Thread {

    static final byte[] EOL = { (byte) '\r', (byte) '\n' };

    /** 2XX: generally "OK" */
    public static final int HTTP_OK = 200;

    public static final int HTTP_CREATED = 201;

    public static final int HTTP_ACCEPTED = 202;

    public static final int HTTP_NOT_AUTHORITATIVE = 203;

    public static final int HTTP_NO_CONTENT = 204;

    public static final int HTTP_RESET = 205;

    public static final int HTTP_PARTIAL = 206;

    /** 3XX: relocation/redirect */
    public static final int HTTP_MULT_CHOICE = 300;

    public static final int HTTP_MOVED_PERM = 301;

    public static final int HTTP_MOVED_TEMP = 302;

    public static final int HTTP_SEE_OTHER = 303;

    public static final int HTTP_NOT_MODIFIED = 304;

    public static final int HTTP_USE_PROXY = 305;

    /** 4XX: client error */
    public static final int HTTP_BAD_REQUEST = 400;

    public static final int HTTP_UNAUTHORIZED = 401;

    public static final int HTTP_PAYMENT_REQUIRED = 402;

    public static final int HTTP_FORBIDDEN = 403;

    public static final int HTTP_NOT_FOUND = 404;

    public static final int HTTP_BAD_METHOD = 405;

    public static final int HTTP_NOT_ACCEPTABLE = 406;

    public static final int HTTP_PROXY_AUTH = 407;

    public static final int HTTP_CLIENT_TIMEOUT = 408;

    public static final int HTTP_CONFLICT = 409;

    public static final int HTTP_GONE = 410;

    public static final int HTTP_LENGTH_REQUIRED = 411;

    public static final int HTTP_PRECON_FAILED = 412;

    public static final int HTTP_ENTITY_TOO_LARGE = 413;

    public static final int HTTP_REQ_TOO_LONG = 414;

    public static final int HTTP_UNSUPPORTED_TYPE = 415;

    /** 5XX: server error */
    public static final int HTTP_SERVER_ERROR = 500;

    public static final int HTTP_INTERNAL_ERROR = 501;

    public static final int HTTP_BAD_GATEWAY = 502;

    public static final int HTTP_UNAVAILABLE = 503;

    public static final int HTTP_GATEWAY_TIMEOUT = 504;

    public static final int HTTP_VERSION = 505;

    static java.util.Hashtable<String, String> map = new java.util.Hashtable<String, String>();

    static {
        fillMap();
    }

    static void setSuffix(String k, String v) {
        map.put(k, v);
    }

    static void fillMap() {
        setSuffix("", "content/unknown");
        setSuffix(".uu", "application/octet-stream");
        setSuffix(".exe", "application/octet-stream");
        setSuffix(".ps", "application/postscript");
        setSuffix(".zip", "application/zip");
        setSuffix(".sh", "application/x-shar");
        setSuffix(".tar", "application/x-tar");
        setSuffix(".snd", "audio/basic");
        setSuffix(".au", "audio/basic");
        setSuffix(".wav", "audio/x-wav");
        setSuffix(".gif", "image/gif");
        setSuffix(".jpg", "image/jpeg");
        setSuffix(".jpeg", "image/jpeg");
        setSuffix(".htm", "text/html");
        setSuffix(".html", "text/html");
        setSuffix(".text", "text/plain");
        setSuffix(".c", "text/plain");
        setSuffix(".cc", "text/plain");
        setSuffix(".c++", "text/plain");
        setSuffix(".cvs", "text/plain");
        setSuffix(".h", "text/plain");
        setSuffix(".pl", "text/plain");
        setSuffix(".txt", "text/plain");
        setSuffix(".java", "text/plain");
    }

    private String root = "www/";

    private boolean killed;

    private boolean suspended;

    private int port;

    private int requests;

    private ServerSocket s;

    private TradeBot bot;

    public MicroWWWServer(String wwwRoot, TradeBot bot, int port) {
        super();
        this.root = wwwRoot == null ? "www/" : wwwRoot;
        this.bot = bot;
        this.port = port;
    }

    public String GetWwwRootPath() {
        return this.root;
    }

    public int getRequests() {
        return this.requests;
    }

    private boolean isDead() {
        return this.killed;
    }

    public boolean isSuspended() {
        return this.suspended;
    }

    public void setSuspended(boolean state) {
        if (this.isSuspended() && state == false) synchronized (this) {
            this.notify();
        }
        this.suspended = state;
    }

    public void kill() {
        this.killed = true;
        if (this.isSuspended()) this.setSuspended(false);
    }

    public void run() {
        this.s = null;
        System.out.println("Webserver starting up on port " + new Integer(this.port).toString());
        try {
            this.s = new ServerSocket(this.port);
            s.setReceiveBufferSize(10000);
        } catch (Exception e) {
            this.bot.log(ILogger.LogLevel.Error, "WWW Init Error: ", e);
            this.kill();
        }
        while (!this.isDead()) {
            if (this.isSuspended()) {
                synchronized (this) {
                    try {
                        this.bot.log(ILogger.LogLevel.Debug, "MicroWWWServer starts waiting");
                        this.wait();
                    } catch (InterruptedException ie) {
                        this.bot.log(ILogger.LogLevel.Error, "MicroWWWServer interrupted while waiting");
                        return;
                    }
                    this.bot.log(ILogger.LogLevel.Debug, "MicroWWWServer back from suspend");
                }
            }
            PrintStream out = null;
            Socket remote = null;
            try {
                remote = s.accept();
                this.generateInfoFile();
                BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
                out = new PrintStream(new java.io.BufferedOutputStream(remote.getOutputStream(), 1000000), false);
                remote.setSoTimeout(300000);
                String dbg = "";
                dbg = in.readLine();
                if (dbg == null) throw new NullPointerException("Invalid request:(null)");
                String[] request = dbg.split(" ");
                String cmd = Parser.parseString(request, 0, 0);
                String urls = Parser.parseString(request, 1, 1);
                if (cmd == null || urls == null) {
                    if (cmd == null) cmd = "(null)";
                    if (urls == null) urls = "(null)";
                    throw new NullPointerException("Invalid request cmd:" + cmd + " url:" + urls);
                }
                String tmp = cmd;
                while (tmp != null && !tmp.equals("")) {
                    tmp = in.readLine();
                    dbg += "\n" + tmp;
                }
                if (!cmd.equals("GET")) {
                    out.print("HTTP/1.0 " + HTTP_INTERNAL_ERROR + "  Not Implemented");
                    out.write(EOL);
                    this.bot.log(ILogger.LogLevel.Debug, "From " + remote.getInetAddress() + ": " + dbg + " -->" + HTTP_BAD_REQUEST);
                } else {
                    File f = null;
                    if (!urls.endsWith("/")) f = new File(this.root + urls); else f = new File(this.root + urls + "/index.html");
                    boolean ok = false;
                    File tmpF = f.getCanonicalFile();
                    while (!ok && tmpF != null && tmpF.getParentFile() != null && tmpF.getParentFile().canRead()) {
                        if (tmpF.getParentFile().isDirectory() && tmpF.getParentFile().getName().equalsIgnoreCase("www")) {
                            ok = true;
                        }
                        tmpF = tmpF.getParentFile();
                    }
                    if (!ok) this.send404(f, out); else {
                        if (this.sendsHeaders(f, out, remote)) {
                            if (f.getName().endsWith(".html")) this.sendProccesFile(f, out); else this.sendFile(f, out);
                        } else {
                            this.send404(f, out);
                        }
                    }
                }
            } catch (Exception ioe) {
                this.bot.log(ILogger.LogLevel.Error, "WWW Error:", ioe);
                if (out != null) {
                    try {
                        out.println("HTTP/1.0 " + HTTP_BAD_REQUEST);
                        out.write(EOL);
                    } catch (Exception e) {
                        this.bot.log(ILogger.LogLevel.Error, "WWW Finalizing Error:", e);
                    }
                }
            } finally {
                try {
                    out.close();
                    remote.close();
                } catch (Exception e) {
                    this.bot.log(ILogger.LogLevel.Error, "WWW Closing Error:", e);
                }
            }
        }
    }

    private long infoFileUpdateInterval = 1000;

    private synchronized void generateInfoFile() {
        try {
            File f;
            f = new File(this.root + "/Info.txt");
            if (f.exists() && new Date().getTime() > f.lastModified() + infoFileUpdateInterval) f.delete(); else return;
            f.createNewFile();
            FileWriter fstream = new FileWriter(f.getPath());
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Hello Java");
            out.close();
        } catch (Exception ex) {
        }
    }

    private boolean sendsHeaders(File f, PrintStream out, Socket remote) throws IOException {
        boolean ret = false;
        String ip = "unknow";
        if (remote != null && remote.getInetAddress() != null) ip = remote.getInetAddress().getHostAddress();
        if (f.canRead()) {
            out.print("HTTP/1.0 " + HTTP_OK + " OK");
            this.bot.log(ILogger.LogLevel.Debug, "From " + ip + ": GET " + f.getAbsolutePath() + "-->" + HTTP_OK);
            ret = true;
        } else {
            out.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
            this.bot.log(ILogger.LogLevel.Debug, "From " + ip + ": GET " + f.getAbsolutePath() + "-->" + HTTP_NOT_FOUND);
            ret = false;
        }
        out.write(EOL);
        out.print("Server: jelb.MicroWWWServer");
        out.write(EOL);
        out.print("Date: " + (new java.util.Date()));
        out.write(EOL);
        if (ret) {
            String name = f.getName();
            int ind = name.lastIndexOf('.');
            String ct = null;
            if (ind > 0) {
                ct = (String) map.get(name.substring(ind));
            }
            if (ct == null) {
                ct = "unknown/unknown";
            }
            if (ct.equalsIgnoreCase("text/html")) {
                out.print("Last-Modified: " + (new java.util.Date()));
                out.write(EOL);
            } else {
                out.print("Content-Length: " + f.length());
                out.write(EOL);
                out.print("Last-Modified: " + (new java.util.Date(f.lastModified())));
                out.write(EOL);
            }
            out.print("Content-Type: " + ct);
        }
        out.write(EOL);
        return ret;
    }

    private void send404(File f, PrintStream out) throws IOException {
        out.print("The requested resource was not found.\n");
    }

    private void sendFile(File f, PrintStream out) throws IOException {
        java.io.InputStream is = null;
        out.write(EOL);
        if (f.exists()) {
            try {
                is = new java.io.FileInputStream(f.getAbsolutePath());
                byte[] buf = new byte[4096];
                while (is.available() > 0) {
                    int readed = is.read(buf);
                    out.write(buf, 0, readed);
                }
            } catch (Exception e) {
                this.bot.log(ILogger.LogLevel.Error, "WWW sendFile() Error:", e);
            } finally {
                is.close();
            }
        }
    }

    private void sendProccesFile(File f, PrintStream out) throws IOException {
        java.io.InputStream is = null;
        out.write(EOL);
        if (f.exists()) {
            try {
                is = new java.io.FileInputStream(f.getAbsolutePath());
                StringBuffer sb = new StringBuffer();
                byte[] buf = new byte[4096];
                while (is.available() > 0) {
                    int readed = is.read(buf);
                    sb.append(new String(buf, 0, readed));
                }
                String outt = this.process(sb.toString());
                out.print(outt);
            } catch (Exception e) {
                this.bot.log(ILogger.LogLevel.Error, "WWW sendProccesFile() Error:", e);
            } finally {
                is.close();
            }
            requests++;
        }
    }

    private String process(String line) {
        line = line.replace("{table_body_inventory}", this.bot.getInventory().toHtml(this.bot.data.getOrders(), true, bot.getMaxLoad()));
        line = line.replace("{table_body_wanted}", this.bot.getInventory().toHtml(this.bot.data.getOrders(), false, bot.getMaxLoad()));
        line = line.replace("{guild_members_online}", this.bot.data.getGuild().members.toHtml(this.bot.getPlayersOnline()));
        line = line.replace("{day}", this.bot.getGameDate());
        line = line.replace("{day_desc}", this.bot.getGameDayDesc());
        line = line.replace("{time}", this.bot.getGameTime());
        line = line.replace("{stats}", this.bot.data.getHistory().toHtml());
        return line;
    }
}
