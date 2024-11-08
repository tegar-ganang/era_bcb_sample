package com.wemove.wcmf.generator.workflow;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author ingo herwig <ingo@wemove.com>
 */
public class FileUtil {

    public static void copyDir(File src, File dst) throws IOException {
        if (!src.isDirectory()) return;
        if (!dst.exists()) dst.mkdir();
        File[] srcFiles = src.listFiles();
        for (int i = 0; i < srcFiles.length; i++) {
            File srcFile = srcFiles[i];
            File dstFile = new File(dst + File.separator + srcFile.getName());
            if (srcFile.isFile()) copyFile(srcFile, dstFile); else if (srcFile.isDirectory()) copyDir(srcFile, dstFile);
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (!src.isFile()) return;
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    @SuppressWarnings("unchecked")
    public static void unzipDir(File target, File archive) throws IOException {
        ZipFile zip = new ZipFile(archive);
        Enumeration entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = (ZipEntry) entries.nextElement();
            File f = new File(target + File.separator + e.getName());
            if (e.isDirectory()) {
                if (!f.exists() && !f.mkdirs()) throw new IOException("Couldn't create directory " + f);
            } else {
                BufferedInputStream is = null;
                BufferedOutputStream os = null;
                try {
                    is = new BufferedInputStream(zip.getInputStream(e));
                    File destDir = f.getParentFile();
                    if (!destDir.exists() && !destDir.mkdirs()) throw new IOException("Couldn't create directory " + destDir);
                    os = new BufferedOutputStream(new FileOutputStream(f));
                    int b = -1;
                    while ((b = is.read()) != -1) os.write(b);
                } finally {
                    if (is != null) is.close();
                    if (os != null) os.close();
                }
            }
        }
    }

    public static void zipDir(File src, File archive) throws IOException {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive));
            zipDir(src, zos, src);
            zos.close();
        } catch (ZipException e) {
            throw new IOException("ZIP Problem " + e.getMessage());
        }
    }

    protected static void zipDir(File zipDir, ZipOutputStream zos, File root) throws IOException {
        String[] dirList = zipDir.list();
        if (dirList != null) {
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    zipDir(f, zos, root);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String relPath = f.getPath().substring(root.getPath().length() + 1);
                ZipEntry anEntry = new ZipEntry(relPath);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) zos.write(readBuffer, 0, bytesIn);
                fis.close();
            }
        }
    }
}
