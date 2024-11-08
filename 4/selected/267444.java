package org.dueam.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletContext;

/**
 * @author Anemone
 * lgh@onhonest.cn
 */
public abstract class FileUtils {

    public static void main(String[] args) {
        System.out.println(getFileExt(".txt"));
        System.out.println(revisePath("c:\\t//t"));
    }

    public static String getRealPath(ServletContext sc, String path) {
        return sc.getRealPath(path);
    }

    public static String getFileExt(String name) {
        int pos = name.lastIndexOf('.');
        if (pos > -1) {
            return name.substring(pos + 1);
        }
        return "";
    }

    private static final int BUFFER_SIZE = 2048;

    public static void writeFile(File src, OutputStream out) throws IOException {
        FileInputStream in = new FileInputStream(src);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len = 0;
        while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    /**
     * ����·��
     * c:\t/t => c:\t\t
     * c:\\t//t => c:\t\t
     */
    public static String revisePath(String path) {
        if (null == path) {
            return null;
        }
        boolean noWrite = true;
        StringBuffer newPath = new StringBuffer();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\\' || c == '/') {
                if (noWrite) {
                    noWrite = false;
                    newPath.append(File.separatorChar);
                }
            } else {
                newPath.append(c);
                noWrite = true;
            }
        }
        return newPath.toString();
    }
}
