package helper;

import halle.KTH;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class IOUtils {

    public static void copyFile(File src, File dest, boolean force) throws IOException {
        if (dest.exists()) {
            if (force) {
                dest.delete();
            } else {
                throw new IOException("Cannot overwrite existing file: " + dest);
            }
        }
        byte[] buffer = new byte[1];
        int read = 0;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            while (true) {
                read = in.read(buffer);
                if (read == -1) {
                    break;
                }
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        }
    }

    public static boolean isNewerLastModified(File f1, File f2) {
        long filedate = f1.lastModified();
        long resourcedate = f2.lastModified();
        if (f1.exists()) {
            KTH.out.pl(filedate, 0);
        }
        if (f2.exists()) {
            KTH.out.pl(resourcedate, 0);
        }
        if (!f1.exists() && !f2.exists()) KTH.out.err("Eine von beiden Dateien existiert nicht. f1:" + f1.exists());
        return resourcedate < filedate;
    }

    public static boolean isNewerContent(File f1, File f2) {
        SAXBuilder builder = new SAXBuilder();
        boolean newer = false;
        try {
            Document doc = builder.build(f1);
            Element re = doc.getRootElement();
            String s1 = re.getChild("info").getChild("lastModified").getValue();
            doc = builder.build(f2);
            re = doc.getRootElement();
            String s2 = re.getChild("info").getChild("lastModified").getValue();
            KTH.out.pl(s1 + s2, 0);
            if (Long.valueOf(s1) > Long.valueOf(s2)) newer = true;
        } catch (Exception e) {
            KTH.out.err(e);
        }
        return newer;
    }
}
