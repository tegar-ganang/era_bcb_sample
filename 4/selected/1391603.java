package org.shopformat.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Utilities {

    private static Log log = LogFactory.getLog(Utilities.class);

    public static void renameFile(File oldFile, File newFile) {
        if (oldFile.exists()) {
            oldFile.renameTo(newFile);
        } else {
            log.warn("Couldn't find file: " + oldFile.getPath());
        }
    }

    public static String removeTags(String Instring) {
        String Out = Instring;
        String escape;
        int k1, k2, i;
        while ((k1 = Out.indexOf('<')) > -1) {
            k2 = Out.indexOf('>', k1);
            if (k2 == -1) k2 = Out.length() - 1;
            Out = Out.substring(0, k1) + Out.substring(k2 + 1, Out.length());
        }
        k1 = -1;
        while ((k1 = Out.indexOf('&', k1 + 1)) > -1) {
            k2 = Out.indexOf(';', k1);
            if (k2 > k1) {
                escape = Out.substring(k1 + 1, k2);
                if (escape.equals("nbsp")) Out = Out.substring(0, k1) + " " + Out.substring(k2 + 1, Out.length());
                if (escape.equals("amp")) Out = Out.substring(0, k1) + "&" + Out.substring(k2 + 1, Out.length());
                if (escape.equals("lt")) Out = Out.substring(0, k1) + "<" + Out.substring(k2 + 1, Out.length());
                if (escape.equals("gt")) Out = Out.substring(0, k1) + ">" + Out.substring(k2 + 1, Out.length());
                if (escape.equals("quot")) Out = Out.substring(0, k1) + "\"" + Out.substring(k2 + 1, Out.length());
                if (escape.equals("copy")) Out = Out.substring(0, k1) + "(c)" + Out.substring(k2 + 1, Out.length());
            }
        }
        return Out;
    }

    public static String escapeForXML(String string) {
        string = substitute(string, "&", "&amp;");
        string = substitute(string, "\"", "&quot;");
        string = substitute(string, "<", "&lt;");
        string = substitute(string, ">", "&gt;");
        string = substitute(string, "\n", "&#10;");
        string = substitute(string, "'", "&apos;");
        string = substitute(string, "`", "&apos;");
        return string;
    }

    public static String substitute(String string, String pattern, String replacement) {
        int start = string.indexOf(pattern);
        while (start != -1) {
            StringBuffer buffer = new StringBuffer(string);
            buffer.delete(start, start + pattern.length());
            buffer.insert(start, replacement);
            string = new String(buffer);
            start = string.indexOf(pattern, start + replacement.length());
        }
        return string;
    }

    public static String removeNonDigits(String original) {
        if (original == null) return null;
        StringBuffer buff = new StringBuffer(original.length());
        for (int i = 0; i < original.length(); i++) {
            if (Character.isDigit(original.charAt(i))) {
                buff.append(original.charAt(i));
            }
        }
        return buff.toString();
    }

    public static String getStringFromFile(String path) throws IOException {
        StringBuffer buff = new StringBuffer(5000);
        BufferedReader in = new BufferedReader(new FileReader(path));
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                buff.append(line).append('\n');
            }
        } catch (IOException ex) {
            log.error("Problem getting string from file", ex);
            throw ex;
        } finally {
            if (in != null) in.close();
        }
        return buff.toString();
    }

    public static void writeStringToFile(String str, File file) throws IOException {
        FileWriter out = new FileWriter(file);
        try {
            out.write(str);
        } catch (IOException ex) {
            log.error("Problem writing string to file", ex);
            throw ex;
        } finally {
            if (out != null) out.close();
        }
    }

    public static void writeInputStreamToFile(InputStream io, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            byte[] buf = new byte[256];
            int read = 0;
            while ((read = io.read(buf)) > 0) {
                fos.write(buf, 0, read);
            }
        } catch (IOException ex) {
            log.error("Problem writing stream to file", ex);
            throw ex;
        } finally {
            if (fos != null) fos.close();
        }
    }

    public static Vector enforceVector(List list) {
        if (list == null || list instanceof Vector) {
            return (Vector) list;
        } else {
            return new Vector(list);
        }
    }

    public static String createPassword(int length) {
        char[] pw = new char[length];
        int c = 'A';
        int r1 = 0;
        for (int i = 0; i < length; i++) {
            r1 = (int) (Math.random() * 3);
            switch(r1) {
                case 0:
                    c = '0' + (int) (Math.random() * 10);
                    break;
                case 1:
                    c = 'a' + (int) (Math.random() * 26);
                    break;
                case 2:
                    c = 'A' + (int) (Math.random() * 26);
                    break;
            }
            pw[i] = (char) c;
        }
        return new String(pw);
    }

    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        try {
            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists()) {
                    targetLocation.mkdir();
                }
                String[] children = sourceLocation.list();
                for (int i = 0; i < children.length; i++) {
                    copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
                }
            } else {
                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
            }
        } catch (IOException ex) {
            log.error("Problem copying directory", ex);
            throw ex;
        }
    }
}
