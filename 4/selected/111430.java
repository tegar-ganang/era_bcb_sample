package org.sss.etrade;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 打包所有源代码
 * @author Jason.Hoo (latest modification by $Author: hujianxin $)
 * @version $Revision: 707 $ $Date: 2012-04-08 11:25:57 -0400 (Sun, 08 Apr 2012) $
 */
public class Achive {

    static final Log log = LogFactory.getLog(Achive.class);

    public static final void main(String[] argv) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("../list.lst"));
        ZipArchiveOutputStream os = new ZipArchiveOutputStream(new FileOutputStream("../eIBS-sources.zip"));
        String line;
        File file = new File("..");
        while ((line = br.readLine()) != null) addDirectory(os, new File(file, line), file.getAbsolutePath());
        os.close();
    }

    private static final void addDirectory(ZipArchiveOutputStream os, File directory, String prefix) throws IOException {
        for (File file : directory.listFiles()) {
            String name = file.getName();
            if (file.isDirectory()) {
                if (name.startsWith(".") || name.equals("bin")) continue;
                addDirectory(os, file, prefix);
            } else {
                if (name.endsWith(".txt") || name.endsWith(".jj") || name.endsWith(".jjt") || name.startsWith("eibs.") || "packaging-build.xml".equals(name) || ".packaging".equals(name)) continue;
                if ("java".equalsIgnoreCase(FilenameUtils.getExtension(file.getAbsolutePath()))) addJavaSource(os, file, prefix); else addFile(os, file, prefix);
            }
        }
    }

    private static final void addFile(ZipArchiveOutputStream os, File file, String prefix) throws IOException {
        ArchiveEntry entry = os.createArchiveEntry(file, file.getAbsolutePath().substring(prefix.length() + 1));
        os.putArchiveEntry(entry);
        FileInputStream fis = new FileInputStream(file);
        IOUtils.copy(fis, os);
        fis.close();
        os.closeArchiveEntry();
    }

    private static final void addJavaSource(ZipArchiveOutputStream os, File file, String prefix) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(file.getAbsolutePath().substring(prefix.length() + 1));
        entry.setSize(file.length());
        os.putArchiveEntry(entry);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.matches("\\s*[/\\*\\\\].*")) continue;
            line = line.replaceAll("//.*", "");
            if ("".equals(line)) continue;
            os.write(line.getBytes());
            os.write('\r');
        }
        br.close();
        os.closeArchiveEntry();
    }
}
