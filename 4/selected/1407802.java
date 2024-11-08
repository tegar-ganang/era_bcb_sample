package com.protomatter.util;

import java.util.*;
import java.io.*;

/**
 *  A MIME encoded message.
 *  This is basically a collection of MIMEAttachment objects. This
 *  class takes care of the ASCII encoding of the message as a whole,
 *  including the segment boundary, etc...  It does <b><i>NOT</i></b>
 *  take care of any headers other than the Content-Type, which it
 *  always identifies as "MULTIPART/MIXED".
 *  This class can also be used to parse "file upload" information
 *  out of HTML forms.
 *
 *  @see MIMEAttachment
 */
public class MIMEMessage implements Serializable {

    private Vector attachments;

    private String boundary;

    private static String CRLF = "\r\n";

    /**
   *  Initialize the MIMEMessage.
   */
    public MIMEMessage() {
        attachments = new Vector();
        boundary = "--------------74329329-84328432-279-4382";
    }

    /**
   *  Get the Content-Type of this message, also includes the boundary.
   */
    public String getContentType() {
        return "MULTIPART/MIXED; BOUNDARY=\"" + boundary + "\"";
    }

    /**
   *  Add an attachment to this message
   */
    public void addAttachment(MIMEAttachment a) {
        attachments.addElement(a);
    }

    /**
   *  Remove an attachment to this message
   */
    public void removeAttachment(MIMEAttachment a) {
        attachments.removeElement(a);
    }

    /**
   *  Get an enumeration of the attachments to this message.
   */
    public Enumeration getAttachments() {
        return attachments.elements();
    }

    /**
   *  Get the boundary between parts of the message.
   */
    public String getBoundary() {
        return boundary;
    }

    /**
   *  Set the boundary between parts of the message.
   */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    /**
   *  Return the encoded message (including all attachments)
   */
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        write(pw);
        pw.flush();
        return sw.toString();
    }

    /**
   *  Write this message to the given output stream.
   */
    public void write(PrintWriter w) {
        Enumeration e = getAttachments();
        while (e.hasMoreElements()) {
            MIMEAttachment a = (MIMEAttachment) e.nextElement();
            w.print("--");
            w.print(boundary);
            w.print(CRLF);
            a.write(w);
            w.print(CRLF);
        }
        w.print("--");
        w.print(boundary);
        w.print("--");
        w.print(CRLF);
    }

    /**
   *  Return a MIMEMessage built from the InputStream that
   *  points to a MIME message.  Reads the stream fully
   *  before parsing, so watch out.
   */
    public static MIMEMessage parse(InputStream s) throws MIMEException {
        byte[] data = null;
        try {
            data = readInputStreamFully(s);
        } catch (Exception x) {
            throw new MIMEException(x + " thrown in read of InputStream.");
        }
        return parse(data);
    }

    /**
   *  Return a MIMEMessage built from the data.
   */
    public static MIMEMessage parse(byte data[]) throws MIMEException {
        try {
            MIMEMessage message = new MIMEMessage();
            int index = 0;
            int endIndex = data.length - 1;
            try {
                while (Character.isWhitespace((char) data[index])) ++index;
                while (Character.isWhitespace((char) data[endIndex])) --endIndex;
                endIndex++;
            } catch (Exception x) {
                throw new MIMEException("Message consists entirely of whitespace.");
            }
            Vector v = new Vector(2);
            v.addElement(new Integer(index));
            v.addElement(new Integer(endIndex));
            String sep = null;
            try {
                sep = readLine(data, v);
            } catch (Exception x) {
                throw new MIMEException(x + " thrown while reading attachment separator.");
            }
            if (sep == null) {
                throw new MIMEException("Content separator not found!");
            }
            try {
                while (index < endIndex) {
                    String line = "x";
                    Hashtable headers = new Hashtable();
                    line = readLine(data, v);
                    while (!line.equals("")) {
                        int cIndex = line.indexOf(":");
                        if (cIndex != -1) {
                            headers.put(line.substring(0, cIndex), line.substring(cIndex + 2));
                        }
                        line = readLine(data, v);
                    }
                    StringBuffer info = new StringBuffer();
                    byte[] content = readBody(sep, data, info, v);
                    if (content != null) {
                        MIMEAttachment a = new MIMEAttachment();
                        a.setHeaders(headers);
                        String encoding = a.getHeader("Content-Transfer-Encoding");
                        if (encoding != null && encoding.equalsIgnoreCase("BASE64")) {
                            byte[] c = Base64.decode(removeWhitespace(content));
                            a.setContent(c);
                            a.setBinary(isBinaryContent(c));
                        } else {
                            a.setContent(content);
                            a.setBinary(isBinaryContent(content));
                        }
                        message.addAttachment(a);
                    } else {
                        return message;
                    }
                    index = getIndex(v);
                }
            } catch (Exception x) {
                ;
            }
            return message;
        } catch (Exception x) {
            throw new MIMEException(x + " thrown during parse.");
        }
    }

    private static final int getIndex(Vector v) {
        return ((Integer) v.firstElement()).intValue();
    }

    private static final int getEndIndex(Vector v) {
        return ((Integer) v.elementAt(1)).intValue();
    }

    private static final void setIndex(Vector v, int i) {
        v.setElementAt(new Integer(i), 0);
    }

    private static final void setEndIndex(Vector v, int i) {
        v.setElementAt(new Integer(i), 1);
    }

    private static final String readLine(byte[] data, Vector v) throws Exception {
        int index = getIndex(v);
        int endIndex = getEndIndex(v);
        if (index == endIndex) return new String();
        int c;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        while (index < endIndex) {
            c = (int) data[index];
            if (isLF(c)) {
                index++;
                setIndex(v, index);
                return new String(b.toByteArray());
            }
            if (isCR(c)) {
                index++;
                if (isLF((int) data[index])) ++index;
                setIndex(v, index);
                return new String(b.toByteArray());
            }
            b.write(c);
            index++;
        }
        setIndex(v, index);
        return new String(b.toByteArray());
    }

    /**
   *  Scan the content and decide if it's binary or ASCII data.
   */
    public static boolean isBinaryContent(byte[] data) {
        return isBinaryContent(data, 0, data.length);
    }

    /**
   *  Scan the content and decide if it's binary or ASCII data.
   */
    public static boolean isBinaryContent(byte[] data, int start, int len) {
        byte[] d = data;
        for (int i = start; i < len; i++) {
            if (((int) d[i]) < 0) return true;
        }
        return false;
    }

    private static final byte[] readBody(String sep, byte data[], StringBuffer info, Vector v) throws MIMEException {
        int index = getIndex(v);
        int endIndex = getEndIndex(v);
        int sepLen = sep.length();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean isBinary = false;
        info.insert(0, "ascii");
        info.setLength(5);
        while (index < endIndex) {
            if ((int) data[index] < 0 && !isBinary) {
                info.insert(0, "binary");
                info.setLength(6);
                isBinary = true;
            }
            if (isCR((int) data[index]) && isLF((int) data[index + 1]) || isLF((int) data[index]) && !isCRLF((int) data[index + 1])) {
                int skip = 0;
                if (isLF((int) data[index + 1])) skip = 2; else skip = 1;
                String sepTry = null;
                sepTry = new String(data, index + skip, sepLen);
                if (sepTry.equals(sep)) {
                    if (((index + skip + sepLen) == endIndex) || ((index + skip + sepLen + 2) == endIndex)) {
                        setIndex(v, endIndex);
                        return buffer.toByteArray();
                    }
                    if (isCR((int) data[skip + index + sepLen]) && isLF((int) data[skip + index + sepLen + 1]) || isLF((int) data[skip + index + sepLen]) && !isCRLF((int) data[skip + index + sepLen + 1])) {
                        if (isLF((int) data[index + sepLen + 1])) setIndex(v, index + sepLen + 2); else setIndex(v, index + sepLen + 1);
                        return buffer.toByteArray();
                    }
                    sepTry = null;
                    buffer.write(data, index, skip);
                    index += skip;
                } else {
                    buffer.write(data, index, skip);
                    index += skip;
                }
            } else {
                buffer.write((int) data[index++]);
            }
        }
        setIndex(v, index);
        return buffer.toByteArray();
    }

    private static byte[] readInputStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        {
            int i = 0;
            while ((i = is.read()) != -1) b.write(i);
        }
        return b.toByteArray();
    }

    private static final boolean isCR(int i) {
        return (i == 13);
    }

    private static final boolean isLF(int i) {
        return (i == 10);
    }

    private static final boolean isCRLF(int i) {
        return ((i == 10) || (i == 13));
    }

    private static final byte[] removeWhitespace(byte[] data) {
        byte[] d = data;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < d.length; i++) {
            if (!Character.isWhitespace((char) d[i])) out.write(d[i]);
        }
        return out.toByteArray();
    }

    public static void main(String args[]) {
        if (args.length == 0) {
            System.out.println("Usage: MIMEMessage parse filename");
            System.out.println(" or    MIMEMessage create file1..fileN");
            System.exit(0);
        }
        try {
            String cmd = args[0];
            if (cmd.equalsIgnoreCase("parse")) {
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(args[1])));
                long time = System.currentTimeMillis();
                MIMEMessage m = MIMEMessage.parse(in);
                time = System.currentTimeMillis() - time;
                System.err.println("Parse took " + time + "ms");
                System.err.println("");
                Enumeration e = m.getAttachments();
                while (e.hasMoreElements()) {
                    MIMEAttachment a = (MIMEAttachment) e.nextElement();
                    System.err.println("Attachment:");
                    System.err.println("  Headers:");
                    Enumeration h = a.getHeaderNames();
                    while (h.hasMoreElements()) {
                        String header = (String) h.nextElement();
                        System.err.println("    " + header + ": " + a.getHeader(header));
                    }
                    System.err.println("  Info:");
                    System.err.println("    Content length: " + a.getContent().length);
                    System.err.println("    Binary:         " + a.isBinary());
                    System.err.println("");
                }
                System.out.println(m);
            } else {
                System.err.println("Creating new MIMEMessage");
                MIMEMessage m = new MIMEMessage();
                for (int i = 1; i < args.length; i++) {
                    String file = args[i];
                    String type = "unknown";
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(file)));
                    byte[] buffer = new byte[8192];
                    int read = 0;
                    while ((read = in.read(buffer)) != -1) bout.write(buffer, 0, read);
                    byte[] data = bout.toByteArray();
                    boolean binary = isBinaryContent(data);
                    MIMEAttachment a = new MIMEAttachment(type, file, data, binary);
                    System.err.println("binary = " + binary);
                    m.addAttachment(a);
                }
                System.out.println(m);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
