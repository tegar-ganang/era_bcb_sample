package vacuum.lgadmin.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

public class IOUtils {

    public static void writeHashMapStringString(File dest, HashMap<String, String> data) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        for (String key : data.keySet()) {
            writeString(fos, key);
            fos.write(':');
            writeString(fos, data.get(key));
            fos.write('\n');
        }
    }

    public static HashMap<String, String> readHashMapStringString(File dest) throws IOException {
        HashMap<String, String> data = new HashMap<String, String>();
        FileInputStream fis = new FileInputStream(dest);
        int i;
        StringBuffer buf = new StringBuffer();
        String name = null;
        while ((i = fis.read()) != -1) {
            switch(i) {
                case ':':
                    name = buf.toString();
                    buf.setLength(0);
                    break;
                case '\n':
                    data.put(name, buf.toString());
                    break;
                default:
                    buf.append((char) i);
            }
        }
        return data;
    }

    public static void writeHashMapStringStackString(File dest, HashMap<String, Stack<String>> data) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        for (String key : data.keySet()) {
            writeString(fos, key);
            fos.write(':');
            Stack<String> stack = data.get(key);
            boolean first = true;
            while (!stack.isEmpty()) {
                if (!first) fos.write('|'); else first = false;
                writeString(fos, stack.pop());
            }
            fos.write('\n');
        }
    }

    public static HashMap<String, Stack<String>> readHashMapStringStackString(File dest) throws IOException {
        HashMap<String, Stack<String>> data = new HashMap<String, Stack<String>>();
        FileInputStream fis = new FileInputStream(dest);
        int i;
        StringBuffer buf = new StringBuffer();
        String name = null;
        Stack<String> stack = new Stack<String>();
        while ((i = fis.read()) != -1) {
            switch(i) {
                case '|':
                    stack.push(buf.toString());
                    buf.setLength(0);
                    break;
                case ':':
                    name = buf.toString();
                    buf.setLength(0);
                    break;
                case '\n':
                    data.put(name, stack);
                    stack = new Stack<String>();
                    break;
                default:
                    buf.append((char) i);
            }
        }
        return data;
    }

    private static void writeString(FileOutputStream fos, String str) throws IOException {
        for (char c : str.toCharArray()) fos.write(c);
    }

    public static void download(File to, URL from) throws IOException {
        InputStream is = from.openStream();
        OutputStream os = new FileOutputStream(to);
        int i = 0;
        while ((i = is.read()) != -1) os.write(i);
        os.flush();
        is.close();
        os.close();
    }

    public static String download(URL from) throws IOException {
        InputStream is = from.openStream();
        StringBuffer buf = new StringBuffer();
        int i = 0;
        while ((i = is.read()) != -1) buf.append((char) i);
        is.close();
        return buf.toString();
    }
}
