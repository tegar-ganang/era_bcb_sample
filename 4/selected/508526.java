package utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;

public class URLDataFetcher {

    private InputStream is = null;

    private boolean isGood = false;

    private boolean isDebug = false;

    private Socket s;

    private URL u;

    private void write2debug(String msg) {
        if (isDebug) {
            System.out.print(msg);
            System.out.flush();
        }
    }

    private void initData(String url, int connTimeout, int ioTimeout) {
        String proto;
        int port;
        write2debug("> URLDataFetcher.initData(" + url + ", " + connTimeout + ", " + ioTimeout + "): ");
        if (url == null) {
            throw new NullPointerException();
        }
        if (url.length() == 0) {
            write2debug("false\n");
            return;
        }
        try {
            u = new URL(url);
            proto = u.getProtocol();
            port = u.getPort();
            if (port == -1) {
                if (proto.compareTo("http") == 0) {
                    port = 80;
                } else if (proto.compareTo("ftp") == 0) {
                    port = 21;
                } else if (proto.compareTo("https") == 0) {
                    port = 443;
                }
            }
            s = new Socket();
            s.setSoLinger(true, 1);
            s.setSoTimeout(ioTimeout);
            s.setKeepAlive(false);
            s.connect(new InetSocketAddress(u.getHost(), port), connTimeout);
            isGood = true;
        } catch (MalformedURLException ex) {
            isGood = false;
        } catch (UnknownHostException ex) {
            isGood = false;
        } catch (IOException ex) {
            isGood = false;
        }
        write2debug(isGood + "\n");
    }

    public void setDebugMode(boolean v) {
        isDebug = v;
    }

    public URLDataFetcher(String url, int connTimeout, int ioTimeout) {
        write2debug("> URLDataFetcher.URLDataFetcher(" + url + ", " + connTimeout + ", " + ioTimeout + ")\n");
        initData(url, connTimeout, ioTimeout);
    }

    public URLDataFetcher(String url, int connTimeout, int ioTimeout, boolean debug) {
        if (debug) {
            System.out.println("> URLDataFetcher.URLDataFetcher(" + url + ", " + connTimeout + ", " + ioTimeout + ", " + debug + ")");
        }
        setDebugMode(debug);
        initData(url, connTimeout, ioTimeout);
    }

    public int read(byte[] buffer) {
        int len;
        write2debug("> URLDataFetcher.read(" + buffer + "): ");
        if (!initVars()) {
            return -1;
        }
        try {
            len = is.read(buffer);
        } catch (Exception X) {
            try {
                s.close();
            } catch (IOException ioXX) {
            }
            s = null;
            is = null;
            write2debug("-1\n");
            return -1;
        }
        if (len == 0) {
            len = -1;
        }
        write2debug(len + "\n");
        return len;
    }

    public boolean isGood() {
        write2debug("> URLDataFetcher.isGood(): " + isGood + "\n");
        return isGood;
    }

    public boolean isClosed() {
        if (s != null && is != null) {
            write2debug("> URLDataFetcher.isClosed(): false\n");
            return false;
        }
        write2debug("> URLDataFetcher.isClosed(): true\n");
        return true;
    }

    public void close() {
        write2debug("> URLDataFetcher.close()\n");
        if (s != null && !s.isClosed()) {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
                s.close();
            } catch (IOException ioX) {
            } finally {
                s = null;
            }
        }
        write2debug("< URLDataFetcher.close()\n");
    }

    public boolean sendHttpPageRequest() {
        String r;
        write2debug("> URLDataFetcher.sendHttpPageRequest(): ");
        if (s != null && s.isConnected()) {
            r = "GET " + u.getPath() + " HTTP/1.0\r\n";
            r += "Host: " + u.getHost() + "\r\n";
            r += "Connection: close\r\n\r\n";
            try {
                s.getOutputStream().write(r.getBytes());
                s.getOutputStream().flush();
                write2debug("true\n");
                return true;
            } catch (IOException ioX) {
                write2debug("false\n");
                return false;
            }
        }
        write2debug("false\n");
        return false;
    }

    public InputStream getInputStream() {
        InputStream localIs;
        write2debug("> URLDataFetcher.getInputStream(): ");
        if (s != null && s.isConnected()) {
            try {
                localIs = s.getInputStream();
            } catch (IOException ioX) {
                write2debug("null (IOException)\n");
                return null;
            }
            write2debug(localIs + "\n");
            return localIs;
        }
        write2debug("null\n");
        return null;
    }

    public Socket getSocket() {
        write2debug("> URLDataFetcher.getSocket(): " + s + "\n");
        return s;
    }

    private boolean initVars() {
        if (s != null && is != null) {
            return true;
        }
        if (s == null) {
            return false;
        }
        if (is == null) {
            try {
                if ((is = s.getInputStream()) == null) {
                    s.close();
                    return false;
                }
            } catch (IOException ioX) {
                try {
                    s.close();
                } catch (IOException ioXX) {
                }
                s = null;
                is = null;
                return false;
            }
        }
        return true;
    }
}
