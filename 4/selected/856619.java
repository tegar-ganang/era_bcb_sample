package org.wportal.core;

import org.w3c.tidy.Tidy;
import org.w3c.tidy.Configuration;
import java.io.*;

/**
 * User: SimonLei
 * Date: 2004-10-10
 * Time: 13:37:06
 * $Id: Utils.java,v 1.5 2004/11/14 10:00:17 simon_lei Exp $
 */
public class Utils {

    public static void copyInput2Output(InputStream in, OutputStream out) throws IOException {
        int read = 0;
        byte buffer[] = new byte[8192];
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public static void copyInputReader2OutWriter(Reader reader, Writer writer) throws IOException {
        int read = 0;
        char buffer[] = new char[8192];
        while ((read = reader.read(buffer)) > -1) {
            writer.write(buffer, 0, read);
        }
        reader.close();
        writer.close();
    }

    public static InputStream getResourceAsStream(String name) {
        return Utils.class.getResourceAsStream(name);
    }

    public static void assertInputStreamEquals(InputStream from, InputStream to) throws Exception {
        ByteArrayOutputStream fromByteOuts = new ByteArrayOutputStream();
        ByteArrayOutputStream toByteOuts = new ByteArrayOutputStream();
        copyInput2Output(from, fromByteOuts);
        copyInput2Output(to, toByteOuts);
        byte[] fromBytes = fromByteOuts.toByteArray();
        byte[] toBytes = toByteOuts.toByteArray();
        if (fromBytes.length != toBytes.length) throw new Exception("They are different from: " + fromBytes.length + " vs. to: " + toBytes.length);
        for (int i = 0; i < fromBytes.length; i++) {
            if (fromBytes[i] != toBytes[i]) throw new Exception("They are different");
        }
    }

    public static String tidyHtml(String raw) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setCharEncoding(Configuration.UTF8);
        tidy.setErrout(new PrintWriter(new StringWriter()));
        InputStream is = new ByteArrayInputStream(raw.getBytes("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tidy.parse(is, out);
        return out.toString("UTF-8");
    }

    public static boolean isNull(String str) {
        if (str == null) return true;
        if ("".equals(str.trim())) return true;
        return false;
    }
}
