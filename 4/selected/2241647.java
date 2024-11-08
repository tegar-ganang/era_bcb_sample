package gullsview;

import java.io.*;
import javax.microedition.io.*;
import java.util.*;

public class MBlog {

    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static final String HEX = "0123456789ABCDEF";

    private static final String MESSAGE_PREFIX = "GV: ";

    private Main main;

    private String user, pass;

    private String encoding;

    private Json json;

    public MBlog(Main main) {
        this.main = main;
        this.encoding = "UTF-8";
        this.json = new Json();
    }

    public void setCredentials(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }

    private boolean isEmpty(String str) {
        return (str == null) || (str.trim()).length() == 0;
    }

    public boolean areCredentialsSet() {
        return !this.isEmpty(this.user) && !this.isEmpty(this.pass);
    }

    public static String urlEncode(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            if (b == ' ') {
                sb.append('+');
            } else if (((b >= '0') && (b <= '9')) || ((b >= 'a') && (b <= 'z')) || ((b >= 'A') && (b <= 'Z'))) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(HEX.charAt(b >> 4));
                sb.append(HEX.charAt(b & 0xf));
            }
        }
        return sb.toString();
    }

    private static String base64Encode(byte[] in) {
        StringBuffer sb = new StringBuffer();
        int acc = 0;
        int stack = 0;
        int count = in.length;
        for (int i = 0; i < count; i++) {
            int b = in[i] & 0xff;
            acc <<= 8;
            acc |= b;
            stack += 8;
            while (stack > 6) {
                int index = (acc >> (stack - 6)) & 0x3f;
                sb.append(BASE64_CHARS.charAt(index));
                stack -= 6;
            }
        }
        if (stack > 0) sb.append(BASE64_CHARS.charAt((acc << (6 - stack)) & 0x3f));
        if (sb.length() % 4 != 0) {
            int padding = 4 - (sb.length() % 4);
            for (int i = 0; i < padding; i++) sb.append('=');
        }
        return sb.toString();
    }

    public void send(String message) throws Exception {
        Hashtable params = new Hashtable();
        params.put("status", MESSAGE_PREFIX + message);
        this.request("http://twitter.com/statuses/update.json", true, params, true, false);
    }

    private static String strip(String str, char sep, int count) {
        int start = -1;
        for (int i = 0; i < count; i++) {
            start = str.indexOf(sep, start + 1);
            if (start < 0) return null;
        }
        int end = str.indexOf(sep, start + 1);
        String ret = (end < 0) ? str.substring(start + 1) : str.substring(start + 1, end);
        return ret.trim();
    }

    public void receive() throws Exception {
        Object root = this.request("http://twitter.com/statuses/friends_timeline.json", false, null, true, true);
        if (!(root instanceof Vector)) throw new Exception("Timeline is not an array");
        Hashtable names = new Hashtable();
        Enumeration en = ((Vector) root).elements();
        while (en.hasMoreElements()) {
            Object status = en.nextElement();
            if (!(status instanceof Hashtable)) throw new Exception("Timeline status is not an object");
            Hashtable statusht = (Hashtable) status;
            Object text = statusht.get("text");
            String textstr = (String) text;
            if (!textstr.startsWith(MESSAGE_PREFIX)) continue;
            textstr = textstr.substring(MESSAGE_PREFIX.length());
            if (!(text instanceof String)) throw new Exception("Status text is not a string");
            Object user = statusht.get("user");
            if (!(user instanceof Hashtable)) throw new Exception("Status user is not an object");
            Object name = ((Hashtable) user).get("name");
            if (!(name instanceof String)) throw new Exception("Status user name is not a string");
            String namestr = (String) name;
            if (names.containsKey(namestr)) continue;
            names.put(namestr, Boolean.TRUE);
            String where = strip(textstr, '|', 0);
            String time = strip(textstr, '|', 1);
            String latlon = strip(textstr, '|', 2);
            if ((where == null) || (time == null) || (latlon == null)) continue;
            int colon = latlon.indexOf(':');
            if (colon < 0) continue;
            double lat, lon;
            try {
                lat = Double.parseDouble(latlon.substring(0, colon));
                lon = Double.parseDouble(latlon.substring(colon + 1));
            } catch (Exception e) {
                continue;
            }
            this.main.updateSignPosition(namestr, lat, lon, where, time);
        }
    }

    private Object request(String url, boolean post, Hashtable params, boolean basicAuth, boolean processOutput) throws Exception {
        HttpConnection conn = null;
        Writer writer = null;
        InputStream is = null;
        try {
            if (!post && (params != null)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(baos, this.encoding);
                this.encodeParams(params, osw);
                osw.flush();
                osw.close();
                osw = null;
                url += "?" + new String(baos.toByteArray(), this.encoding);
                baos = null;
            }
            conn = (HttpConnection) Connector.open(url);
            conn.setRequestMethod(post ? HttpConnection.POST : HttpConnection.GET);
            if (basicAuth) {
                if (!this.areCredentialsSet()) throw new Exception("Credentials are not set");
                String token = base64Encode((this.user + ":" + this.pass).getBytes(this.encoding));
                conn.setRequestProperty("Authorization", "Basic " + token);
            }
            if (post && (params != null)) {
                OutputStream os = conn.openOutputStream();
                writer = new OutputStreamWriter(os, this.encoding);
                this.encodeParams(params, writer);
                writer.flush();
                writer.close();
                os = null;
                writer = null;
            }
            int code = conn.getResponseCode();
            if ((code != 200) && (code != 302)) throw new Exception("Unexpected response code " + code + ": " + conn.getResponseMessage());
            is = conn.openInputStream();
            if (processOutput) {
                synchronized (this.json) {
                    return this.json.parse(is);
                }
            } else {
                this.pump(is, System.out, 1024);
                return null;
            }
        } finally {
            if (writer != null) writer.close();
            if (is != null) is.close();
            if (conn != null) conn.close();
        }
    }

    private void encodeParams(Hashtable params, Writer writer) throws IOException {
        Enumeration en = params.keys();
        for (int i = 0; en.hasMoreElements(); i++) {
            if (i > 0) writer.write('&');
            String key = (String) en.nextElement();
            String value = (String) params.get(key);
            writer.write(key);
            writer.write('=');
            writer.write((urlEncode(value.getBytes(this.encoding))));
        }
    }

    private void pump(InputStream in, OutputStream out, int size) throws IOException {
        byte[] buffer = new byte[size];
        int count;
        while ((count = in.read(buffer, 0, size)) >= 0) out.write(buffer, 0, count);
        out.flush();
    }

    private static String doubleToString(double d, int decimal) {
        boolean neg = d < 0;
        d = Math.abs(d);
        long intp = (long) Math.floor(d);
        double rest = d - intp;
        StringBuffer sb = new StringBuffer();
        if (neg) sb.append('-');
        sb.append(intp);
        if (decimal > 0) {
            sb.append('.');
            for (int i = 0; i < decimal; i++) {
                rest *= 10;
                long digit = ((long) rest) % 10;
                sb.append(digit);
            }
        }
        return sb.toString();
    }

    private static void appendTwoDigit(StringBuffer sb, int value) {
        if ((value >= 0) && (value < 10)) sb.append('0');
        sb.append(value);
    }

    private static String getTimeAsString(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(millis));
        StringBuffer sb = new StringBuffer();
        appendTwoDigit(sb, cal.get(Calendar.YEAR));
        sb.append('/');
        appendTwoDigit(sb, cal.get(Calendar.MONTH) + 1);
        sb.append('/');
        appendTwoDigit(sb, cal.get(Calendar.DAY_OF_MONTH));
        sb.append(' ');
        appendTwoDigit(sb, cal.get(Calendar.HOUR_OF_DAY));
        sb.append(':');
        appendTwoDigit(sb, cal.get(Calendar.MINUTE));
        sb.append(':');
        appendTwoDigit(sb, cal.get(Calendar.SECOND));
        return sb.toString();
    }

    public void report(double lat, double lon, long time, String text) throws Exception {
        String slat = doubleToString(lat, 6);
        String slon = doubleToString(lon, 6);
        String url = "http://maps.google.com/?ie=UTF8&ll=" + MBlog.urlEncode((slat + "," + slon).getBytes("UTF-8"));
        String stime = getTimeAsString(System.currentTimeMillis());
        String message = text + " | " + stime + " | " + slat + ":" + slon + " | " + url;
        this.send(message);
    }
}
