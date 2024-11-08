package org.regadou.nalasys.system;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Array;
import org.regadou.nalasys.*;

public class Stream {

    public static final int DEFAULT_BUFFER_SIZE = 1024;

    public static final String DIRECTORY_MIMETYPE = "inode/directory";

    private static Map MIMETYPES = null;

    private static Map EXTENSIONS = null;

    private static final Class[] STREAMABLES = { char[].class, CharSequence.class, File.class, URL.class, URI.class, Stream.class, Socket.class, InputStream.class, OutputStream.class, Reader.class, Writer.class };

    private static final List STREAMABLELIST = Arrays.asList(STREAMABLES);

    public static String extensionToMimetype(String extension) {
        initMimeFile();
        Object ext = MIMETYPES.get(extension);
        if (ext == null) return null; else if (ext.getClass().isArray()) return Array.get(ext, 0).toString(); else if (ext instanceof Collection) return ((Collection) ext).iterator().next().toString(); else return ext.toString();
    }

    public static String mimetypeToExtension(String mimetype) {
        initMimeFile();
        Object mime = EXTENSIONS.get(mimetype);
        if (mime == null) return null; else if (mime.getClass().isArray()) return Array.get(mime, 0).toString(); else if (mime instanceof Collection) return ((Collection) mime).iterator().next().toString(); else return mime.toString();
    }

    public static Class[] getStreamables() {
        return STREAMABLES;
    }

    public static boolean isStreamable(Object src) {
        if (src == null) return false;
        for (Class parent = src.getClass(); parent != null; parent = parent.getSuperclass()) {
            if (STREAMABLELIST.contains(parent)) return true;
        }
        Class[] ifaces = src.getClass().getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            if (STREAMABLELIST.contains(ifaces[i])) return true;
        }
        return false;
    }

    public static byte[] getBytes(InputStream input) throws IOException {
        return getBytes(input, -1);
    }

    public static byte[] getBytes(InputStream input, int size) throws IOException {
        if (input == null) return null;
        byte dst[] = null;
        if (size < 0) {
            byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
            int got = 0;
            while (got >= 0) {
                got = input.read(buffer);
                if (got > 0) dst = concatBytes(dst, buffer, got);
            }
        } else if (size > 0) {
            dst = new byte[size];
            for (int got = 0; got < size; ) got += input.read(dst, got, size - got);
        }
        try {
            input.close();
        } catch (Exception e) {
        }
        return dst;
    }

    private Object source = null;

    private InputStream input = null;

    private OutputStream output = null;

    private BufferedReader reader = null;

    private BufferedWriter writer = null;

    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private int size = -1;

    private Date lastModified = null;

    private String mimetype;

    public Stream(Object src) {
        if (src == null) throw new RuntimeException("Source parameter is null"); else if (!isStreamable(src)) throw new RuntimeException("Invalid source parameter: " + src); else if ((src instanceof char[]) || (src instanceof CharSequence)) {
            Object s = Types.toString(src);
            if (s != null) src = s;
        }
        if (src instanceof File) {
            try {
                source = ((File) src).getCanonicalFile();
            } catch (Exception e) {
                throw new RuntimeException("File " + src + ": " + e.toString());
            }
        } else if (src instanceof URL) source = src; else if (src instanceof URI) {
            try {
                source = ((URI) src).toURL();
            } catch (Exception e) {
                throw new RuntimeException("URI " + src + ": " + e.toString());
            }
        } else if (src instanceof Stream) {
            Stream s = (Stream) src;
            if (s.source != null) source = s.source; else {
                if (s.input != null) input = s.input; else if (s.reader != null) reader = s.reader;
                if (s.output != null) output = s.output; else if (s.writer != null) writer = s.writer;
            }
        } else if (src instanceof Socket) source = src; else if (src instanceof InputStream) input = (InputStream) src; else if (src instanceof OutputStream) output = (OutputStream) src; else if (src instanceof BufferedReader) reader = (BufferedReader) src; else if (src instanceof Reader) reader = new BufferedReader((Reader) src); else if (src instanceof BufferedWriter) writer = (BufferedWriter) src; else if (src instanceof Writer) writer = new BufferedWriter((Writer) src); else throw new RuntimeException("Invalid source parameter: " + src);
    }

    public String toString() {
        String txt;
        if (source == null) {
            if (input != null) {
                if (output == null) txt = input.toString() + "->"; else txt = input.toString() + "->" + output.toString();
            } else if (output != null) txt = "->" + output.toString(); else if (reader != null) txt = reader.toString() + "->"; else if (writer != null) txt = "->" + writer.toString(); else txt = super.toString();
        } else txt = getSource();
        return "[Stream " + txt + "]";
    }

    public String getSource() {
        if (source == null) return null; else if (source instanceof Socket) {
            Socket s = (Socket) source;
            return s.getInetAddress().getCanonicalHostName() + ":" + s.getPort();
        } else return source.toString();
    }

    public InputStream getInputStream() {
        if (input == null) open("r");
        return input;
    }

    public OutputStream getOutputStream() {
        if (output == null) open("w");
        return output;
    }

    public BufferedReader getReader() {
        if (reader == null) {
            getInputStream();
            if (input != null) reader = new BufferedReader(new InputStreamReader(input));
        }
        return reader;
    }

    public BufferedWriter getWriter() {
        if (writer == null) {
            getOutputStream();
            if (output != null) writer = new BufferedWriter(new OutputStreamWriter(output));
        }
        return writer;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int size) {
        if (size > 0) bufferSize = size;
    }

    public String getMimetype() {
        if (mimetype == null) {
            if (source instanceof URL) getUrlInfo(); else if (source instanceof File) mimetype = Address.getMimetype(source);
        }
        return (mimetype == null) ? "text/plain" : mimetype;
    }

    public void setMimetype(String t) {
        if (t != null) {
            t = t.trim();
            mimetype = t.equals("") ? null : t;
        } else mimetype = null;
    }

    public Date getDate() {
        if (lastModified != null) return lastModified; else if (source instanceof URL) {
            getUrlInfo();
            return lastModified;
        } else if (source instanceof File) return new Date(((File) source).lastModified()); else return null;
    }

    public int getSize() {
        if (size >= 0) return size; else if (source instanceof URL) {
            getUrlInfo();
            return size;
        } else if (source instanceof File) {
            File f = (File) source;
            if (!f.exists()) return 0; else if (f.isDirectory()) return f.list().length; else return (int) f.length();
        } else return 0;
    }

    public boolean isFileExists() {
        return (source instanceof File) ? ((File) source).exists() : false;
    }

    public boolean isDirectory() {
        if (source instanceof File) return ((File) source).isDirectory(); else return false;
    }

    public boolean open(String mode) {
        if (source instanceof String) return false; else if (mode == null) mode = ""; else mode = mode.toLowerCase();
        boolean toread = false, towrite = false;
        if (mode.indexOf("r") >= 0) toread = true;
        if (mode.indexOf("w") >= 0) towrite = true;
        if (!toread && !towrite) toread = towrite = true;
        try {
            if (toread && input == null) {
                if (isDirectory()) return true; else if (reader != null) return true; else if (source instanceof File) input = new FileInputStream((File) source); else if (source instanceof Socket) input = ((Socket) source).getInputStream(); else if (source instanceof URL) return getUrlInfo(toread, towrite); else return false;
            }
            if (towrite && output == null) {
                if (isDirectory()) return false; else if (writer != null) return true; else if (source instanceof File) output = new FileOutputStream((File) source); else if (source instanceof Socket) output = ((Socket) source).getOutputStream(); else if (source instanceof URL) return getUrlInfo(toread, towrite); else return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        try {
            input.close();
            input = null;
        } catch (Exception e) {
        }
        try {
            output.flush();
            output.close();
            output = null;
        } catch (Exception e) {
        }
        try {
            reader.close();
            reader = null;
        } catch (Exception e) {
        }
        try {
            writer.flush();
            writer.close();
            writer = null;
        } catch (Exception e) {
        }
    }

    public boolean isReadable() {
        if (!open("r")) return false;
        try {
            if (isDirectory()) return true; else if (reader != null) return reader.ready(); else return input.available() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isWritable() {
        if (!open("w")) return false;
        return (writer != null || output != null);
    }

    public byte[] getBytes() throws IOException {
        return getBytes(input, -1);
    }

    public byte[] getBytes(int size) throws IOException {
        open("r");
        return getBytes(input, size);
    }

    public void setBytes(byte[] bytes) throws IOException {
        if (bytes == null || !open("w")) return;
        if (writer != null) {
            writer.write(new String(bytes));
            writer.flush();
        } else if (output != null) {
            output.write(bytes);
            output.flush();
        }
    }

    public String read() throws IOException {
        if (isDirectory()) return Types.toString(((File) source).list()); else if (!open("r")) return null;
        if (reader != null) return reader.readLine();
        byte[] bytes = getBytes(-1);
        return (bytes == null) ? null : new String(bytes);
    }

    public void write(String txt) throws IOException {
        if (txt == null || txt.equals("") || !open("w")) return; else if (writer != null) {
            writer.write(txt);
            writer.flush();
        } else if (output != null) {
            output.write(txt.getBytes());
            output.flush();
        }
    }

    private static byte[] concatBytes(byte first[], byte second[], int length) {
        byte dst[];
        if (first == null) {
            if (second == null) dst = new byte[0]; else {
                if (second.length < length) length = second.length;
                dst = new byte[length];
                System.arraycopy(second, 0, dst, 0, length);
            }
        } else if (second == null) {
            dst = new byte[first.length];
            System.arraycopy(first, 0, dst, 0, first.length);
        } else {
            if (second.length < length) length = second.length;
            dst = new byte[first.length + length];
            System.arraycopy(first, 0, dst, 0, first.length);
            System.arraycopy(second, 0, dst, first.length, length);
        }
        return dst;
    }

    private boolean getUrlInfo() {
        return getUrlInfo(true, false);
    }

    private boolean getUrlInfo(boolean toread, boolean towrite) {
        if (toread == (input != null) && towrite == (output != null)) return true;
        try {
            URLConnection c = ((URL) source).openConnection();
            lastModified = new Date(c.getLastModified());
            size = c.getContentLength();
            if (mimetype == null) mimetype = c.getContentType();
            if (toread) {
                InputStream i = c.getInputStream();
                if ((i != null) == (input != null)) {
                    try {
                        input.close();
                    } catch (Exception e) {
                    }
                }
                input = i;
            }
            if (towrite) {
                OutputStream o = c.getOutputStream();
                if ((o != null) == (output != null)) {
                    try {
                        output.close();
                    } catch (Exception e) {
                    }
                }
                output = o;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static void initMimeFile() {
        if (MIMETYPES != null && EXTENSIONS != null) return;
        MIMETYPES = new HashMap();
        EXTENSIONS = new HashMap();
        String mimefile = Context.getConfigValue("mimetypes");
        URL url;
        try {
            url = new URL(mimefile);
        } catch (Exception e) {
            url = Thread.currentThread().getContextClassLoader().getResource(mimefile);
        }
        try {
            Parser parser = new Parser();
            String[] lines = new String(new Stream(url).getBytes(-1)).split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int p = line.indexOf('#');
                if (p >= 0) line = line.substring(0, p);
                line = line.trim().toLowerCase();
                if (line.equals("")) continue;
                Object result = parser.parse(line);
                if (result == null || !result.getClass().isArray()) continue;
                Object[] words = (Object[]) result;
                if (words.length < 2) continue;
                String mimetype = null;
                int nbext = 0;
                char c;
                for (int w = 0; w < words.length; w++) {
                    String word = words[w].toString();
                    if ((c = word.charAt(0)) < '0' || c > 'z' || (c > '9' && c < 'a')) {
                        words[w] = null;
                        continue;
                    } else if (word.indexOf('/') > 0) {
                        if (mimetype == null) mimetype = word; else Service.debug("Several mimetypes found: " + lines[i]);
                        words[w] = null;
                    } else nbext++;
                }
                if (mimetype == null) Service.debug("Mimetype not found: " + lines[i]); else if (nbext == 0) Service.debug("Extension not found: " + lines[i]); else {
                    for (int w = 0; w < words.length; w++) {
                        if (words[w] == null) continue;
                        String ext = words[w].toString();
                        Object val = EXTENSIONS.get(mimetype);
                        if (val == null) EXTENSIONS.put(mimetype, ext); else if (val instanceof List) ((List) val).add(ext); else {
                            List lst = new ArrayList();
                            lst.add(val);
                            lst.add(ext);
                            EXTENSIONS.put(mimetype, lst);
                        }
                        val = MIMETYPES.get(ext);
                        if (val == null) MIMETYPES.put(ext, mimetype); else if (val instanceof List) ((List) val).add(mimetype); else {
                            List lst = new ArrayList();
                            lst.add(val);
                            lst.add(mimetype);
                            MIMETYPES.put(ext, lst);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Service.debug("Error while initing mime types", e);
        }
    }
}
