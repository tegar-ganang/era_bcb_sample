package malgnsoft.util;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import malgnsoft.util.Malgn;

public class UnZip {

    public String errMsg;

    public void copyInputStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) return;
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public boolean extract(String file, String folder) {
        return extract(new File(file), folder);
    }

    public boolean extract(File f, String folder) {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(f);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry == null) continue;
                String path = folder + "/" + entry.getName().replace('\\', '/');
                if (!entry.isDirectory()) {
                    File destFile = new File(path);
                    String parent = destFile.getParent();
                    if (parent != null) {
                        File parentFile = new File(parent);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                    }
                    copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(destFile)));
                }
            }
            zipFile.close();
        } catch (IOException ioe) {
            this.errMsg = ioe.getMessage();
            Malgn.errorLog("{UnZip.extract} " + ioe.getMessage());
            return false;
        }
        return true;
    }
}
