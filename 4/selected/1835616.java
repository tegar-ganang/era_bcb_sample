package com.memoire.fu;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class with only static methods. For small and useful services.
 */
public class FuLib {

    private static final boolean DEBUG = Fu.DEBUG && true;

    private static final boolean TRACE = Fu.TRACE && true;

    public static final String DEFAULT_ENCODING = getSystemProperty("file.encoding");

    public static void sleep(long _delay) {
        FuLog.error("FLB: don't use FuLib.sleep(long), use FuLib.sleep(int) instead");
        sleep((int) _delay);
    }

    public static void sleep(int _delay) {
        try {
            Thread.sleep(_delay);
        } catch (InterruptedException ex) {
        }
    }

    public static void wait(Object _o, int _delay) {
        try {
            _o.wait(_delay);
        } catch (InterruptedException ex) {
        }
    }

    public static final String toTitleCase(String _s) {
        if (_s == null) return null;
        int l = _s.length();
        if (l == 0) return _s;
        StringBuffer r = new StringBuffer(l);
        boolean done = false;
        boolean first = true;
        for (int i = 0; i < l; i++) {
            char c = _s.charAt(i);
            if (Character.isLetter(c)) {
                if (first) {
                    c = Character.toUpperCase(c);
                    first = false;
                    done = true;
                }
            } else first = true;
            r.append(c);
        }
        return done ? r.toString() : _s;
    }

    public static final String leftpad(String _s, int _l, char _c) {
        return leftpad(_s, _l, _c, true);
    }

    public static final String leftpad(String _s, int _l, char _c, boolean _truncate) {
        String s = _s;
        if (s == null) s = "";
        int n = s.length();
        if (n == _l) return s;
        if (n > _l) return _truncate ? s.substring(n - _l) : s;
        n = _l - n;
        StringBuffer r = new StringBuffer(_l);
        while (n > 0) {
            r.append(_c);
            n--;
        }
        r.append(s);
        return r.toString();
    }

    public static final String rightpad(String _s, int _l, char _c) {
        return rightpad(_s, _l, _c, true);
    }

    public static final String rightpad(String _s, int _l, char _c, boolean _truncate) {
        String s = _s;
        if (s == null) s = "";
        int n = s.length();
        if (n == _l) return s;
        if (n > _l) return _truncate ? s.substring(0, _l) : s;
        n = _l - n;
        StringBuffer r = new StringBuffer(_l);
        r.append(s);
        while (n > 0) {
            r.append(_c);
            n--;
        }
        return r.toString();
    }

    /**
   * Return the common characters found at the beginning
   * 
   * @param _a a string
   * @param _b a string
   * @return the common start
   */
    public static final String commonStart(final String _a, final String _b) {
        int l = Math.min(_a.length(), _b.length());
        for (int i = 0; i < l; i++) if (_a.charAt(i) != _b.charAt(i)) return _a.substring(0, i);
        return _a.substring(0, l);
    }

    /**
   * Replace parts of a string.
   * 
   * @param _s the initial string
   * @param _a the string to be found
   * @param _b the string which will replace
   * @return the modified string
   */
    public static final String replace(final String _s, final String _a, String _b) {
        String b = _b;
        if (_s.indexOf(_a) < 0) return _s;
        StringBuffer r = new StringBuffer(Math.min(_s.length(), 1024));
        int la = _a.length();
        if (la == 0) throw new IllegalArgumentException("_a is empty");
        if (b == null) b = "";
        int k, i = 0;
        while ((i = _s.indexOf(_a, k = i)) >= k) {
            r.append(_s.substring(k, i));
            r.append(b);
            i += la;
        }
        r.append(_s.substring(k));
        return r.toString();
    }

    /**
   * Replace parts of a string, ignoring case.
   * 
   * @param _s the initial string
   * @param _a the string to be found
   * @param _b the string which will replace
   * @return the modified string
   */
    public static final String replaceIgnoreCase(final String _s, String _a, String _b) {
        String s = _s.toLowerCase();
        String a = _a.toLowerCase();
        String b = _b.toLowerCase();
        if (s.indexOf(a) < 0) return _s;
        StringBuffer r = new StringBuffer(Math.min(_s.length(), 1024));
        int la = a.length();
        if (la == 0) throw new IllegalArgumentException("_a is empty");
        if (b == null) b = "";
        int k, i = 0;
        while ((i = s.indexOf(a, k = i)) >= k) {
            r.append(_s.substring(k, i));
            r.append(b);
            i += la;
        }
        r.append(_s.substring(k));
        return r.toString();
    }

    /**
   * Remove parts of a string.
   * 
   * @param _s the initial string
   * @param _a the start tag
   * @param _b the end tag
   * @return the modified string
   */
    public static final String remove(final String _s, final String _a, final String _b) {
        return remove(_s, _a, _b, Integer.MAX_VALUE);
    }

    /**
   * Remove parts of a string.
   * 
   * @param _s the initial string
   * @param _a the start tag
   * @param _b the end tag
   * @return the modified string
   */
    public static final String remove(final String _s, final String _a, final String _b, final int _distance) {
        if (_s.indexOf(_a) < 0) return _s;
        StringBuffer r = new StringBuffer(Math.min(_s.length(), 1024));
        int la = _a.length();
        int lb = _b.length();
        if ((la == 0) && (lb == 0)) throw new IllegalArgumentException("both _a and _b are empty");
        int k, i = 0;
        while ((i = _s.indexOf(_a, k = i)) >= k) {
            int j = _s.indexOf(_b, i + la);
            if (j < 0) break;
            if (j - (i + la) <= _distance) {
                r.append(_s.substring(k, i));
                i = j + lb;
            } else i += la;
        }
        r.append(_s.substring(k));
        return r.toString();
    }

    /**
   * Remove parts of a string, ignoring case.
   * 
   * @param _s the initial string
   * @param _a the start tag
   * @param _b the end tag
   * @return the modified string
   */
    public static final String removeIgnoreCase(final String _s, String _a, String _b) {
        String s = _s.toLowerCase();
        String a = _a.toLowerCase();
        String b = _b.toLowerCase();
        if (s.indexOf(a) < 0) return _s;
        StringBuffer r = new StringBuffer(Math.min(s.length(), 1024));
        int la = a.length();
        int lb = b.length();
        if ((la == 0) && (lb == 0)) throw new IllegalArgumentException("both _a and _b are empty");
        int k, i = 0;
        while ((i = s.indexOf(a, k = i)) >= k) {
            int j = s.indexOf(b, i + la);
            if (j < 0) break;
            r.append(_s.substring(k, i));
            i = j + lb;
        }
        r.append(_s.substring(k));
        return r.toString();
    }

    /**
   * Test the start of a string, ignoring case.
   * 
   * @param _s the string
   * @param _a the start
   * @return true if _s starts with _a
   */
    public static final boolean startsWithIgnoreCase(String _s, String _a) {
        return _s.toLowerCase().startsWith(_a.toLowerCase());
    }

    /**
   * Look for a string in an array.
   * 
   * @param _a the array
   * @param _s the string to find
   * @return true if the string is in the array
   */
    public static final boolean contains(String[] _a, String _s) {
        int l = _a.length;
        for (int i = 0; i < l; i++) if (_s.equals(_a[i])) return true;
        return false;
    }

    /**
   * Append a string to an array of String.
   * 
   * @param _a the array
   * @param _s the string to append
   * @return the array of strings
   */
    public static final String[] append(String[] _a, String _s) {
        int l = _a.length;
        String[] r = new String[l + 1];
        System.arraycopy(_a, 0, r, 0, l);
        r[l] = _s;
        return r;
    }

    /**
   * Split a string according to a given separator, ignoring empty fields.
   * 
   * @param _s the initial string
   * @param _c the separator
   * @return the array of strings
   */
    public static final String[] split(String _s, char _c) {
        return split(_s, _c, true, false);
    }

    /**
   * Split a string according to a given separator.
   * 
   * @param _s the initial string
   * @param _c the separator
   * @param _empty accept empty field
   * @param _trim trim the field
   * @return the array of strings
   */
    public static final String[] split(String _s, char _c, boolean _empty, boolean _trim) {
        FuVectorString r = new FuVectorString();
        int l = _s.length();
        int p = -1;
        for (int i = 0; i < l; i++) {
            if (_s.charAt(i) == _c) {
                String f = (p + 1 < i) ? _s.substring(p + 1, i) : "";
                if (_trim) f = f.trim();
                if (_empty || !"".equals(f)) r.addElement(f);
                p = i;
            }
        }
        {
            String f = (p >= l - 1) ? "" : _s.substring(p + 1);
            if (_trim) f = f.trim();
            if (_empty || !"".equals(f)) r.addElement(f);
        }
        return r.toArray();
    }

    /**
   * Concatenate an array of strings according to a given separator.
   * 
   * @param _s the array
   * @param _c the separator
   * @return the built string
   */
    public static final String join(String[] _s, char _c) {
        StringBuffer r = new StringBuffer();
        int l = _s.length;
        for (int i = 0; i < l; i++) {
            if (_s[i] != null) {
                if (i > 0) r.append(_c);
                r.append(_s[i]);
            }
        }
        return r.toString();
    }

    /**
   * Replace any non letter or digit char by an underscore.
   * 
   * @param _s the initial string
   * @return the modified string
   */
    public static final String clean(String _s) {
        return FuText.clean(_s, '_');
    }

    /**
   * Return a specified area of a string.
   * 
   * @param _s the initial string
   * @param _x the starting column
   * @param _y the starting row
   * @param _w the number of columns
   * @param _h the number of rows
   * @return the array of strings
   */
    public static final String cut(String _s, int _x, int _y, int _w, int _h) {
        if ((_w <= 0) || (_h <= 0)) return "";
        int l = _s.length();
        char[] c = new char[l];
        _s.getChars(0, l, c, 0);
        StringBuffer r = new StringBuffer((_w + 1) * _h);
        int x = -1, y = -1;
        for (int i = 0; i < l; i++) {
            if (c[i] == '\n') {
                y++;
                x = -1;
                if (y < _y) continue;
                if (y >= _y + _h) break;
                r.append('\n');
                continue;
            }
            x++;
            if (x < _x) continue;
            if (x >= _x + _w) continue;
            r.append(c[i]);
        }
        return r.toString();
    }

    /**
   * @return the current time in seconds.
   */
    public static final int currentTimeSeconds() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    /**
   * @return the current time in milliseconds.
   */
    public static final long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static final String getSystemProperty(String _property) {
        String r = null;
        try {
            r = System.getProperty(_property);
        } catch (SecurityException th) {
        }
        return r;
    }

    public static final String setSystemProperty(String _property, String _value) {
        if (_value == null) return null;
        String r = null;
        try {
            System.getProperties().put(_property, _value);
            r = _value;
        } catch (SecurityException th) {
            r = getSystemProperty(_property);
        }
        return r;
    }

    public static final String getJavaHome() {
        String r = getSystemProperty("java.home");
        if (r == null) r = "";
        return r;
    }

    public static final String getJavaTmp() {
        String r = getSystemProperty("java.io.tmpdir");
        if (r == null) r = "";
        return r;
    }

    public static final String getUserHome() {
        String r = getSystemProperty("user.home");
        if (r == null) r = "";
        return r;
    }

    public static final String getUserDir() {
        String r = getSystemProperty("user.dir");
        if (r == null) r = "";
        return r;
    }

    /**
   * @return a nicely formated date (today).
   */
    public static final String date() {
        return date(System.currentTimeMillis(), DateFormat.LONG);
    }

    /**
   * @return a nicely formated date.
   */
    public static final String date(long _time) {
        return date(_time, DateFormat.LONG);
    }

    /**
   * @return a nicely formated date.
   */
    public static final String date(long _time, int _format) {
        Date now = new Date(_time);
        Calendar cal = Calendar.getInstance();
        DateFormat fmt;
        cal.setTime(now);
        fmt = DateFormat.getDateInstance(_format, Locale.getDefault());
        fmt.setCalendar(cal);
        return fmt.format(now);
    }

    /**
   * @return a nicely formated date.
   */
    public static final String date(long _time, String _simple) {
        Date now = new Date(_time);
        Calendar cal = Calendar.getInstance();
        DateFormat fmt;
        cal.setTime(now);
        fmt = new SimpleDateFormat(_simple, Locale.getDefault());
        fmt.setCalendar(cal);
        return fmt.format(now);
    }

    /**
   * @return a nicely formated time (today).
   */
    public static final String time() {
        return time(System.currentTimeMillis(), DateFormat.MEDIUM);
    }

    /**
   * @return a nicely formated date.
   */
    public static final String time(long _time) {
        return time(_time, DateFormat.MEDIUM);
    }

    /**
   * @return a nicely formated date.
   */
    public static final String time(long _time, int _format) {
        Date now = new Date(_time);
        Calendar cal = Calendar.getInstance();
        DateFormat fmt;
        cal.setTime(now);
        fmt = DateFormat.getTimeInstance(_format, Locale.getDefault());
        fmt.setCalendar(cal);
        String r = fmt.format(now);
        int i = r.indexOf(' ');
        if (i >= 0) r = r.substring(0, i);
        return r;
    }

    /**
   * @return a nicely formated date.
   */
    public static final String time(long _time, String _simple) {
        Date now = new Date(_time);
        Calendar cal = Calendar.getInstance();
        DateFormat fmt;
        cal.setTime(now);
        fmt = new SimpleDateFormat(_simple);
        fmt.setCalendar(cal);
        return fmt.format(now);
    }

    /**
   * @return a nicely formated duration.
   */
    public static final String duration(long _d) {
        return duration(_d, false);
    }

    /**
   * @return a nicely formated duration.
   */
    public static final String duration(long _d, boolean _short) {
        long d = _d;
        d = d / 1000;
        long s = d % 60;
        d /= 60;
        long m = d % 60;
        d /= 60;
        long h = d % 24;
        d /= 24;
        long j = d % 30;
        d /= 30;
        long n = d % 12;
        d /= 12;
        long a = d;
        String r = "";
        if (_short) {
            if (h > 0) {
                r += h;
                r += 'h';
            }
            if (m > 0) {
                r += m;
                r += "min";
            }
            if (s > 0) {
                if (s < 10) r += "0";
                r += s;
                r += 's';
            }
            if ((j > 0) || (n > 0) || (a > 0)) {
                r = j + "j" + r;
                if (a > 0) r = n + "m" + r;
                if (a > 0) r = a + "a" + r;
            }
            if ("".equals(r)) r = "0s";
        } else {
            r += h;
            r += ':';
            if (m < 10) r += "0";
            r += m;
            r += ':';
            if (s < 10) r += "0";
            r += s;
            if ((j > 0) || (n > 0) || (a > 0)) {
                r = j + "j " + r;
                if (a > 0) r = n + "m " + r;
                if (a > 0) r = a + "a " + r;
            }
        }
        return r;
    }

    /**
   * Safely close an input stream.
   * 
   * @param _in the input stream, may be null.
   */
    public static void safeClose(final InputStream _in) {
        if (_in != null) {
            try {
                _in.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
   * Safely close an output stream.
   * 
   * @param _out the output stream, may be null.
   */
    public static void safeClose(final OutputStream _out) {
        if (_out != null) {
            try {
                _out.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
   * Safely close a reader.
   * 
   * @param _in the reader, may be null.
   */
    public static void safeClose(final Reader _in) {
        if (_in != null) {
            try {
                _in.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
   * Safely close a writer.
   * 
   * @param _out the writer, may be null.
   */
    public static void safeClose(final Writer _out) {
        if (_out != null) {
            try {
                _out.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void readFully(InputStream _in, byte[] _out, int _offset, int _length) throws IOException {
        int length = _length;
        int offset = _offset;
        while (length > 0) {
            int n = _in.read(_out, offset, length);
            if (n == -1) throw new EOFException();
            offset += n;
            length -= n;
        }
    }

    public static void copyFully(InputStream _in, OutputStream _out) throws IOException {
        byte[] buffer = null;
        try {
            buffer = FuFactoryByteArray.get(32768, -1, false);
            final int lb = buffer.length;
            int nr;
            while ((nr = _in.read(buffer, 0, lb)) >= 0) _out.write(buffer, 0, nr);
        } finally {
            FuFactoryByteArray.release(buffer);
        }
    }

    public static void skipFully(InputStream _in, long _length) throws IOException {
        long length = _length;
        if (length > 0) {
            byte[] buffer = null;
            try {
                int n = (int) Math.min(32768, length);
                buffer = FuFactoryByteArray.get(n, -1, false);
                while (length > 0L) {
                    n = (int) Math.min(buffer.length, length);
                    readFully(_in, buffer, 0, n);
                    length -= n;
                }
            } finally {
                FuFactoryByteArray.release(buffer);
            }
        }
    }

    public static String[] splitCommandLine(String _s) {
        StreamTokenizer t = new StreamTokenizer(new StringReader(_s));
        t.resetSyntax();
        t.wordChars(0x0000, 0xFFFF);
        t.whitespaceChars(' ', ' ');
        t.whitespaceChars('\t', '\t');
        t.whitespaceChars('\n', '\n');
        t.whitespaceChars('\r', '\r');
        t.quoteChar('\'');
        t.quoteChar('\"');
        t.commentChar('#');
        t.ordinaryChar(';');
        t.slashSlashComments(false);
        t.slashStarComments(false);
        t.eolIsSignificant(false);
        FuVectorString r = new FuVectorString(20);
        try {
            boolean exit = false;
            while (!exit) {
                t.nextToken();
                String ss = null;
                switch(t.ttype) {
                    case StreamTokenizer.TT_EOF:
                        exit = true;
                        break;
                    case StreamTokenizer.TT_EOL:
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        ss = t.sval;
                        break;
                    case StreamTokenizer.TT_WORD:
                        ss = t.sval;
                        break;
                    case '"':
                        ss = t.sval;
                        break;
                    case '\'':
                        ss = t.sval;
                        break;
                    default:
                        ss = "" + (char) t.ttype;
                        break;
                }
                if (ss != null) r.addElement(ss);
            }
        } catch (IOException ex) {
            FuLog.error(ex);
        }
        return r.toArray();
    }

    /**
   * Start an external program. With exception.
   * 
   * @param _cmd the command line
   * @return a runnable to stop it
   */
    public static Runnable[] startProgram(String[] _cmd) throws IOException {
        if (TRACE && FuLog.isTrace()) {
            StringBuffer sb = new StringBuffer("FLB: startProgram");
            for (int i = 0; i < _cmd.length; i++) {
                sb.append(' ');
                sb.append(_cmd[i]);
            }
            FuLog.trace(sb.toString());
        }
        final Process proc = Runtime.getRuntime().exec(_cmd);
        final boolean[] exited = new boolean[1];
        final Thread thread = new Thread(new Runnable() {

            public void run() {
                InputStream sin1 = null;
                InputStream sin2 = null;
                try {
                    sin1 = new BufferedInputStream(proc.getInputStream());
                    sin2 = new BufferedInputStream(proc.getErrorStream());
                    while (!exited[0] && ((sin1 != null) || (sin2 != null))) {
                        boolean wait = true;
                        int c = 0;
                        try {
                            while ((sin1 != null) && (sin1.available() > 0)) {
                                c = sin1.read();
                                if (c == -1) {
                                    sin1 = null;
                                    break;
                                }
                                wait = false;
                            }
                        } catch (IOException ex) {
                            sin1 = null;
                        }
                        try {
                            while ((sin2 != null) && (sin2.available() > 0)) {
                                c = sin2.read();
                                if (c == -1) {
                                    sin2 = null;
                                    break;
                                }
                                wait = false;
                            }
                        } catch (IOException ex) {
                            sin2 = null;
                        }
                        if (wait) {
                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                    while ((sin1 != null) && (sin1.available() > 0)) sin1.read();
                    while ((sin2 != null) && (sin2.available() > 0)) sin2.read();
                } catch (IOException ex) {
                } finally {
                    try {
                        if (sin1 != null) sin1.close();
                    } catch (IOException _evt) {
                        FuLog.error(_evt);
                    }
                    try {
                        if (sin2 != null) sin2.close();
                    } catch (IOException _evt) {
                        FuLog.error(_evt);
                    }
                }
            }
        }, "Command " + _cmd[0]);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
        thread.start();
        return new Runnable[] { new Runnable() {

            public void run() {
                try {
                    proc.waitFor();
                } catch (InterruptedException ex) {
                }
                try {
                    proc.waitFor();
                } catch (InterruptedException ex) {
                }
                exited[0] = true;
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                }
            }
        }, new Runnable() {

            public void run() {
                proc.destroy();
                try {
                    proc.waitFor();
                } catch (InterruptedException ex) {
                }
                exited[0] = true;
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                }
            }
        } };
    }

    public static String runProgram(String[] _cmd) throws IOException {
        return runProgram(_cmd, null);
    }

    /**
   * Runs an external program. With exception.
   * @param _cmd
   * @param _dir
   */
    public static String runProgram(String[] _cmd, File _dir) throws IOException {
        return runProgram(_cmd, _dir, null, null);
    }

    /**
   * Runs an external program, with errors and outputs in differents buffers. With exception.
   * 
   * @param _cmd the command line
   * @param _sbout Output stringbuffer. Can be null.
   * @param _sberr Errors stringbuffer. Ca be null.
   * @return the output of the command execution
   */
    public static String runProgram(String[] _cmd, File _dir, final StringBuffer _sbout, final StringBuffer _sberr) throws IOException {
        final Process proc = _dir == null ? Runtime.getRuntime().exec(_cmd) : Runtime.getRuntime().exec(_cmd, null, _dir);
        final ByteArrayOutputStream sall = new ByteArrayOutputStream();
        final ByteArrayOutputStream serr = new ByteArrayOutputStream();
        final ByteArrayOutputStream sout = new ByteArrayOutputStream();
        final boolean[] exited = new boolean[1];
        Thread thread = new Thread(new Runnable() {

            public void run() {
                InputStream sin1 = null;
                InputStream sin2 = null;
                try {
                    sin1 = new BufferedInputStream(proc.getInputStream());
                    sin2 = new BufferedInputStream(proc.getErrorStream());
                    while (!exited[0] && ((sin1 != null) || (sin2 != null))) {
                        boolean wait = true;
                        int c = 0;
                        try {
                            while ((sin1 != null) && (sin1.available() > 0)) {
                                c = sin1.read();
                                if (c == -1) {
                                    sin1 = null;
                                    break;
                                }
                                sout.write(c);
                                sall.write(c);
                                wait = false;
                            }
                        } catch (IOException ex) {
                            sin1 = null;
                        }
                        try {
                            while ((sin2 != null) && (sin2.available() > 0)) {
                                c = sin2.read();
                                if (c == -1) {
                                    sin2 = null;
                                    break;
                                }
                                serr.write(c);
                                sall.write(c);
                                wait = false;
                            }
                        } catch (IOException ex) {
                            sin2 = null;
                        }
                        if (wait) {
                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                    while ((sin1 != null) && (sin1.available() > 0)) {
                        int c = sin1.read();
                        sout.write(c);
                        sall.write(c);
                    }
                    while ((sin2 != null) && (sin2.available() > 0)) {
                        int c = sin2.read();
                        serr.write(c);
                        sall.write(c);
                    }
                } catch (IOException ex) {
                } finally {
                    try {
                        sall.flush();
                        sout.flush();
                        serr.flush();
                    } catch (IOException _exc) {
                        FuLog.error(_exc);
                    }
                    try {
                        if (sin1 != null) sin1.close();
                    } catch (IOException _evt) {
                        FuLog.error(_evt);
                    }
                    try {
                        if (sin2 != null) sin2.close();
                    } catch (IOException _evt) {
                        FuLog.error(_evt);
                    }
                }
            }
        }, "Command " + _cmd[0]);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.currentThread().getPriority() - 1));
        thread.start();
        try {
            proc.waitFor();
        } catch (InterruptedException ex) {
        }
        exited[0] = true;
        try {
            thread.join();
        } catch (InterruptedException ex) {
        }
        if (_sberr != null) _sberr.append(serr.toString());
        if (_sbout != null) _sbout.append(sout.toString());
        return new String(sall.toByteArray());
    }

    /**
   * Runs an external command inside a shell. No exception.
   * 
   * @param _cmd the command line
   * @return the output of the command execution
   */
    public static String runShellCommand(String _cmd) {
        try {
            return runProgram(isWindows() ? new String[] { "cmd", "/C", _cmd } : new String[] { "/bin/sh", "-c", _cmd });
        } catch (IOException ex) {
            return "Could not run the command:\n" + "  " + _cmd;
        }
    }

    /**
   * Reduce a path by replacing the user's home dir.
   * 
   * @return the reduced path.
   */
    public static final String reducedPath(String _p) {
        String r = _p;
        String h = getSystemProperty("user.home");
        if (r.startsWith(h)) r = "~" + r.substring(h.length());
        return r;
    }

    /**
   * Expand a path with the full user's home dir.
   * 
   * @return the expanded path.
   */
    public static final String expandedPath(String _p) {
        String r = _p;
        String h = getSystemProperty("user.home");
        if (r.startsWith("~")) r = h + r.substring(1);
        return r;
    }

    /**
   * @return the filename part of a path.
   */
    public static final String fileName(String _p) {
        String r = _p;
        String s = getSystemProperty("file.separator");
        int i = r.lastIndexOf(s);
        if (i >= 0) r = r.substring(i + 1);
        return r;
    }

    /**
   * @deprecated
   */
    public static final String encode(String _s) {
        return encodeWwwFormUrl(_s);
    }

    /**
   * @deprecated
   */
    public static final String decode(String _s) {
        return decodeWwwFormUrl(_s);
    }

    /**
   * Encodes a string (x-www-form-urlencoded).
   * 
   * @return the encoded value.
   */
    public static final String encodeWwwFormUrl(String _s) {
        try {
            return URLEncoder.encode(_s, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException _evt) {
            FuLog.error(_evt);
        }
        return null;
    }

    /**
   * Decodes a string (x-www-form-urlencoded).
   * 
   * @return the decoded value.
   */
    public static final String decodeWwwFormUrl(String _s) {
        if (jdk() >= 1.2) {
            try {
                return URLDecoder.decode(_s, DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException _evt) {
                FuLog.error(_evt);
            }
            return null;
        }
        int l = _s.length();
        StringBuffer sb = new StringBuffer(l);
        for (int i = 0; i < l; i++) {
            char c = _s.charAt(i);
            switch(c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char) Integer.parseInt(_s.substring(i + 1, i + 3), 16));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException();
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        String r = sb.toString();
        return r;
    }

    /**
   * Encodes a string using quoted printable.
   * 
   * @return the encoded value.
   */
    public static final String encodeQuotedPrintable(String _s) {
        int l = _s.length();
        StringBuffer sb = new StringBuffer(2 * l);
        for (int i = 0; i < l; i++) {
            char c = _s.charAt(i);
            if (Character.isLetterOrDigit(c)) sb.append(c); else {
                int n = c;
                if (n < 256) {
                    sb.append('=');
                    String v = Integer.toHexString(c).toUpperCase();
                    if (v.length() < 2) sb.append('0');
                    sb.append(v);
                }
            }
        }
        return sb.toString();
    }

    /**
   * Decodes a quoted-printablestring.
   * 
   * @return the decoded value.
   */
    public static final String decodeQuotedPrintable(String _s) {
        if (_s.indexOf("=") < 0) return _s;
        int l = _s.length();
        StringBuffer sb = new StringBuffer(l);
        for (int i = 0; i < l; i++) {
            char c = _s.charAt(i);
            switch(c) {
                case '=':
                    try {
                        sb.append((char) Integer.parseInt(_s.substring(i + 1, i + 3), 16));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException();
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static final char[] ENC64 = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    private static final byte[] DEC64 = { 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 127, 62, 127, 127, 127, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 127, 127, 127, 126, 127, 127, 127, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 127, 127, 127, 127, 127, 127, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 127, 127, 127, 127, 127 };

    /**
   * Encodes a string using base 64.
   * 
   * @return the encoded value.
   */
    public static final String encodeBase64(byte[] _s) {
        byte[] b = _s;
        int l = b.length;
        StringBuffer sb = new StringBuffer(2 * l);
        for (int i = 0; i < l; i += 3) {
            int c0 = b[i + 0];
            int c1 = (i + 1 < l) ? b[i + 1] : 0;
            int c2 = (i + 2 < l) ? b[i + 2] : 0;
            if (c0 < 0) c0 += 256;
            if (c1 < 0) c1 += 256;
            if (c2 < 0) c2 += 256;
            char d0 = ENC64[c0 >> 2];
            char d1 = ENC64[((c0 & 0x03) << 4) | (c1 >> 4)];
            char d2 = ENC64[((c1 & 0x0F) << 2) | (c2 >> 6)];
            char d3 = ENC64[c2 & 0x3F];
            if (i + 2 >= l) d3 = '=';
            if (i + 1 >= l) d2 = '=';
            sb.append(d0);
            sb.append(d1);
            sb.append(d2);
            sb.append(d3);
        }
        return sb.toString();
    }

    /**
   * Decodes a string using base 64.
   * 
   * @return the decoded value.
   */
    public static final byte[] decodeBase64(String _s) {
        int l = _s.length();
        byte[] r = new byte[l];
        int j = 0;
        for (int i = 0; i < l; i += 4) {
            int d0 = _s.charAt(i + 0);
            int d1 = _s.charAt(i + 1);
            int d2 = _s.charAt(i + 2);
            int d3 = _s.charAt(i + 3);
            int m = 3;
            d0 = DEC64[d0];
            d1 = DEC64[d1];
            if (d2 == 126) {
                d2 = 0;
                m = 1;
            } else d2 = DEC64[d2];
            if (d3 == 126) {
                d3 = 0;
                m = 2;
            } else d3 = DEC64[d3];
            int n = (d0 << 18) + (d1 << 12) + (d2 << 6) + d3;
            r[j] = (byte) ((n >> 16) & 0xFF);
            j++;
            if (m > 2) {
                r[j] = (byte) ((n >> 8) & 0xFF);
                j++;
            }
            if (m > 1) {
                r[j] = (byte) ((n) & 0xFF);
                j++;
            }
        }
        byte[] v = new byte[j];
        System.arraycopy(r, 0, v, 0, j);
        return v;
    }

    /**
   * @return the url representing the file.
   */
    public static final URL toURL(File _f) throws MalformedURLException {
        URL r = null;
        if (_f != null) {
            if (FuLib.jdk() >= 1.4) {
                r = _f.toURI().toURL();
            } else {
                String p = _f.getAbsolutePath();
                if (File.separatorChar != '/') p = p.replace(File.separatorChar, '/');
                if (!p.startsWith("/")) p = "/" + p;
                if (_f.isDirectory() && !p.endsWith("/")) p = p + "/";
                String[] a = split(p, '/');
                for (int i = 0; i < a.length; i++) a[i] = encodeWwwFormUrl(a[i]);
                p = join(a, '/');
                r = new URL("file", "", p);
            }
        }
        return r;
    }

    /**
   * @return the decoded path part of the url.
   */
    public static final String toFilePath(URL _u) {
        if (_u == null) return null;
        String p = _u.getPath();
        if (p == null) return null;
        if (File.separatorChar != '/') p = p.replace('/', File.separatorChar);
        return decodeWwwFormUrl(p);
    }

    /**
   * @return the parent file.
   */
    public static final File getParentFile(File _f) {
        File r = null;
        if (_f != null) {
            if (FuLib.jdk() >= 1.2) {
                r = _f.getParentFile();
            } else {
                String p = _f.getParent();
                if (p != null) r = new File(p);
            }
        }
        return r;
    }

    /**
   * @return true if the file is bzip2ed.
   */
    public static final boolean isBzip2ed(String _p) {
        boolean r = false;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(_p);
            r = isBzip2ed(fis);
        } catch (IOException ex) {
        } finally {
            safeClose(fis);
        }
        return r;
    }

    /**
   * @return true if the input stream is gziped.
   */
    public static final boolean isBzip2ed(InputStream _in) throws IOException {
        if (!_in.markSupported()) throw new IOException("mark/reset not supported for " + _in.getClass().getName());
        boolean r = false;
        _in.mark(3);
        try {
            int c1 = _in.read();
            int c2 = _in.read();
            int c3 = _in.read();
            r = (c1 == 'B') && (c2 == 'Z') && (c3 == 'h');
        } catch (IOException ex) {
        } finally {
            _in.reset();
        }
        return r;
    }

    /**
   * @return true if the file is gziped.
   */
    public static final boolean isGziped(String _p) {
        boolean r = false;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(_p);
            r = isGziped(fis);
        } catch (IOException ex) {
        } finally {
            safeClose(fis);
        }
        return r;
    }

    /**
   * @return true if the input stream is gziped.
   */
    public static final boolean isGziped(InputStream _in) throws IOException {
        if (!_in.markSupported()) throw new IOException("mark/reset not supported for " + _in.getClass().getName());
        boolean r = false;
        _in.mark(2);
        try {
            int c1 = _in.read();
            int c2 = _in.read();
            r = (c1 == 0x1F) && (c2 == 0x8B);
        } catch (IOException ex) {
        } finally {
            _in.reset();
        }
        return r;
    }

    /**
   * @return true if the class of the given name exists.
   */
    public static final boolean classExists(String _s) {
        boolean r = false;
        try {
            ClassLoader l = FuLib.class.getClassLoader();
            Class c = (l == null ? Class.forName(_s) : l.loadClass(_s));
            r = (c != null);
        } catch (Throwable th) {
        }
        return r;
    }

    /**
   * @return null or an instance of a class.
   */
    public static final Object classInstance(String _s) {
        Object r = null;
        try {
            Class c = Class.forName(_s);
            r = c.newInstance();
        } catch (Exception ex) {
        }
        return r;
    }

    /**
   * @return the double release number of the JVM.
   */
    public static final double jdk() {
        return JDK;
    }

    private static final double JDK = computeJDK();

    private static final double computeJDK() {
        double r = 1.1;
        try {
            r = new Double(getJavaVersion().substring(0, 3)).doubleValue();
        } catch (Exception ex) {
            FuLog.warning("FLB: not reconized java version: " + getJavaVersion(), ex);
        }
        return r;
    }

    public static final String getJavaVersion() {
        String r = getSystemProperty("java.version");
        if (r == null) {
            r = "1.1.8";
            if (DEBUG) FuLog.debug("FLB: unknown JDK version, switching to 1.1.8.");
        } else {
            int p = r.indexOf('-');
            if (p > 0) r = r.substring(0, p);
        }
        return r;
    }

    /**
   * @return true if os is linux
   */
    public static final boolean isLinux() {
        return (getSystemProperty("os.name").startsWith("Linux"));
    }

    /**
   * @return true if os is mac osx
   */
    public static final boolean isMacOSX() {
        return (getSystemProperty("os.name").startsWith("Mac OS X"));
    }

    /**
   * @return true if os is mac
   */
    public static final boolean isMachintosh() {
        return (getSystemProperty("os.name").startsWith("Mac"));
    }

    /**
   * @return true if os is windows
   */
    public static final boolean isWindows() {
        return (getSystemProperty("os.name").startsWith("Win"));
    }

    /**
   * @return true if os is unix
   */
    public static final boolean isUnix() {
        return isMacOSX() || (!isWindows() && !isMachintosh());
    }

    /**
   * @return true if jre is kaffe
   */
    public static final boolean isKaffe() {
        return "Kaffe.org project".equals(getSystemProperty("java.vendor"));
    }

    /**
   * @return true if jdistro is running
   */
    public static final boolean isJDistroRunning() {
        return FuLib.getSystemProperty("korte.running") != null;
    }

    /**
   * @return true if korte is running
   */
    public static final boolean isKorteRunning() {
        return "true".equals(FuLib.getSystemProperty("korte.running"));
    }

    /**
   * @return true if wharf is running
   */
    public static final boolean isWharfRunning() {
        return "true".equals(FuLib.getSystemProperty("wharf.running"));
    }

    /**
   * @return true if eper is running
   */
    public static final boolean isEperRunning() {
        return "true".equals(FuLib.getSystemProperty("eper.running"));
    }

    public static final URL createURL(String _url) {
        URL r = null;
        try {
            r = new URL(_url);
        } catch (MalformedURLException ex1) {
            try {
                File f = new File(_url);
                if (f.exists()) r = toURL(f);
            } catch (MalformedURLException ex2) {
            }
        }
        return r;
    }

    public static final URL createURL(File _file) {
        URL r = null;
        try {
            r = toURL(_file);
        } catch (MalformedURLException ex) {
            throw new RuntimeException("MalformedURLException: " + _file);
        }
        return r;
    }

    public static final String toLocalPath(URL _url) {
        if (!"file".equals(_url.getProtocol())) throw new IllegalArgumentException("bad protocol");
        String t = _url.getFile();
        t = FuLib.decodeWwwFormUrl(t).replace('/', File.separatorChar);
        return t;
    }

    /**
   * Turns on or off the accessibility of a member.
   */
    public static final void setAccessible(Member _m, boolean _b) {
        if (jdk() >= 1.2) {
            if (_m instanceof AccessibleObject) ((AccessibleObject) _m).setAccessible(_b);
        }
    }

    /**
   * Set a new protocol for URLs.
   */
    public static final void setUrlHandler(final String _protocol, final URLStreamHandler _handler) {
        if (TRACE && FuLog.isTrace()) {
            if (_handler == null) FuLog.trace("FLB: remove the " + _protocol + " protocol for URLs"); else FuLog.trace("FLB: set the " + _protocol + " protocol for URLs");
        }
        try {
            Field field = URL.class.getDeclaredField("handlers");
            setAccessible(field, true);
            Hashtable t = (Hashtable) field.get(null);
            if (_handler == null) t.remove(_protocol); else t.put(_protocol, _handler);
        } catch (Exception ex) {
            if (!isKaffe()) FuLog.error("FLB: setUrlHandler", ex); else FuLog.warning("FLB: skip URL handler in Kaffe: FuLib#1787");
        }
    }

    /**
   * Returns the properties of an object. [[name,value] ...]
   */
    public static final Object[][] getProperties(Object _o) {
        Object[][] r = null;
        if (_o != null) {
            try {
                BeanInfo bi = Introspector.getBeanInfo(_o.getClass());
                PropertyDescriptor[] pd = bi.getPropertyDescriptors();
                int nd = pd.length;
                r = new Object[nd][2];
                for (int i = 0; i < nd; i++) {
                    r[i][0] = pd[i].getDisplayName();
                    try {
                        Method rm = pd[i].getReadMethod();
                        r[i][1] = rm.invoke(_o, FuEmptyArrays.OBJECT0);
                    } catch (Exception ex) {
                    }
                }
                FuSort.sort(r, new FuComparator() {

                    public int compare(Object _a, Object _b) {
                        return ((Object[]) _a)[0].toString().compareTo(((Object[]) _b)[0].toString());
                    }
                });
            } catch (Exception ex) {
            }
        }
        if (r == null) r = new Object[0][2];
        return r;
    }

    /**
   * Returns the pairs of properties. [[name,value] ...]
   */
    public static final String[][] convertPropertiesToArray(Properties _t) {
        String[][] r = null;
        if (_t != null) {
            synchronized (_t) {
                FuVectorString v = new FuVectorString(256);
                Enumeration e = _t.propertyNames();
                while (e.hasMoreElements()) v.addElement((String) e.nextElement());
                e = null;
                v.sort();
                int l = v.size();
                r = new String[l][2];
                for (int i = 0; i < l; i++) {
                    r[i][0] = v.elementAt(i);
                    r[i][1] = _t.getProperty(r[i][0]);
                }
                v = null;
            }
        }
        if (r == null) r = new String[0][2];
        return r;
    }

    /**
   * Returns the pairs of a hashtable. [[name,value] ...]
   */
    public static final Object[][] convertHashtableToArray(Map _t) {
        Object[][] r = null;
        if (_t != null) {
            synchronized (_t) {
                int n = _t.size();
                int i = 0;
                Iterator e = _t.keySet().iterator();
                Object[] k = new Object[n];
                while (e.hasNext()) {
                    k[i] = e.next();
                    i++;
                }
                FuSort.sort(k);
                r = new Object[n][2];
                i = 0;
                while (i < n) {
                    r[i][0] = k[i];
                    r[i][1] = _t.get(k[i]);
                    i++;
                }
            }
        }
        if (r == null) r = new Object[0][2];
        return r;
    }

    public static final String codeLocation() {
        String r = "";
        try {
            throw new RuntimeException();
        } catch (Throwable th) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            PrintWriter pw = new PrintWriter(out);
            th.printStackTrace(pw);
            pw.close();
            r = new String(out.toByteArray());
            int i;
            i = r.indexOf('\n');
            if (i >= 0) r = r.substring(i + 1);
            i = r.indexOf('\n');
            if (i >= 0) r = r.substring(i + 1);
            i = r.indexOf('\n');
            if (i >= 0) r = r.substring(i + 1);
            i = r.indexOf('\n');
            if (i >= 0) r = r.substring(0, i);
            i = r.indexOf('(');
            if (i >= 0) r = r.substring(i + 1);
            i = r.indexOf(')');
            if (i >= 0) r = r.substring(0, i);
        }
        return r;
    }
}
