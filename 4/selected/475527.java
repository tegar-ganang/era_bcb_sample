package com.metanology.mde.utils;

import java.io.*;

/**
 * @author wwang
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class FileUtils {

    public static void copy(File src, File dest) throws FileNotFoundException, IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dest);
        try {
            byte[] buf = new byte[1024];
            int c = -1;
            while ((c = in.read(buf)) > 0) out.write(buf, 0, c);
        } finally {
            in.close();
            out.close();
        }
    }

    public static String removeExtension(String filename) {
        File f = new File(filename);
        String name = f.getName();
        int index = name.lastIndexOf(".");
        if (index > 0) {
            name = name.substring(0, index);
            String parent = f.getParent();
            if (parent != null) {
                return parent + File.separatorChar + name;
            } else {
                return name;
            }
        }
        return filename;
    }
}
