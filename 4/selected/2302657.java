package oxygen.test;

import java.net.*;
import java.util.*;
import java.io.*;

public class HttpTest {

    private static String[] DEFAULT_ARGS = new String[] { "-h", "localhost", "-p", "7001", "-path", "/dd/httptest.jsp", "-s", "ugorji", "cool", "-s", "nigeria", "cool", "-mp", "true", "-f", "file1", "C:/software/downloads/sql2diagram-sxd-1.0.5.tar.gz" };

    private String host;

    private Socket sock;

    private int port;

    private String charset = "UTF-8";

    private String boundary = "---------------------------29772313742745";

    private PrintStream diagos = System.err;

    private long sleeptime = 200l;

    public void init() throws Exception {
        sock = new Socket(host, port);
    }

    public void close() throws Exception {
        sock.close();
    }

    public void sendPostData(String path, Map<String, String> stringParams, Map<String, File> fileParams) throws Exception {
        int clen = 0;
        Map<String, String> sm = new HashMap<String, String>();
        for (Map.Entry me : stringParams.entrySet()) {
            sm.put(URLEncoder.encode((String) me.getKey()), URLEncoder.encode((String) me.getValue()));
        }
        Map<String, File> fm = new HashMap<String, File>();
        for (Map.Entry me : fileParams.entrySet()) {
            fm.put(URLEncoder.encode((String) me.getKey()), (File) me.getValue());
        }
        for (Map.Entry me : sm.entrySet()) {
            clen += ((String) me.getKey()).length() + 1 + ((String) me.getValue()).length() + 1;
        }
        for (Map.Entry me : fm.entrySet()) {
            clen += ((String) me.getKey()).length() + 1 + ((File) me.getValue()).length() + 1;
        }
        String lsep = "\r\n";
        OutputStream os = sock.getOutputStream();
        w4(os, path, "application/x-www-form-urlencoded", clen);
        w(os, "");
        try {
            Thread.sleep(2000l);
        } catch (Exception exc) {
        }
        for (Map.Entry me : sm.entrySet()) {
            w2(os, (String) me.getKey());
            w2(os, "=");
            w2(os, (String) me.getValue());
            w2(os, "&");
        }
        for (Map.Entry me : fm.entrySet()) {
            w2(os, (String) me.getKey());
            w2(os, "=");
            diagos.println("XXXXXXXXXX: Will now send file bits: " + ((File) me.getValue()).getName());
            FileInputStream fis = new FileInputStream((File) me.getValue());
            w3(fis, os, sleeptime, "XXXXXXXXXX: Sent File bits of size: ");
            w2(os, "&");
        }
        os.write("\r\n".getBytes(charset));
        os.flush();
    }

    public void sendPostDataMultipart(String path, Map<String, String> stringParams, Map<String, File> fileParams) throws Exception {
        int clen = 0;
        Map<String, String> sm = stringParams;
        Map<String, File> fm = fileParams;
        String lsep = "\r\n";
        OutputStream os = sock.getOutputStream();
        w4(os, path, "multipart/form-data; boundary=" + boundary, (Long.MAX_VALUE - 3));
        try {
            Thread.sleep(2000l);
        } catch (Exception exc) {
        }
        for (Map.Entry me : sm.entrySet()) {
            w(os, boundary);
            w(os, "Content-Disposition: form-data; name=\"" + (String) me.getKey() + "\"");
            w(os, "");
            w(os, (String) me.getValue());
        }
        for (Map.Entry me : fm.entrySet()) {
            File ff = (File) me.getValue();
            w(os, boundary);
            w(os, "Content-Disposition: form-data; name=\"" + (String) me.getKey() + "\"; filename=\"" + ff.getName() + "\"");
            w(os, "Content-Type: application/octet-stream");
            w(os, "");
            FileInputStream fis = new FileInputStream(ff);
            diagos.println("XXXXXXXXXX: Will now send file bits");
            w3(fis, os, sleeptime, "XXXXXXXXXX: Sent File bits of size: ");
            w(os, "");
        }
        w(os, boundary + "--");
        os.flush();
    }

    public void receiveResponse(OutputStream os) throws Exception {
        InputStream fis = sock.getInputStream();
        w3(fis, os, -1, "YYYYYYYYYY: Read data of size: ");
    }

    private void w(OutputStream os, String s) throws Exception {
        byte[] b = new byte[0];
        if (s != null && s.length() > 0) {
            b = s.getBytes(charset);
            os.write(b);
        }
        os.write("\r\n".getBytes(charset));
        os.flush();
        diagos.println("XXXXXXXXXX: Sent String data of size: " + b.length);
    }

    private void w2(OutputStream os, String s) throws Exception {
        os.write(s.getBytes(charset));
        os.flush();
    }

    private void w3(InputStream fis, OutputStream os, long sleep, String pfx) throws Exception {
        byte[] bb = new byte[1024];
        int bread = -1;
        while ((bread = fis.read(bb)) != -1) {
            os.write(bb, 0, bread);
            os.flush();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (Exception exc) {
                }
            }
            if (pfx != null) diagos.println(pfx + bread);
        }
    }

    private void w4(OutputStream os, String path, String contype, long clen) throws Exception {
        w(os, "POST " + path + " HTTP/1.1");
        w(os, "Host: " + host + ":" + port);
        w(os, "From: j@j.com");
        w(os, "User-Agent: XXX");
        w(os, "Keep-Alive: 300");
        w(os, "Connection: keep-alive");
        w(os, "Content-Type: " + contype);
        w(os, "Content-Length: " + clen);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) args = DEFAULT_ARGS;
        final HashMap<String, String> sm = new HashMap<String, String>();
        final HashMap<String, File> fm = new HashMap<String, File>();
        final HttpTest ht = new HttpTest();
        String path = null;
        boolean multipart = true;
        for (int i = 0; i < args.length; i++) {
            if ("-h".equals(args[i])) {
                ht.host = args[++i];
            } else if ("-p".equals(args[i])) {
                ht.port = Integer.parseInt(args[++i]);
            } else if ("-path".equals(args[i])) {
                path = args[++i];
            } else if ("-mp".equals(args[i])) {
                multipart = "true".equals(args[++i]);
            } else if ("-s".equals(args[i])) {
                sm.put(args[++i], args[++i]);
            } else if ("-f".equals(args[i])) {
                fm.put(args[++i], new File(args[++i]));
            }
        }
        ht.init();
        final String path2 = path;
        final boolean multipart2 = multipart;
        Thread t1 = new Thread() {

            public void run() {
                try {
                    if (multipart2) {
                        ht.sendPostDataMultipart(path2, sm, fm);
                    } else {
                        ht.sendPostData(path2, sm, fm);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        Thread t2 = new Thread() {

            public void run() {
                try {
                    ht.receiveResponse(System.out);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        ht.close();
    }
}
